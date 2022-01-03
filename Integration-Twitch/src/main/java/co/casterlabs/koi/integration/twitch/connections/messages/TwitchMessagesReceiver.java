package co.casterlabs.koi.integration.twitch.connections.messages;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.gikk.twirk.Twirk;
import com.gikk.twirk.enums.CLEARCHAT_MODE;
import com.gikk.twirk.enums.EMOTE_SIZE;
import com.gikk.twirk.events.TwirkListener;
import com.gikk.twirk.types.TwitchTags;
import com.gikk.twirk.types.clearChat.ClearChat;
import com.gikk.twirk.types.clearMsg.ClearMsg;
import com.gikk.twirk.types.emote.Emote;
import com.gikk.twirk.types.notice.Notice;
import com.gikk.twirk.types.roomstate.Roomstate;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.usernotice.Usernotice;
import com.gikk.twirk.types.usernotice.subtype.Raid;
import com.gikk.twirk.types.users.TwitchUser;
import com.gikk.twirk.types.users.Userstate;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import co.casterlabs.koi.client.connection.Connection;
import co.casterlabs.koi.client.connection.ConnectionHolder;
import co.casterlabs.koi.events.ChatEvent;
import co.casterlabs.koi.events.ClearChatEvent;
import co.casterlabs.koi.events.MessageMetaEvent;
import co.casterlabs.koi.events.PlatformMessageEvent;
import co.casterlabs.koi.events.RaidEvent;
import co.casterlabs.koi.events.RoomstateEvent;
import co.casterlabs.koi.integration.twitch.data.TwitchUserConverter;
import co.casterlabs.koi.integration.twitch.impl.TwitchAppAuth;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.koi.util.RepeatingThread;
import co.casterlabs.koi.util.WebUtil;
import lombok.NonNull;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

public class TwitchMessagesReceiver implements TwirkListener, Connection {
    private static final String CHANNEL_BADGE_ENDPOINT = "https://badges.twitch.tv/v1/badges/channels/%s/display";
    private static final String GLOBAL_BADGE_ENDPOINT = "https://badges.twitch.tv/v1/badges/global/display";

    private static JsonObject globalBadges = new JsonObject();

    private Twirk twirk;
    private ConnectionHolder holder;
    private TwitchAppAuth auth;

    private JsonObject channelBadges = new JsonObject();
    private RepeatingThread badgeThread;

    private RoomstateEvent roomstate;

    private boolean isCheckingForMod = false;

    static {
        new RepeatingThread("Twitch Badge Poll - Koi", TimeUnit.HOURS.toMillis(1), () -> {
            JsonObject response = WebUtil.jsonSendHttpGet(GLOBAL_BADGE_ENDPOINT, null, JsonObject.class);

            globalBadges = response.getAsJsonObject("badge_sets");
        }).start();
    }

    public TwitchMessagesReceiver(ConnectionHolder holder, @NonNull TwitchAppAuth twitchAppAuth) {
        this.holder = holder;
        this.auth = twitchAppAuth;

        this.badgeThread = new RepeatingThread("Twitch Badge Poll - Koi", TimeUnit.MINUTES.toMillis(15), () -> {
            JsonObject response = WebUtil.jsonSendHttpGet(String.format(CHANNEL_BADGE_ENDPOINT, this.holder.getSimpleProfile().getChannelId()), null, JsonObject.class);

            this.channelBadges = response.getAsJsonObject("badge_sets");
        });

        this.badgeThread.start();
    }

    public void sendMessage(@NonNull String message) {
        this.twirk.channelMessage(message);
    }

    private void reconnect() {
        try {
            this.twirk = this.auth.getTwirk(this.holder.getProfile().getUsername());

            this.holder.setHeldEvent(null);
            this.twirk.addIrcListener(this);
            this.twirk.connect();
        } catch (Exception e) {
            this.reconnect();
        }
    }

    @Override
    public void onDisconnect() {
        if (this.holder.isExpired()) {
            this.badgeThread.stop();
            FastLogger.logStatic(LogLevel.DEBUG, "Closed messages for %s", this.holder.getSimpleProfile());
        } else {
            this.reconnect();
        }
    }

    @Override
    public void onRoomstate(Roomstate roomstate) {
        boolean isEmoteOnly = roomstate.getEmoteOnlyMode() > 0;
        boolean isSubsOnly = roomstate.getSubMode() > 0;
        boolean isR9K = roomstate.get9kMode() > 0;
        boolean isFollowersOnly = roomstate.getFollowersMode() > 0;
        boolean isSlowMode = roomstate.getSlowModeTimer() > 0;

        this.roomstate = new RoomstateEvent(this.holder.getProfile())
            .setEmoteOnly(isEmoteOnly)
            .setSubsOnly(isSubsOnly)
            .setR9K(isR9K)
            .setFollowersOnly(isFollowersOnly)
            .setSlowMode(isSlowMode);

        this.holder.broadcastEvent(this.roomstate);
    }

