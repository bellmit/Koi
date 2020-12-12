package co.casterlabs.koi.user.caffeine;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.caffeineapi.realtime.messages.CaffeineMessages;
import co.casterlabs.caffeineapi.realtime.messages.CaffeineMessages.CaffeineMessagesListener;
import co.casterlabs.caffeineapi.realtime.messages.PropEvent;
import co.casterlabs.caffeineapi.realtime.query.CaffeineQuery;
import co.casterlabs.caffeineapi.realtime.query.CaffeineQuery.CaffeineQueryListener;
import co.casterlabs.caffeineapi.requests.CaffeineFollowersListRequest;
import co.casterlabs.caffeineapi.requests.CaffeineFollowersListRequest.CaffeineFollower;
import co.casterlabs.caffeineapi.requests.CaffeineUserInfoRequest;
import co.casterlabs.koi.ErrorReporting;
import co.casterlabs.koi.Koi;
import co.casterlabs.koi.events.ChatEvent;
import co.casterlabs.koi.events.DonationEvent;
import co.casterlabs.koi.events.FollowEvent;
import co.casterlabs.koi.events.StreamStatusEvent;
import co.casterlabs.koi.events.UpvoteEvent;
import co.casterlabs.koi.events.UserUpdateEvent;
import co.casterlabs.koi.user.IdentifierException;
import co.casterlabs.koi.user.SerializedUser;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.koi.user.UserProvider;

public class CaffeineUser extends User implements CaffeineMessagesListener, CaffeineQueryListener {
    private CaffeineMessages messageSocket;
    private CaffeineQuery querySocket;

    private CaffeineUser(String identifier, Object data) throws IdentifierException {
        super(UserPlatform.CAFFEINE);

        if (data == null) {
            this.UUID = identifier; // TEMP for updateUser();

            this.updateUser();
        } else {
            this.updateUser(data);
        }

        this.load();

        CaffeineFollowersListRequest request = new CaffeineFollowersListRequest((CaffeineAuth) Koi.getInstance().getAuthProvider(UserPlatform.CAFFEINE));

        request.setCAID(this.UUID);
        request.sendAsync().thenAccept((followers) -> {
            for (CaffeineFollower follower : followers) {
                this.followers.add(follower.getCAID());
            }
        });
    }

    @Override
    protected void close0() {
        if (this.messageSocket != null) {
            this.messageSocket.disconnect();
        }

        if (this.querySocket != null) {
            this.querySocket.disconnect();
        }
    }

    @Override
    public void tryExternalHook() {
        if (this.messageSocket == null) {
            this.messageSocket = new CaffeineMessages(this.UUID);
            this.messageSocket.setListener(this);
        }

        if (this.querySocket == null) {
            this.querySocket = new CaffeineQuery(this.getUsername());
            this.querySocket.setListener(this);
        }

        if (this.slim) {
            this.messageSocket.disconnect();
        } else {
            this.messageSocket.connect();
        }

        this.querySocket.connect();

        this.wake();
    }

    @Override
    public void calculateScopes() {
        boolean old = this.slim;

        super.calculateScopes();

        if (old != this.slim) {
            this.tryExternalHook();
        }
    }

    @Override
    protected void update0() {
        try {
            if (!this.slim) {
                CaffeineFollowersListRequest request = new CaffeineFollowersListRequest((CaffeineAuth) Koi.getInstance().getAuthProvider(UserPlatform.CAFFEINE));

                request.setCAID(this.UUID);

                List<CaffeineFollower> followers = request.send();

                for (CaffeineFollower follower : followers) {
                    if (this.followers.add(follower.getCAID())) {
                        SerializedUser user = CaffeineUserConverter.getInstance().get(follower.getCAID());

                        this.broadcastEvent(new FollowEvent(user, this));
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    @Override
    protected void updateUser() throws IdentifierException {
        CaffeineUserInfoRequest request = new CaffeineUserInfoRequest();

        request.setCAID(this.UUID);

        try {
            this.updateUser(request.send());
        } catch (Exception e) {
            if (e.getMessage().contains("User does not exist")) {
                throw new IdentifierException();
            } else {
                ErrorReporting.uncaughterror(e);
            }
        }
    }

    @Override
    public void updateUser(@Nullable Object obj) {
        if ((obj != null) && obj instanceof CaffeineUserInfoRequest.CaffeineUser) {
            CaffeineUserInfoRequest.CaffeineUser data = (CaffeineUserInfoRequest.CaffeineUser) obj;

            this.UUID = data.getCAID();
            this.setUsername(data.getUsername());
            this.imageLink = data.getImageLink();
            this.followerCount = data.getFollowersCount();

            this.broadcastEvent(new UserUpdateEvent(this));
        }
    }

    public static class Provider implements UserProvider {
        @Override
        public User get(String identifier, Object data) throws IdentifierException {
            return new CaffeineUser(identifier, data);
        }
    }

    @Override
    public void streamStateChanged(boolean isLive, String title) {
        this.broadcastEvent(new StreamStatusEvent(isLive, title, this));
    }

    @Override
    public void onChat(co.casterlabs.caffeineapi.realtime.messages.ChatEvent event) {
        this.broadcastEvent(this.convert(event));
    }

    @Override
    public void onShare(co.casterlabs.caffeineapi.realtime.messages.ShareEvent event) {
        this.broadcastEvent(this.convert(event));
    }

    @Override
    public void onProp(co.casterlabs.caffeineapi.realtime.messages.PropEvent event) {
        this.broadcastEvent(this.convert(event));
    }

    @Override
    public void onUpvote(co.casterlabs.caffeineapi.realtime.messages.UpvoteEvent event) {
        this.broadcastEvent(new UpvoteEvent(this.convert(event.getEvent()), event.getUpvotes()));
    }

    @Override
    public void onClose() {
        this.tryExternalHook();
    }

    private ChatEvent convert(co.casterlabs.caffeineapi.realtime.messages.ChatEvent event) {
        SerializedUser sender = CaffeineUserConverter.getInstance().transform(event.getSender());

        if (event instanceof PropEvent) {
            PropEvent propEvent = (PropEvent) event;

            return new DonationEvent(event.getId(), event.getMessage(), sender, this, propEvent.getProp().getStaticImagePath(), "DIGIES", propEvent.getProp().getCredits(), propEvent.getProp().getUniversalVideoPropPath());
        } else {
            return new ChatEvent(event.getId(), event.getMessage(), sender, this);
        }
    }

    @Override
    public void onOpen() {} // Not used, but the compiler wants it.

}
