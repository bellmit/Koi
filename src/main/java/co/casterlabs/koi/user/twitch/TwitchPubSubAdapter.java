package co.casterlabs.koi.user.twitch;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import co.casterlabs.koi.Koi;
import co.casterlabs.koi.events.ChannelPointsEvent;
import co.casterlabs.koi.events.DonationEvent;
import co.casterlabs.koi.events.DonationEvent.Donation;
import co.casterlabs.koi.events.DonationEvent.DonationType;
import co.casterlabs.koi.events.SubscriptionEvent;
import co.casterlabs.koi.events.SubscriptionEvent.SubscriptionLevel;
import co.casterlabs.koi.events.SubscriptionEvent.SubscriptionType;
import co.casterlabs.koi.user.ConnectionHolder;
import co.casterlabs.koi.user.User;
import co.casterlabs.twitchapi.helix.CheermoteCache;
import co.casterlabs.twitchapi.helix.CheermoteCache.CheermoteMatch;
import co.casterlabs.twitchapi.pubsub.PubSubError;
import co.casterlabs.twitchapi.pubsub.PubSubListenRequest;
import co.casterlabs.twitchapi.pubsub.PubSubListener;
import co.casterlabs.twitchapi.pubsub.PubSubRouter;
import co.casterlabs.twitchapi.pubsub.PubSubTopic;
import co.casterlabs.twitchapi.pubsub.messages.BitsV2TopicMessage;
import co.casterlabs.twitchapi.pubsub.messages.ChannelPointsV1TopicMessage;
import co.casterlabs.twitchapi.pubsub.messages.PubSubMessage;
import co.casterlabs.twitchapi.pubsub.messages.SubscriptionsV1TopicMessage;
import lombok.NonNull;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

public class TwitchPubSubAdapter {
    private static PubSubRouter router = new PubSubRouter();

    public static Closeable hook(@NonNull ConnectionHolder holder, TwitchTokenAuth twitchAuth) {
        PubSubListenRequest request = new PubSubListenRequest(twitchAuth, new PubSubListener() {

            @Override
            public void onError(PubSubError error) {
                if (error == PubSubError.DISCONNECTED) {
                    // Recursively hook, since variables must be effectively final.
                    holder.setCloseable(hook(holder, twitchAuth));
                } else {
                    FastLogger.logStatic(LogLevel.SEVERE, "Twitch PubSub error: %s", error);
                }
            }

            @Override
            public void onMessage(PubSubMessage message) {
                if (message.getType() == PubSubTopic.BITS_v2) {
                    BitsV2TopicMessage bitsMessage = (BitsV2TopicMessage) message;

                    User sender = TwitchUserConverter.getInstance().get(bitsMessage.getUsername());

                    List<Donation> donations = new ArrayList<>();
                    Map<String, String> emotes = new HashMap<>();

                    Map<String, CheermoteMatch> cheermotes = CheermoteCache.getCheermotesInMessage(bitsMessage.getChatMessage());

                    for (Map.Entry<String, CheermoteMatch> match : cheermotes.entrySet()) {
                        String animatedImage = match.getValue().getTier().getImages().getDark().getAnimated().getLargeImageLink();
                        String staticImage = match.getValue().getTier().getImages().getDark().getStill().getLargeImageLink();

                        emotes.put(match.getKey(), animatedImage);

                        //@formatter:off
                        donations.add(
                            new Donation(
                                animatedImage, 
                                "TWITCH_BITS", 
                                match.getValue().getAmount(), 
                                staticImage, 
                                DonationType.TWITCH_BITS
                            )
                        );
                        //@formatter:on
                    }

                    DonationEvent event = new DonationEvent(bitsMessage.getMessageId(), bitsMessage.getChatMessage(), sender, holder.getProfile(), donations);

                    event.setEmotes(emotes);

                    holder.broadcastEvent(event);
                } else if (message.getType() == PubSubTopic.SUBSCRIPTIONS_v1) {
                    SubscriptionsV1TopicMessage subMessage = (SubscriptionsV1TopicMessage) message;

                    SubscriptionType type = SubscriptionType.valueOf(subMessage.getContext().name());
                    SubscriptionLevel level;

                    switch (subMessage.getSubPlan()) {
                        case PRIME:
                            level = SubscriptionLevel.TWITCH_PRIME;
                            break;

                        case TIER_1:
                            level = SubscriptionLevel.TIER_1;
                            break;

                        case TIER_2:
                            level = SubscriptionLevel.TIER_2;
                            break;

                        case TIER_3:
                            level = SubscriptionLevel.TIER_3;
                            break;

                        default:
                            level = SubscriptionLevel.UNKNOWN;
                            break;
                    }

                    User subscriber = null;
                    User giftee = null;

                    if (!subMessage.isAnonymous()) {
                        subscriber = TwitchUserConverter.getInstance().get(subMessage.getUsername());
                    }

                    if (subMessage.isGift()) {
                        giftee = TwitchUserConverter.getInstance().get(subMessage.getRecipientUsername());
                    }

                    SubscriptionEvent event = new SubscriptionEvent(subscriber, holder.getProfile(), subMessage.getMonthDuration(), giftee, type, level);

                    holder.broadcastEvent(event);
                } else if (message.getType() == PubSubTopic.CHANNEL_POINTS_V1) {
                    ChannelPointsV1TopicMessage pointsMessage = (ChannelPointsV1TopicMessage) message;

                    User sender = TwitchUserConverter.getInstance().get(pointsMessage.getUser().getUsername());

                    String rewardJson = Koi.GSON.toJson(pointsMessage.getReward());
                    ChannelPointsEvent.ChannelPointsReward reward = Koi.GSON.fromJson(rewardJson, ChannelPointsEvent.ChannelPointsReward.class);

                    if (pointsMessage.getReward().getImage() != null) {
                        reward.setRewardImage(pointsMessage.getReward().getImage().getLargeImage());
                    }

                    if (pointsMessage.getReward().getDefaultImage() != null) {
                        reward.setDefaultRewardImage(pointsMessage.getReward().getDefaultImage().getLargeImage());
                    }

                    ChannelPointsEvent.RedemptionStatus status = ChannelPointsEvent.RedemptionStatus.valueOf(pointsMessage.getStatus().name());

                    ChannelPointsEvent event = new ChannelPointsEvent(sender, holder.getProfile(), reward, status, pointsMessage.getId(), pointsMessage.getUserInput());

                    holder.broadcastEvent(event);
                }
            }
        });

        request.addTopic(PubSubTopic.BITS_v2, holder.getProfile().getUUID());
        request.addTopic(PubSubTopic.SUBSCRIPTIONS_v1, holder.getProfile().getUUID());
        request.addTopic(PubSubTopic.CHANNEL_POINTS_V1, holder.getProfile().getUUID());

        router.subscribeTopic(request);

        return new Closeable() {

            @Override
            public void close() throws IOException {
                request.setUnlistenMode(true);

                router.unsubscribeTopic(request);
            }

        };
    }

}
