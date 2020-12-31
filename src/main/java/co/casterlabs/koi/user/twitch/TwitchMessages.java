package co.casterlabs.koi.user.twitch;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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
import co.casterlabs.koi.events.DonationEvent.Donation;
import co.casterlabs.koi.events.ViewerListEvent;
import co.casterlabs.koi.user.ConnectionHolder;
import co.casterlabs.koi.user.IdentifierException;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserPlatform;

public class TwitchMessages implements TwirkListener, Closeable {
    private Twirk twirk;
    private ConnectionHolder holder;

    private Map<String, User> viewers = new HashMap<>();

    public TwitchMessages(ConnectionHolder holder) {
        this.holder = holder;
        this.reconnect();
    }

    private void reconnect() {
        try {
            this.twirk = ((TwitchCredentialsAuth) Koi.getInstance().getAuthProvider(UserPlatform.TWITCH)).getTwirk(this.holder.getProfile().getUsername());

            this.viewers.clear();
            this.holder.setHeldEvent(null);
            this.twirk.addIrcListener(this);
            this.twirk.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDisconnect() {
        if (!this.holder.getUsers().isEmpty()) {
            this.reconnect();
        }
    }

    @Override
    public void onPrivMsg(com.gikk.twirk.types.users.TwitchUser user, TwitchMessage message) {
        User sender = TwitchUserConverter.getInstance().transform(user);
        ChatEvent event;

        if (message.isCheer()) {
            List<Donation> donations = new ArrayList<>();

            for (Cheer cheer : message.getCheers()) {
                donations.add(new Donation(cheer.getImageURL(CheerTheme.DARK, CheerType.ANIMATED, CheerSize.LARGE), "TWITCH BITS", cheer.getBits(), cheer.getImageURL(CheerTheme.DARK, CheerType.STATIC, CheerSize.LARGE)));
            }

            event = new DonationEvent(message.getMessageID(), message.getContent(), sender, this.holder.getProfile(), donations);
        } else {
            event = new ChatEvent(message.getMessageID(), message.getContent(), sender, this.holder.getProfile());
        }

        if (message.hasEmotes()) {
            Map<String, String> emotes = new HashMap<>();

            for (Emote emote : message.getEmotes()) {
                emotes.put(emote.getPattern(), emote.getEmoteImageUrl(EMOTE_SIZE.LARGE).replace("http://", "https://"));
            }

            event.setEmotes(emotes);
        }

        this.holder.broadcastEvent(event);
    }

    @Override
    public void onJoin(String name) {
        try {
            this.viewers.put(name, TwitchUserConverter.getInstance().getByLogin(name));

            this.updateViewers();
        } catch (IdentifierException ignored) {}
    }

    @Override
    public void onPart(String name) {
        this.viewers.remove(name);

        this.updateViewers();
    }

    @Override
    public void onNamesList(Collection<String> names) {
        for (String name : names) {
            try {
                this.viewers.put(name, TwitchUserConverter.getInstance().getByLogin(name));
            } catch (IdentifierException ignored) {}
        }

        this.updateViewers();
    }

    private void updateViewers() {
        ViewerListEvent event = new ViewerListEvent(this.viewers.values(), this.holder.getProfile());

        this.holder.setHeldEvent(event);
        this.holder.broadcastEvent(event);
    }

    @Override
    public void close() {
        this.twirk.close();
    }

}
