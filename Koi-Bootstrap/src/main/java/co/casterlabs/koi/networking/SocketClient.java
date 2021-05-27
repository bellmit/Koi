package co.casterlabs.koi.networking;

import java.util.Collection;

import org.java_websocket.WebSocket;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.koi.Koi;
import co.casterlabs.koi.KoiImpl;
import co.casterlabs.koi.StatsReporter;
import co.casterlabs.koi.client.Client;
import co.casterlabs.koi.client.ClientEventListener;
import co.casterlabs.koi.client.Puppet;
import co.casterlabs.koi.clientid.ClientIdMeta;
import co.casterlabs.koi.clientid.ClientIdMismatchException;
import co.casterlabs.koi.clientid.ClientIdScope;
import co.casterlabs.koi.config.ThirdPartyBannerConfig;
import co.casterlabs.koi.events.Event;
import co.casterlabs.koi.networking.incoming.ChatRequest;
import co.casterlabs.koi.networking.incoming.ChatRequest.Chatter;
import co.casterlabs.koi.networking.incoming.CredentialsRequest;
import co.casterlabs.koi.networking.incoming.DeleteRequest;
import co.casterlabs.koi.networking.incoming.PuppetLoginRequest;
import co.casterlabs.koi.networking.incoming.TestEventRequest;
import co.casterlabs.koi.networking.incoming.UpvoteRequest;
import co.casterlabs.koi.networking.incoming.UserLoginRequest;
import co.casterlabs.koi.networking.incoming.UserStreamStatusRequest;
import co.casterlabs.koi.networking.outgoing.ClientBannerNotice;
import co.casterlabs.koi.networking.outgoing.OutgoingMessageErrorType;
import co.casterlabs.koi.networking.outgoing.OutgoingMessageType;
import co.casterlabs.koi.user.IdentifierException;
import co.casterlabs.koi.user.PlatformException;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import xyz.e3ndr.eventapi.EventHelper;
import xyz.e3ndr.eventapi.listeners.EventListener;
import xyz.e3ndr.eventapi.listeners.EventWrapper;

@RequiredArgsConstructor
public class SocketClient implements ClientEventListener {
    private static final JsonObject keepAliveJson = new JsonObject();

    private @Getter @NonNull ClientIdMeta clientIdMeta;
    private @Getter @NonNull String clientId;
    private @NonNull WebSocket socket;
    private @NonNull Koi koi;

    private long lastPing = System.currentTimeMillis();

    private @Getter Collection<EventWrapper> wrappers = EventHelper.wrap(this).values();

    private @Getter Client client;
    private @Getter Puppet puppet;

    static {
        keepAliveJson.addProperty("disclaimer", "Made with \u2665 by Casterlabs");
    }

    /* -------------------------------- */
    /* Request related code             */
    /* -------------------------------- */

    @EventListener
    public void onUserLoginRequest(UserLoginRequest request) {
        try {
            if (this.clientIdMeta.hasScope(ClientIdScope.USER_AUTH)) {
                if (this.client == null) {
                    this.client = new Client(this, request.getToken(), this.clientId);

                    StatsReporter.get(this.client.getAuth().getPlatform()).registerConnection(this.client.getProfile().getUsername(), this.clientId);

                    for (ClientBannerNotice notice : KoiImpl.getInstance().getNotices()) {
                        this.sendNotice(notice);
                    }

                    for (ClientBannerNotice notice : ThirdPartyBannerConfig.getBanners(this.client.getAuth().getPlatform())) {
                        this.sendNotice(notice);
                    }
                } else {
                    this.sendError(OutgoingMessageErrorType.USER_ALREADY_PRESENT, request.getNonce());
                }
            } else {
                this.sendError(OutgoingMessageErrorType.CLIENT_ID_MISSING_SCOPES, request.getNonce());
            }
        } catch (IdentifierException | PlatformException e) {
            this.sendError(OutgoingMessageErrorType.USER_AUTH_INVALID, request.getNonce());
        } catch (ClientIdMismatchException e) {
            this.sendError(OutgoingMessageErrorType.CLIENT_ID_MISMATCH, request.getNonce());
        }
    }