    @Override
    public void onNotice(Notice notice) {
        // Parse the notice and update the roomstate.
        switch (notice.getRawNoticeID()) {

            // Emote Only

            case "emote_only_on": {
                this.holder.broadcastEvent(
                    this.roomstate
                        .setEmoteOnly(true)
                );
                break;
            }

            case "emote_only_off": {
                this.holder.broadcastEvent(
                    this.roomstate
                        .setEmoteOnly(false)
                );
                break;
            }

            // Followers Only

            case "followers_onzero":
            case "followers_on": {
                this.holder.broadcastEvent(
                    this.roomstate
                        .setFollowersOnly(true)
                );
                break;
            }

            case "followers_off": {
                this.holder.broadcastEvent(
                    this.roomstate
                        .setFollowersOnly(false)
                );
                break;
            }

            // R9K

            case "r9k_on": {
                this.holder.broadcastEvent(
                    this.roomstate
                        .setR9K(true)
                );
                break;
            }

            case "r9k_off": {
                this.holder.broadcastEvent(
                    this.roomstate
                        .setR9K(false)
                );
                break;
            }

            // Slow Mode

            case "slow_on": {
                this.holder.broadcastEvent(
                    this.roomstate
                        .setSlowMode(true)
                );
                break;
            }

            case "slow_off": {
                this.holder.broadcastEvent(
                    this.roomstate
                        .setSlowMode(false)
                );
                break;
            }

            // Subs Only

            case "subs_on": {
                this.holder.broadcastEvent(
                    this.roomstate
                        .setSubsOnly(true)
                );
                break;
            }

            case "subs_off": {
                this.holder.broadcastEvent(
                    this.roomstate
                        .setSubsOnly(false)
                );
                break;
            }

            case "room_mods":
            case "no_mods": {
                if (this.isCheckingForMod) {
                    this.isCheckingForMod = false;

                    // We need to send a message warning the user that they will not be able to run
                    // commands without going to their channel and typing /mod casterlabs.
                    if (!notice.getMessage().contains("casterlabs")) {
                        String message = String.format(
                            "You need to type \"/mod casterlabs\" (without the quotes) into your channel's chat (https://twitch.tv/%s/chat) in order for slash commands to work. (We know, it's not ideal)",
                            this.holder.getProfile().getUsername()
                        );

                        this.holder.broadcastEvent(
                            new PlatformMessageEvent(
                                message,
                                UserPlatform.CASTERLABS_SYSTEM,
                                this.holder.getProfile(),
                                true
                            )
                        );
                    }

                    return;
                }
            }

        }

        // Forward the message to the streamer.
        boolean isError = false; // TODO

        this.holder.broadcastEvent(
            new PlatformMessageEvent(
                notice.getMessage(),
                UserPlatform.TWITCH,
                this.holder.getProfile(),
                isError
            )
        );
    }

    @Override
    public void onUserstate(Userstate userstate) {
        String color = "#" + Integer.toHexString(userstate.getColor()).toUpperCase();

        TwitchUserConverter.setColor(this.holder.getSimpleProfile().getId(), color);
    }

    @Override
    public void onUsernotice(TwitchUser user, Usernotice usernotice) {
        if (usernotice.isRaid()) {
            User host = TwitchUserConverter.getInstance().transform(user);

            Raid raid = usernotice.getRaid().get();

            RaidEvent event = new RaidEvent(host, this.holder.getProfile(), raid.getRaidCount());

            this.holder.broadcastEvent(event);
        }
    }

    @Override
    public void onPrivMsg(com.gikk.twirk.types.users.TwitchUser user, TwitchMessage message) {
        // We use PubSub for this, and Twirk seems broken.
        if (message.getTagMap().getAsInt(TwitchTags.BITS) == -1) {
            User sender = TwitchUserConverter.getInstance().transform(user);
            ChatEvent event = new ChatEvent("chat:" + message.getMessageID(), message.getContent(), sender, this.holder.getProfile());

            for (String badgeData : user.getBadges()) {
                try {
                    String link = this.getBadgeUrl(badgeData);

                    sender.getBadges().add(link);
                } catch (Exception ignored) {}
            }

            if (message.hasEmotes()) {
                for (Emote emote : message.getEmotes()) {
                    event.getEmotes().put(emote.getPattern(), emote.getEmoteImageUrl(EMOTE_SIZE.LARGE).replace("http://", "https://"));
                }
            }

            // MESSAGE_ID seems confusing, but that's Twitch's way of saying MESSAGE_TYPE.
            // ID is the true unique message id, Twitch sux.
            String messageType = message.getTagMap().getAsString(TwitchTags.MESSAGE_ID);
            if (messageType != null) {
                switch (messageType) {
                    case "highlighted-message": {
                        event.setHighlighted(true);
                        break;
                    }
                }
            }

            this.holder.broadcastEvent(event);
        }
    }

    @Override
    public void onClearChat(ClearChat clearChat) {
        String upid = null;

        if (clearChat.getMode() == CLEARCHAT_MODE.USER) {
            User user = TwitchUserConverter.getInstance().get(clearChat.getTarget());

            upid = user.getId() + ";TWITCH";
        }

        this.holder.broadcastEvent(new ClearChatEvent(this.holder.getProfile(), upid));
    }

    @Override
    public void onClearMsg(ClearMsg clearMsg) {
        MessageMetaEvent event = new MessageMetaEvent(this.holder.getProfile(), "chat:" + clearMsg.getTargetMsgId());

        event.setVisible(false);

        this.holder.broadcastEvent(event);
    }

    @Override
    public void close() {
        this.twirk.close();
    }

    @Override
    public void open() throws IOException {
        this.reconnect();
    }

    @Override
    public boolean isOpen() {
        if (this.twirk == null) {
            return false;
        } else {
            return this.twirk.isConnected();
        }
    }

    private String getBadgeUrl(String badge) {
        String[] split = badge.split("/");
        String name = split[0];
        String version = split[1];

        JsonObject badgeSet = this.channelBadges.getAsJsonObject(name);

        if ((badgeSet == null) || !badgeSet.getAsJsonObject("versions").has(version)) {
            JsonElement e = globalBadges.get(name);

            badgeSet = e.getAsJsonObject();
        }

        JsonObject badgeData = badgeSet.getAsJsonObject("versions").getAsJsonObject(version);

        return badgeData.get("image_url_4x").getAsString();
    }

    public void checkIfMod() {
        this.isCheckingForMod = true;
        this.sendMessage("/mods");
    }

}
