package co.casterlabs.koi.user.caffeine;

import java.util.Arrays;

import co.casterlabs.caffeineapi.realtime.messages.CaffeineMessages;
import co.casterlabs.caffeineapi.realtime.messages.CaffeineMessagesListener;
import co.casterlabs.caffeineapi.realtime.messages.ShareEvent;
import co.casterlabs.caffeineapi.requests.CaffeineProp;
import co.casterlabs.koi.events.ChatEvent;
import co.casterlabs.koi.events.DonationEvent;
import co.casterlabs.koi.events.DonationEvent.Donation;
import co.casterlabs.koi.events.FollowEvent;
import co.casterlabs.koi.events.UpvoteEvent;
import co.casterlabs.koi.user.ConnectionHolder;
import co.casterlabs.koi.user.User;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

@AllArgsConstructor
public class CaffeineMessagesListenerAdapter implements CaffeineMessagesListener {
    private @NonNull CaffeineMessages conn;
    private @NonNull ConnectionHolder holder;

    @Override
    public void onShare(ShareEvent event) { // Not used in Casterlabs
        this.onChat(event);
    }

    @Override
    public void onChat(co.casterlabs.caffeineapi.realtime.messages.ChatEvent event) {
        User sender = CaffeineUserConverter.getInstance().transform(event.getSender());

        ChatEvent e = new ChatEvent(event.getId(), event.getMessage(), sender, this.holder.getProfile());

        this.holder.broadcastEvent(e);
    }

    @Override
    public void onProp(co.casterlabs.caffeineapi.realtime.messages.PropEvent event) {
        User sender = CaffeineUserConverter.getInstance().transform(event.getSender());
        CaffeineProp prop = event.getProp();

        Donation donation = new Donation(prop.getPreviewImagePath(), "CAFFEINE_CREDITS", event.getAmount() * prop.getCredits(), prop.getStaticImagePath());
        DonationEvent e = new DonationEvent(event.getId(), event.getMessage(), sender, this.holder.getProfile(), Arrays.asList(donation));

        this.holder.broadcastEvent(e);
    }

    @Override
    public void onUpvote(co.casterlabs.caffeineapi.realtime.messages.UpvoteEvent event) {
        UpvoteEvent e = new UpvoteEvent(this.holder.getProfile(), event.getEvent().getId(), event.getUpvotes());

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
            this.conn.connect();
        } else {
            FastLogger.logStatic(LogLevel.DEBUG, "Closed messages for %s;%s", this.holder.getProfile().getUUID(), this.holder.getProfile().getPlatform());
        }
    }

}