    @EventListener
    public void onPuppetLoginRequest(PuppetLoginRequest request) {
        try {
            if (this.clientIdMeta.hasScope(ClientIdScope.USER_PUPPET_AUTH)) {
                if (this.client == null) {
                    this.sendError(OutgoingMessageErrorType.USER_NOT_AUTHORIZED, request.getNonce());
                } else {
                    if (this.puppet != null) {
                        this.puppet.close();
                    }

                    if (request.getToken() != null) {
                        this.puppet = new Puppet(this.client, request.getToken(), this.clientId);
                    }
                }
            } else {
                this.sendError(OutgoingMessageErrorType.CLIENT_ID_MISSING_SCOPES, request.getNonce());
            }
        } catch (IdentifierException | PlatformException e) {
            this.sendError(OutgoingMessageErrorType.PUPPET_AUTH_INVALID, request.getNonce());
        } catch (ClientIdMismatchException e) {
            this.sendError(OutgoingMessageErrorType.CLIENT_ID_MISMATCH, request.getNonce());
        }
    }

    @EventListener
    public void onUpvoteRequest(UpvoteRequest request) {
        if (this.clientIdMeta.hasScope(ClientIdScope.USER_UPVOTE_MESSAGE)) {
            if ((this.client == null) || (this.client.getAuth() == null)) {
                this.sendError(OutgoingMessageErrorType.USER_NOT_AUTHORIZED, request.getNonce());
            } else {
                try {
                    this.client.upvote(request.getMessageId());
                } catch (UnsupportedOperationException e) {
                    this.sendError(OutgoingMessageErrorType.NOT_IMPLEMENTED, request.getNonce());
                }
            }
        } else {
            this.sendError(OutgoingMessageErrorType.CLIENT_ID_MISSING_SCOPES, request.getNonce());
        }
    }

    @EventListener
    public void onDeleteRequest(DeleteRequest request) {
        if (this.clientIdMeta.hasScope(ClientIdScope.USER_DELETE_MESSAGE)) {
            if ((this.client == null) || (this.client.getAuth() == null)) {
                this.sendError(OutgoingMessageErrorType.USER_NOT_AUTHORIZED, request.getNonce());
            } else {
                try {
                    this.client.delete(request.getMessageId());
                } catch (UnsupportedOperationException e) {
                    this.sendError(OutgoingMessageErrorType.NOT_IMPLEMENTED, request.getNonce());
                }
            }
        } else {
            this.sendError(OutgoingMessageErrorType.CLIENT_ID_MISSING_SCOPES, request.getNonce());
        }
    }

    @EventListener
    public void onChatRequest(ChatRequest request) {
        if (this.clientIdMeta.hasScope(ClientIdScope.USER_SEND_CHAT)) {
            if ((this.client == null) || (this.client.getAuth() == null)) {
                this.sendError(OutgoingMessageErrorType.USER_NOT_AUTHORIZED, request.getNonce());
            } else {
                try {
                    if (request.getChatter() == Chatter.PUPPET) {
                        if (this.puppet == null) {
                            this.sendError(OutgoingMessageErrorType.PUPPET_USER_NOT_AUTHORIZED, request.getNonce());
                        } else {
                            try {
                                this.puppet.chat(request.getMessage());
                            } catch (ApiAuthException e) {
                                this.puppet.close();
                                this.puppet = null;
                                this.sendError(OutgoingMessageErrorType.PUPPET_AUTH_INVALID, request.getNonce());
                            }
                        }
                    } else {
                        this.client.chat(request.getMessage());
                    }
                } catch (UnsupportedOperationException e) {
                    this.sendError(OutgoingMessageErrorType.NOT_IMPLEMENTED, request.getNonce());
                }
            }
        } else {
            this.sendError(OutgoingMessageErrorType.CLIENT_ID_MISSING_SCOPES, request.getNonce());
        }
    }

    @EventListener
    public void onUserStreamStatusRequest(UserStreamStatusRequest request) {
        try {
            if (this.clientIdMeta.hasStreamStatus(request.getUsername(), request.getPlatform())) {
                if (this.client == null) {
                    this.client = new Client(this, request.getUsername(), request.getPlatform());

                    StatsReporter.get(request.getPlatform()).registerConnection(request.getUsername(), this.clientId);
                } else {
                    this.sendError(OutgoingMessageErrorType.USER_ALREADY_PRESENT, request.getNonce());
                }
            } else {
                this.sendError(OutgoingMessageErrorType.CLIENT_ID_MISSING_SCOPES, request.getNonce());
            }
        } catch (IdentifierException e) {
            this.sendError(OutgoingMessageErrorType.USER_INVALID, request.getNonce());
        } catch (PlatformException e) {
            this.sendError(OutgoingMessageErrorType.USER_PLATFORM_INVALID, request.getNonce());
        }
    }

