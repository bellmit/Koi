package co.casterlabs.koi.integration.caffeine.user;

import java.util.ArrayList;
import java.util.List;

import co.casterlabs.caffeineapi.realtime.messages.CaffeineMessages;
import co.casterlabs.caffeineapi.realtime.messages.CaffeineMessagesListener;
import co.casterlabs.caffeineapi.realtime.messages.ShareEvent;
import co.casterlabs.caffeineapi.requests.CaffeineProp;
import co.casterlabs.koi.Koi;
import co.casterlabs.koi.client.ConnectionHolder;
import co.casterlabs.koi.events.ChatEvent;
import co.casterlabs.koi.events.DonationEvent;
import co.casterlabs.koi.events.DonationEvent.Donation;
import co.casterlabs.koi.events.DonationEvent.DonationType;
import co.casterlabs.koi.events.Event;
import co.casterlabs.koi.events.FollowEvent;
import co.casterlabs.koi.events.MessageMetaEvent;
import co.casterlabs.koi.user.User;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

@NonNull
@AllArgsConstructor
public class CaffeineMessagesListenerAdapter implements CaffeineMessagesListener {
    private CaffeineMessages conn;
    private ConnectionHolder holder;

    private void updateHeldUpvoteCount(MessageMetaEvent e) {
        for (Event held : new ArrayList<>(this.holder.getHeldCatchupEvents())) {
            ChatEvent chat = (ChatEvent) held;

            if (chat.getId().equals(e.getId())) {
                chat.setUpvotes(e.getUpvotes());
                return;
            }
        }
    }

    private void holdChatEvent(ChatEvent e) {
        this.holder.getHeldCatchupEvents().add(e);

        // Shift the list over, keeps it capped at 100 message history.
        if (this.holder.getHeldCatchupEvents().size() > 100) {
            this.holder.getHeldCatchupEvents().remove(0);
        }
    }

    @Override
    public void onShare(ShareEvent event) { // Not used in Casterlabs
        this.onChat(event);
    }

    @Override
    public void onChat(co.casterlabs.caffeineapi.realtime.messages.ChatEvent event) {
        User sender = CaffeineUserConverter.getInstance().transform(event.getSender());

        ChatEvent e = new ChatEvent("chat:" + event.getId(), event.getMessage(), sender, this.holder.getProfile());

        e.abilities.setUpvotable(true);

        this.holdChatEvent(e);

        this.holder.broadcastEvent(e);
    }

    @Override
    public void onProp(co.casterlabs.caffeineapi.realtime.messages.PropEvent event) {
        User sender = CaffeineUserConverter.getInstance().transform(event.getSender());
        CaffeineProp prop = event.getProp();

        List<Donation> donations = new ArrayList<>();

        for (int i = 0; i < event.getAmount(); i++) {
            donations.add(
                new Donation(
                    prop.getPreviewImagePath(),
                    "CAFFEINE_CREDITS",
                    prop.getCredits(),
                    prop.getStaticImagePath(),
                    DonationType.CAFFEINE_PROP,
                    prop.getName()
                )
            );
        }

        DonationEvent e = new DonationEvent("chat:" + event.getId(), event.getMessage(), sender, this.holder.getProfile(), donations);

        e.abilities.setUpvotable(true);

        this.holdChatEvent(e);

        this.holder.broadcastEvent(e);
    }

    @Override
    public void onUpvote(co.casterlabs.caffeineapi.realtime.messages.UpvoteEvent event) {
        MessageMetaEvent e = new MessageMetaEvent(this.holder.getProfile(), "chat:" + event.getEvent().getId());

        e.setUpvotes(event.getUpvotes());

        this.updateHeldUpvoteCount(e);

        this.holder.broadcastEvent(e);
    }

    @Override
    public void onFollow(co.casterlabs.caffeineapi.realtime.messages.FollowEvent event) {
        User follower = CaffeineUserConverter.getInstance().transform(event.getFollower());
        FollowEvent e = new FollowEvent(follower, this.holder.getProfile());

        this.holder.broadcastEvent(e);
    }

    @Override
    public void onClose(boolean remote) {
        if (!this.holder.isExpired()) {
            Koi.clientThreadPool.submit(() -> this.conn.connect());
        } else {
            FastLogger.logStatic(LogLevel.DEBUG, "Closed messages for %s", this.holder.getSimpleProfile());
        }
    }

}
