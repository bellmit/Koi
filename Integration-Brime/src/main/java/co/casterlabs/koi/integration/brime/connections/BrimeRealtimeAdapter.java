package co.casterlabs.koi.integration.brime.connections;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import co.casterlabs.brimeapijava.realtime.BrimeChatMessage;
import co.casterlabs.brimeapijava.realtime.BrimeChatMessage.BrimeChatEmote;
import co.casterlabs.brimeapijava.realtime.BrimeRealtime;
import co.casterlabs.brimeapijava.realtime.BrimeRealtimeListener;
import co.casterlabs.koi.Koi;
import co.casterlabs.koi.client.connection.Connection;
import co.casterlabs.koi.client.connection.ConnectionHolder;
import co.casterlabs.koi.events.ChatEvent;
import co.casterlabs.koi.events.FollowEvent;
import co.casterlabs.koi.events.MessageMetaEvent;
import co.casterlabs.koi.events.RaidEvent;
import co.casterlabs.koi.events.SubscriptionEvent;
import co.casterlabs.koi.events.SubscriptionEvent.SubscriptionLevel;
import co.casterlabs.koi.events.SubscriptionEvent.SubscriptionType;
import co.casterlabs.koi.integration.brime.data.BrimeUserConverter;
import co.casterlabs.koi.events.ViewerJoinEvent;
import co.casterlabs.koi.events.ViewerLeaveEvent;
import co.casterlabs.koi.events.ViewerListEvent;
import co.casterlabs.koi.user.User;
import lombok.RequiredArgsConstructor;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

@RequiredArgsConstructor
public class BrimeRealtimeAdapter implements BrimeRealtimeListener, Connection {
    private final ConnectionHolder holder;
    private final BrimeRealtime conn;

    private List<String> viewers = new LinkedList<>();
    private Set<String> oldViewersSet = new HashSet<>();

    private void holdChatEvent(ChatEvent e) {
        this.holder.getHeldCatchupEvents().add(e);

        // Shift the list over, keeps it capped at 100 message history.
        if (this.holder.getHeldCatchupEvents().size() > 100) {
            this.holder.getHeldCatchupEvents().remove(0);
        }
    }

    @Override
    public void onChat(BrimeChatMessage chat) {
        User sender = BrimeUserConverter.getInstance().transform(chat.getSender());

        ChatEvent e = new ChatEvent(chat.getMessageId(), chat.getMessage(), sender, this.holder.getProfile());

        e.abilities.setDeletable(true);

        for (Entry<String, BrimeChatEmote> entry : chat.getEmotes().entrySet()) {
            e.getEmotes().put(entry.getKey(), entry.getValue().get3xImageUrl());
        }

        this.holdChatEvent(e);

        this.holder.broadcastEvent(e);
    }

    @Override
    public void onChatDelete(String messageId) {
        MessageMetaEvent event = new MessageMetaEvent(this.holder.getProfile(), messageId);

        event.setVisible(false);

        this.holder.broadcastEvent(event);
    }

    @Override
    public void onSub(String username, String userId, boolean isResub) {
        User subscriber = BrimeUserConverter.getInstance().get(username);
        SubscriptionType type = isResub ? SubscriptionType.RESUB : SubscriptionType.SUB;

        SubscriptionEvent e = new SubscriptionEvent(subscriber, this.holder.getProfile(), 1, null, type, SubscriptionLevel.TIER_1);

        this.holder.broadcastEvent(e);
    }

    @Override
    public void onRaid(String channel, String channelId, int viewers) {
        User raider = BrimeUserConverter.getInstance().get(channel);

        RaidEvent e = new RaidEvent(raider, this.holder.getProfile(), viewers);

        this.holder.broadcastEvent(e);
    }

    @Override
    public void onFollow(String username, String userId) {
        User follower = BrimeUserConverter.getInstance().get(username);

        FollowEvent e = new FollowEvent(follower, this.holder.getProfile());

        this.holder.broadcastEvent(e);
    }

    @Override
    public void onJoin(String username) {
        this.viewers.add(username);

        this.updateViewers();
    }

    @Override
    public void onLeave(String username) {
        this.viewers.remove(username);

        this.updateViewers();
    }

    private synchronized void updateViewers() {
        Set<String> set = new HashSet<>(this.viewers);
        List<User> viewers = new ArrayList<>();

        for (String username : set) {
            User viewer = BrimeUserConverter.getInstance().get(username);

            if (viewer != null) {
                viewers.add(viewer);

                // JOIN
                if (!this.oldViewersSet.contains(username)) {
                    this.holder.broadcastEvent(new ViewerJoinEvent(viewer, this.holder.getProfile()));
                }
            }
        }

        for (String username : this.oldViewersSet) {
            // LEAVE
            if (!set.contains(username)) {
                User viewer = BrimeUserConverter.getInstance().get(username);

                if (viewer != null) {
                    this.holder.broadcastEvent(new ViewerLeaveEvent(viewer, this.holder.getProfile()));
                }
            }
        }

        this.oldViewersSet = set;

        ViewerListEvent event = new ViewerListEvent(viewers, this.holder.getProfile());

        this.holder.setHeldEvent(event);
        this.holder.broadcastEvent(event);
    }

    @Override
    public void onClose() {
        this.viewers.clear();
        this.oldViewersSet.clear();

        if (!this.holder.isExpired()) {
            Koi.clientThreadPool.submit(() -> this.conn.connect());
        } else {
            FastLogger.logStatic(LogLevel.DEBUG, "Closed chat for %s", this.holder.getSimpleProfile());
        }
    }

    @Override
    public void close() throws IOException {
        this.conn.close();
    }

    @Override
    public void open() throws IOException {
        this.conn.connect();
    }

    @Override
    public boolean isOpen() {
        return this.conn.isOpen();
    }

}
