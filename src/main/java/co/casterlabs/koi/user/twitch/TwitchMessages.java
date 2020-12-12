package co.casterlabs.koi.user.twitch;

import java.util.HashMap;
import java.util.Map;

import com.gikk.twirk.Twirk;
import com.gikk.twirk.enums.EMOTE_SIZE;
import com.gikk.twirk.events.TwirkListener;
import com.gikk.twirk.types.cheer.Cheer;
import com.gikk.twirk.types.cheer.CheerSize;
import com.gikk.twirk.types.cheer.CheerTheme;
import com.gikk.twirk.types.cheer.CheerType;
import com.gikk.twirk.types.emote.Emote;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;

import co.casterlabs.koi.Koi;
import co.casterlabs.koi.events.ChatEvent;
import co.casterlabs.koi.events.DonationEvent;
import co.casterlabs.koi.user.SerializedUser;
import co.casterlabs.koi.user.UserPlatform;
import lombok.Getter;

public class TwitchMessages implements TwirkListener {
    private @Getter Twirk twirk;
    private TwitchUser user;

    public TwitchMessages(TwitchUser user) {
        this.user = user;
        this.reconnect();
    }

    private void reconnect() {
        try {
            this.twirk = ((TwitchAuth) Koi.getInstance().getAuthProvider(UserPlatform.TWITCH)).getTwirk(this.user.getUsername());

            this.twirk.addIrcListener(this);
            this.twirk.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDisconnect() {
        if (this.user.hasListeners()) {
            Koi.getMiscThreadPool().execute(() -> this.reconnect());
        }
    }

    @Override
    public void onPrivMsg(com.gikk.twirk.types.users.TwitchUser user, TwitchMessage message) {
        SerializedUser sender = TwitchUserConverter.getInstance().transform(user);
        ChatEvent event;

        if (message.isCheer()) {
            Cheer cheer = message.getCheers().get(0);

            event = new DonationEvent(message.getMessageID(), message.getContent(), sender, this.user, cheer.getImageURL(CheerTheme.DARK, CheerType.STATIC, CheerSize.LARGE), "BITS", message.getBits(), cheer.getImageURL(CheerTheme.DARK, CheerType.ANIMATED, CheerSize.LARGE));
        } else {
            event = new ChatEvent(message.getMessageID(), message.getContent(), sender, this.user);
        }

        if (message.hasEmotes()) {
            Map<String, String> emotes = new HashMap<>();

            for (Emote emote : message.getEmotes()) {
                emotes.put(emote.getPattern(), emote.getEmoteImageUrl(EMOTE_SIZE.LARGE).replace("http://", "https://"));
            }

            event.setEmotes(emotes);
        }

        this.user.broadcastEvent(event);
    }

    public void close() {
        this.twirk.close();
    }

}
