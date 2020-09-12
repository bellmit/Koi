package co.casterlabs.koi.user.twitch;

import com.gikk.twirk.Twirk;
import com.gikk.twirk.events.TwirkListener;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;

import co.casterlabs.koi.Koi;
import co.casterlabs.koi.events.ChatEvent;
import co.casterlabs.koi.events.DonationEvent;
import co.casterlabs.koi.events.Event;
import co.casterlabs.koi.user.SerializedUser;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.koi.util.TwirkUtil;
import lombok.SneakyThrows;

public class TwitchMessages implements TwirkListener {
    private TwitchUser user;
    private Twirk twirk;

    public TwitchMessages(TwitchUser user) {
        this.user = user;

        try {
            this.twirk = ((TwitchAuth) Koi.getInstance().getAuthProvider(UserPlatform.TWITCH)).getTwirk(this.user.getUsername());

            this.twirk.addIrcListener(this);
            this.twirk.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SneakyThrows
    @Override
    public void onDisconnect() {
        if (this.user.hasListeners()) {
            Koi.getMiscThreadPool().execute(() -> {
                try {
                    this.twirk.connect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    @Override
    public void onPrivMsg(com.gikk.twirk.types.users.TwitchUser user, TwitchMessage message) {
        SerializedUser sender = TwitchUserConverter.getInstance().transform(user);
        Event event;

        if (message.isCheer()) {
            event = new DonationEvent(message.getMessageID(), message.getContent(), sender, this.user, "", "BITS", message.getBits());
        } else {
            event = new ChatEvent(message.getMessageID(), message.getContent(), sender, this.user);
        }

        this.user.broadcastEvent(event);
    }

    public void close() {
        TwirkUtil.close(this.twirk);
    }

}
