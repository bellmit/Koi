package co.casterlabs.koi.user.twitch;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.gikk.twirk.Twirk;
import com.gikk.twirk.enums.EMOTE_SIZE;
import com.gikk.twirk.events.TwirkListener;
import com.gikk.twirk.types.cheer.Cheer;
import com.gikk.twirk.types.cheer.CheerSize;
import com.gikk.twirk.types.cheer.CheerTheme;
import com.gikk.twirk.types.cheer.CheerType;
import com.gikk.twirk.types.emote.Emote;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import co.casterlabs.koi.RepeatingThread;
import co.casterlabs.koi.events.ChatEvent;
import co.casterlabs.koi.events.DonationEvent;
import co.casterlabs.koi.events.DonationEvent.Donation;
import co.casterlabs.koi.events.DonationEvent.DonationType;
import co.casterlabs.koi.events.ViewerJoinEvent;
import co.casterlabs.koi.events.ViewerLeaveEvent;
import co.casterlabs.koi.events.ViewerListEvent;
import co.casterlabs.koi.user.ConnectionHolder;
import co.casterlabs.koi.user.IdentifierException;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.util.WebUtil;
import lombok.NonNull;

public class TwitchMessages implements TwirkListener, Closeable {
    private static final String CHANNEL_BADGE_ENDPOINT = "https://badges.twitch.tv/v1/badges/channels/%s/display";
    private static final String GLOBAL_BADGE_ENDPOINT = "https://badges.twitch.tv/v1/badges/global/display";

    private static JsonObject globalBadges = new JsonObject();

    private Twirk twirk;
    private ConnectionHolder holder;
    private TwitchTokenAuth auth;

    private Map<String, User> viewers = new HashMap<>();

    private JsonObject channelBadges = new JsonObject();
    private RepeatingThread badgeThread;

    static {
        new RepeatingThread("Twitch Badge Poll - Koi", TimeUnit.HOURS.toMillis(1), () -> {
            JsonObject response = WebUtil.jsonSendHttpGet(GLOBAL_BADGE_ENDPOINT, null, JsonObject.class);

            globalBadges = response.getAsJsonObject("badge_sets");
        }).start();
    }

    public TwitchMessages(ConnectionHolder holder, @NonNull TwitchTokenAuth auth) {
        this.holder = holder;
        this.auth = auth;

        this.badgeThread = new RepeatingThread("Twitch Badge Poll - Koi", TimeUnit.MINUTES.toMillis(15), () -> {
            JsonObject response = WebUtil.jsonSendHttpGet(String.format(CHANNEL_BADGE_ENDPOINT, this.holder.getProfile().getUUID()), null, JsonObject.class);

            this.channelBadges = response.getAsJsonObject("badge_sets");
        });

        this.badgeThread.start();

        this.reconnect();
    }

    public void sendMessage(@NonNull String message) {
        this.twirk.channelMessage(message);

        this.holder.broadcastEvent(new ChatEvent("-1", message, this.holder.getProfile(), this.holder.getProfile()));
    }

    private void reconnect() {
        try {
            this.twirk = this.auth.getTwirk(this.holder.getProfile().getLowername());

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
        if (this.holder.isExpired()) {
            this.badgeThread.stop();
        } else {
            this.reconnect();
        }
    }

    @Override
    public void onPrivMsg(com.gikk.twirk.types.users.TwitchUser user, TwitchMessage message) {
        User sender = TwitchUserConverter.getInstance().transform(user);
        ChatEvent event;

        for (String badgeData : user.getBadges()) {
            try {
                String link = this.getBadgeUrl(badgeData);

                sender.getBadges().add(link);
            } catch (Exception ignored) {}
        }

        if (message.isCheer()) {
            List<Donation> donations = new ArrayList<>();

            for (Cheer cheer : message.getCheers()) {
                //@formatter:off
                donations.add(
                    new Donation(
                        cheer.getImageURL(CheerTheme.DARK, CheerType.ANIMATED, CheerSize.LARGE), 
                        "TWITCH_BITS", 
                        cheer.getBits(), 
                        cheer.getImageURL(CheerTheme.DARK, CheerType.STATIC, CheerSize.LARGE), 
                        DonationType.TWITCH_BITS
                    )
                );
                //@formatter:on
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
            User user = TwitchUserConverter.getInstance().getByLogin(name);

            this.viewers.put(name, user);
            this.holder.broadcastEvent(new ViewerJoinEvent(user, this.holder.getProfile()));

            this.updateViewers();
        } catch (IdentifierException ignored) {}
    }

    @Override
    public void onPart(String name) {
        try {
            User user = TwitchUserConverter.getInstance().getByLogin(name);

            this.viewers.remove(name);
            this.holder.broadcastEvent(new ViewerLeaveEvent(user, this.holder.getProfile()));

            this.updateViewers();
        } catch (IdentifierException ignored) {}
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

}