    @EventListener
    public void onTestEventRequest(TestEventRequest request) {
        Event e = request.getEventType().getTestEvent();

        if (e == null) {
            this.sendError(OutgoingMessageErrorType.REQUEST_CRITERIA_INVAID, request.getNonce());
        } else {
            this.sendEvent(e);
        }
    }

    @EventListener
    public void onCredentialsRequest(CredentialsRequest request) {
        if (this.clientIdMeta.hasScope(ClientIdScope.USER_CREDENTIALS)) {
            if ((this.client == null) || (this.client.getAuth() == null)) {
                this.sendError(OutgoingMessageErrorType.USER_NOT_AUTHORIZED, request.getNonce());
            } else {
                try {
                    JsonElement e = this.client.getCredentials();

                    if (e.isJsonNull()) {
                        this.sendError(OutgoingMessageErrorType.USER_AUTH_INVALID, request.getNonce());
                    } else {
                        this.send(e.getAsJsonObject(), OutgoingMessageType.CREDENTIALS);
                    }
                } catch (UnsupportedOperationException e) {
                    this.sendError(OutgoingMessageErrorType.NOT_IMPLEMENTED, request.getNonce());
                }
            }
        } else {
            this.sendError(OutgoingMessageErrorType.CLIENT_ID_MISSING_SCOPES, request.getNonce());
        }
    }

    /* -------------------------------- */
    /* Connection related code          */
    /* -------------------------------- */

    public void onClose() {
        if (this.client != null) {
            StatsReporter.get(this.client.getProfile().getPlatform()).unregisterConnection(this.client.getProfile().getUsername(), this.clientId);

            this.client.close();
            this.client = null;
        }

        if (this.puppet != null) {
            this.puppet.close();
            this.puppet = null;
        }
    }

    public void sendKeepAlive() {
        if (this.isAlive()) {
            this.send(keepAliveJson, OutgoingMessageType.KEEP_ALIVE);
        }
    }

    public boolean isExpired(long current) {
        return (current - this.lastPing) > (SocketServer.KEEP_ALIVE_INTERVAL * 2);
    }

    public void onPong() {
        this.lastPing = System.currentTimeMillis();
    }

    public boolean isAlive() {
        return this.socket.isOpen();
    }

    /* -------------------------------- */
    /* Transmission related code        */
    /* -------------------------------- */

    @Override
    public void onCredentialExpired() {
        this.sendError(OutgoingMessageErrorType.USER_AUTH_INVALID, null);
        this.onClose();
    }

    @Override
    public void onEvent(Event e) {
        this.sendEvent(e);
    }

    public void sendEvent(Event e) {
        if (e != null) {
            JsonObject json = new JsonObject();

            json.add("event", e.serialize());

            this.send(json, OutgoingMessageType.EVENT);
        }
    }

    public void sendSystemMessage(String message) {
        this.sendString(OutgoingMessageType.SYSTEM, "server", message, null);
    }

    public void sendNotice(ClientBannerNotice notice) {
        if ((this.client != null) && (this.client.getAuth() != null)) {
            JsonObject json = new JsonObject();

            json.add("notice", notice.getAsJson());

            this.send(json, OutgoingMessageType.NOTICE);
        }
    }

    public void sendError(OutgoingMessageErrorType error, String nonce) {
        this.sendString(OutgoingMessageType.ERROR, "error", error.name(), nonce);
    }

    public void sendWelcomeMessage() {
        this.sendString(OutgoingMessageType.WELCOME, "server", "Welcome! - Koi v" + Koi.VERSION, null);
    }

    public void send(JsonObject json, OutgoingMessageType type) {
        json.addProperty("type", type.name());

        if (this.isAlive()) {
            if (this.isAlive()) {
                this.socket.send(json.toString());
            }
        }
    }

    private void sendString(OutgoingMessageType type, String key, String value, String nonce) {
        JsonObject json = new JsonObject();

        json.addProperty(key, value);
        json.addProperty("nonce", nonce);

        this.send(json, type);
    }

}
