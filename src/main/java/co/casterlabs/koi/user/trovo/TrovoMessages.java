package co.casterlabs.koi.user.trovo;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.apiutil.web.ApiException;
import co.casterlabs.koi.client.ConnectionHolder;
import co.casterlabs.koi.events.ChatEvent;
import co.casterlabs.koi.events.DonationEvent;
import co.casterlabs.koi.events.DonationEvent.Donation;
import co.casterlabs.koi.events.DonationEvent.DonationType;
import co.casterlabs.koi.events.FollowEvent;
import co.casterlabs.koi.events.SubscriptionEvent;
import co.casterlabs.koi.events.SubscriptionEvent.SubscriptionLevel;
import co.casterlabs.koi.events.SubscriptionEvent.SubscriptionType;
import co.casterlabs.koi.events.ViewerJoinEvent;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.User.UserRoles;
import co.casterlabs.trovoapi.chat.ChatListener;
import co.casterlabs.trovoapi.chat.EmoteCache;
import co.casterlabs.trovoapi.chat.TrovoChat;
import co.casterlabs.trovoapi.chat.TrovoSpell;
import co.casterlabs.trovoapi.chat.TrovoSpell.TrovoSpellCurrency;
import co.casterlabs.trovoapi.chat.TrovoSubLevel;
import co.casterlabs.trovoapi.chat.TrovoUserMedal;
import co.casterlabs.trovoapi.chat.TrovoUserRoles;
import co.casterlabs.trovoapi.chat.messages.TrovoChatMessage;
import co.casterlabs.trovoapi.chat.messages.TrovoCustomSpellMessage;
import co.casterlabs.trovoapi.chat.messages.TrovoFollowMessage;
import co.casterlabs.trovoapi.chat.messages.TrovoGiftSubMessage;
import co.casterlabs.trovoapi.chat.messages.TrovoGiftSubRandomlyMessage;
import co.casterlabs.trovoapi.chat.messages.TrovoMagicChatMessage;
import co.casterlabs.trovoapi.chat.messages.TrovoMessage;
import co.casterlabs.trovoapi.chat.messages.TrovoSpellMessage;
import co.casterlabs.trovoapi.chat.messages.TrovoSubscriptionMessage;
import co.casterlabs.trovoapi.chat.messages.TrovoWelcomeMessage;

public class TrovoMessages implements ChatListener, Closeable {
    private TrovoChat connection;
    private ConnectionHolder holder;

    private EmoteCache globalEmoteCache = new EmoteCache();
    private EmoteCache channelEmoteCache;

    private boolean isNew = true;

    public TrovoMessages(ConnectionHolder holder, TrovoUserAuth auth) throws ApiAuthException, ApiException, IOException {
        this.holder = holder;
        this.connection = new TrovoChat(auth);

        this.connection.setListener(this);

        this.connection.connect();

        this.channelEmoteCache = new EmoteCache(this.holder.getSimpleProfile().getUUID());
    }

    @Override
    public void onBatchMessages(List<TrovoMessage> messages) {
        // Skip the initial message history.
        if (this.isNew) {
            this.isNew = false;
        } else {
            ChatListener.super.onBatchMessages(messages);
        }
    }

    @Override
    public void onChatMessage(TrovoChatMessage message) {
        User user = TrovoUserConverter.getInstance().get(message.getSenderNickname());

        user.setImageLink(message.getSenderAvatar());

        user.getBadges().clear();
        user.getRoles().clear();

        if (message.getSenderMedals() != null) {
            for (TrovoUserMedal medal : message.getSenderMedals()) {
                user.getBadges().add(medal.getImage());
            }
        }

        if (message.getSenderRoles() != null) {
            for (TrovoUserRoles role : message.getSenderRoles()) {
                switch (role) {
                    case FOLLOWER:
                        user.getRoles().add(UserRoles.FOLLOWER);
                        break;

                    case MOD:
                        user.getRoles().add(UserRoles.MODERATOR);
                        break;

                    case STREAMER:
                        user.getRoles().add(UserRoles.BROADCASTER);
                        break;

                    case SUBSCRIBER:
                        user.getRoles().add(UserRoles.SUBSCRIBER);
                        break;

                    case ADMIN:
                    case WARDEN:
                        user.getRoles().add(UserRoles.STAFF);
                        break;
                }
            }
        }

        ChatEvent event = new ChatEvent("chat:" + message.getMessageId() + ":" + message.getSenderId(), message.getMessage(), user, this.holder.getProfile());

        event.getEmotes().putAll(this.channelEmoteCache.parseEmotes(message.getMessage()));
        event.getEmotes().putAll(this.globalEmoteCache.parseEmotes(message.getMessage()));

        this.holder.broadcastEvent(event);
    }

    @Override
    public void onFollow(TrovoFollowMessage message) {
        User user = TrovoUserConverter.getInstance().get(message.getFollowerNickname());

        user.setImageLink(message.getFollowerAvatar());

        user.getBadges().clear();
        user.getRoles().clear();

        if (message.getFollowerMedals() != null) {
            for (TrovoUserMedal medal : message.getFollowerMedals()) {
                user.getBadges().add(medal.getImage());
            }
        }

        if (message.getFollowerRoles() != null) {
            for (TrovoUserRoles role : message.getFollowerRoles()) {
                switch (role) {
                    case FOLLOWER:
                        user.getRoles().add(UserRoles.FOLLOWER);
                        break;

                    case MOD:
                        user.getRoles().add(UserRoles.MODERATOR);
                        break;

                    case STREAMER:
                        user.getRoles().add(UserRoles.BROADCASTER);
                        break;

                    case SUBSCRIBER:
                        user.getRoles().add(UserRoles.SUBSCRIBER);
                        break;

                    case ADMIN:
                    case WARDEN:
                        user.getRoles().add(UserRoles.STAFF);
                        break;
                }
            }
        }

        this.holder.broadcastEvent(new FollowEvent(user, this.holder.getProfile()));
    }

    @Override
    public void onSpell(TrovoSpellMessage message) {
        User user = TrovoUserConverter.getInstance().get(message.getSenderNickname());

        user.setImageLink(message.getSenderAvatar());

        user.getBadges().clear();
        user.getRoles().clear();

        if (message.getSenderMedals() != null) {
            for (TrovoUserMedal medal : message.getSenderMedals()) {
                user.getBadges().add(medal.getImage());
            }
        }

        if (message.getSenderRoles() != null) {
            for (TrovoUserRoles role : message.getSenderRoles()) {
                switch (role) {
                    case FOLLOWER:
                        user.getRoles().add(UserRoles.FOLLOWER);
                        break;

                    case MOD:
                        user.getRoles().add(UserRoles.MODERATOR);
                        break;

                    case STREAMER:
                        user.getRoles().add(UserRoles.BROADCASTER);
                        break;

                    case SUBSCRIBER:
                        user.getRoles().add(UserRoles.SUBSCRIBER);
                        break;

                    case ADMIN:
                    case WARDEN:
                        user.getRoles().add(UserRoles.STAFF);
                        break;
                }
            }
        }

        TrovoSpell spell = message.getSpell();

        //@formatter:off
        List<Donation> donations = Arrays.asList(
                new Donation(
                    spell.getAnimatedImage(), 
                    "TROVO_" + spell.getCurrency(), 
                    (spell.getCurrency() == TrovoSpellCurrency.MANA) ? 0 : spell.getCost(), 
                    spell.getStaticImage(), 
                    DonationType.TROVO_SPELL,
                    spell.getName()
                )
        );
        //@formatter:on

        this.holder.broadcastEvent(new DonationEvent("chat:" + message.getMessageId() + ":" + message.getSenderId(), "", user, this.holder.getProfile(), donations));
    }

    @Override
    public void onCustomSpell(TrovoCustomSpellMessage message) {
        User user = TrovoUserConverter.getInstance().get(message.getSenderNickname());

        user.setImageLink(message.getSenderAvatar());

        user.getBadges().clear();
        user.getRoles().clear();

        if (message.getSenderMedals() != null) {
            for (TrovoUserMedal medal : message.getSenderMedals()) {
                user.getBadges().add(medal.getImage());
            }
        }

        if (message.getSenderRoles() != null) {
            for (TrovoUserRoles role : message.getSenderRoles()) {
                switch (role) {
                    case FOLLOWER:
                        user.getRoles().add(UserRoles.FOLLOWER);
                        break;

                    case MOD:
                        user.getRoles().add(UserRoles.MODERATOR);
                        break;

                    case STREAMER:
                        user.getRoles().add(UserRoles.BROADCASTER);
                        break;

                    case SUBSCRIBER:
                        user.getRoles().add(UserRoles.SUBSCRIBER);
                        break;

                    case ADMIN:
                    case WARDEN:
                        user.getRoles().add(UserRoles.STAFF);
                        break;
                }
            }
        }

        String image = message.getImageLink(this.holder.getSimpleProfile().getUUID());

        //@formatter:off
        List<Donation> donations = Arrays.asList(
                new Donation(
                    image, 
                    "TROVO_ELIXIR", 
                    0, // TODO GET VALUE FROM TROVO'S API AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA 
                    image.replace("/webp", "/png"), // Image view api.
                    DonationType.TROVO_SPELL,
                    message.getGift()
                )
        );
        //@formatter:on

        this.holder.broadcastEvent(new DonationEvent("chat:" + message.getMessageId() + ":" + message.getSenderId(), "", user, this.holder.getProfile(), donations));
    }

    @Override
    public void onWelcome(TrovoWelcomeMessage message) {
        User user = TrovoUserConverter.getInstance().get(message.getViewerNickname());

        user.setImageLink(message.getViewerAvatar());

        user.getBadges().clear();
        user.getRoles().clear();

        if (message.getViewerMedals() != null) {
            for (TrovoUserMedal medal : message.getViewerMedals()) {
                user.getBadges().add(medal.getImage());
            }
        }

        if (message.getViewerRoles() != null) {
            for (TrovoUserRoles role : message.getViewerRoles()) {
                switch (role) {
                    case FOLLOWER:
                        user.getRoles().add(UserRoles.FOLLOWER);
                        break;

                    case MOD:
                        user.getRoles().add(UserRoles.MODERATOR);
                        break;

                    case STREAMER:
                        user.getRoles().add(UserRoles.BROADCASTER);
                        break;

                    case SUBSCRIBER:
                        user.getRoles().add(UserRoles.SUBSCRIBER);
                        break;

                    case ADMIN:
                    case WARDEN:
                        user.getRoles().add(UserRoles.STAFF);
                        break;
                }
            }
        }

        if (message.getViewerMedals() != null) {
            for (TrovoUserMedal medal : message.getViewerMedals()) {
                user.getBadges().add(medal.getImage());
            }
        }

        this.holder.broadcastEvent(new ViewerJoinEvent(user, this.holder.getProfile()));
    }

    @Override
    public void onGiftSub(TrovoGiftSubMessage message) {
        User subscriber = TrovoUserConverter.getInstance().get(message.getSenderNickname());
        User giftee = TrovoUserConverter.getInstance().get(message.getGifteeNickname());
        SubscriptionLevel level = convertLevel(message.getSenderSubLevel());

        subscriber.setImageLink(message.getSenderAvatar());

        subscriber.getBadges().clear();
        subscriber.getRoles().clear();

        if (message.getSenderMedals() != null) {
            for (TrovoUserMedal medal : message.getSenderMedals()) {
                subscriber.getBadges().add(medal.getImage());
            }
        }

        if (message.getSenderRoles() != null) {
            for (TrovoUserRoles role : message.getSenderRoles()) {
                switch (role) {
                    case FOLLOWER:
                        subscriber.getRoles().add(UserRoles.FOLLOWER);
                        break;

                    case MOD:
                        subscriber.getRoles().add(UserRoles.MODERATOR);
                        break;

                    case STREAMER:
                        subscriber.getRoles().add(UserRoles.BROADCASTER);
                        break;

                    case SUBSCRIBER:
                        subscriber.getRoles().add(UserRoles.SUBSCRIBER);
                        break;

                    case ADMIN:
                    case WARDEN:
                        subscriber.getRoles().add(UserRoles.STAFF);
                        break;
                }
            }
        }

        SubscriptionEvent event = new SubscriptionEvent(subscriber, this.holder.getProfile(), 1, giftee, SubscriptionType.SUBGIFT, level);

        this.holder.broadcastEvent(event);
    }

    @Override
    public void onSubscription(TrovoSubscriptionMessage message) {
        User subscriber = TrovoUserConverter.getInstance().get(message.getSubscriberNickname());
        SubscriptionLevel level = convertLevel(message.getSubscriberSubLevel());

        subscriber.setImageLink(message.getSubscriberAvatar());

        subscriber.getBadges().clear();
        subscriber.getRoles().clear();

        if (message.getSubscriberMedals() != null) {
            for (TrovoUserMedal medal : message.getSubscriberMedals()) {
                subscriber.getBadges().add(medal.getImage());
            }
        }

        if (message.getSubscriberRoles() != null) {
            for (TrovoUserRoles role : message.getSubscriberRoles()) {
                switch (role) {
                    case FOLLOWER:
                        subscriber.getRoles().add(UserRoles.FOLLOWER);
                        break;

                    case MOD:
                        subscriber.getRoles().add(UserRoles.MODERATOR);
                        break;

                    case STREAMER:
                        subscriber.getRoles().add(UserRoles.BROADCASTER);
                        break;

                    case SUBSCRIBER:
                        subscriber.getRoles().add(UserRoles.SUBSCRIBER);
                        break;

                    case ADMIN:
                    case WARDEN:
                        subscriber.getRoles().add(UserRoles.STAFF);
                        break;
                }
            }
        }

        if (message.getSubscriberMedals() != null) {
            for (TrovoUserMedal medal : message.getSubscriberMedals()) {
                subscriber.getBadges().add(medal.getImage());
            }
        }

        SubscriptionEvent event = new SubscriptionEvent(subscriber, this.holder.getProfile(), 1, null, SubscriptionType.SUB, level);

        this.holder.broadcastEvent(event);
    }

    @Override
    public void onGiftSubRandomly(TrovoGiftSubRandomlyMessage message) {
        // ?
    }

    @Override
    public void onMagicChat(TrovoMagicChatMessage message) {
        User user = TrovoUserConverter.getInstance().get(message.getSenderNickname());

        user.setImageLink(message.getSenderAvatar());

        user.getBadges().clear();
        user.getRoles().clear();

        if (message.getSenderMedals() != null) {
            for (TrovoUserMedal medal : message.getSenderMedals()) {
                user.getBadges().add(medal.getImage());
            }
        }

        if (message.getSenderRoles() != null) {
            for (TrovoUserRoles role : message.getSenderRoles()) {
                switch (role) {
                    case FOLLOWER:
                        user.getRoles().add(UserRoles.FOLLOWER);
                        break;

                    case MOD:
                        user.getRoles().add(UserRoles.MODERATOR);
                        break;

                    case STREAMER:
                        user.getRoles().add(UserRoles.BROADCASTER);
                        break;

                    case SUBSCRIBER:
                        user.getRoles().add(UserRoles.SUBSCRIBER);
                        break;

                    case ADMIN:
                    case WARDEN:
                        user.getRoles().add(UserRoles.STAFF);
                        break;
                }
            }
        }

        String currency = null;
        String image = null;
        String name = null;
        int cost = 0;

        switch (message.getType()) {
            case MAGIC_CHAT_BULLET_SCREEN:
                cost = 1500;
                image = "https://assets.casterlabs.co/trovo/bulletscreen.png";
                currency = "TROVO_ELIXIR";
                name = "Bullet Screen";
                break;

            case MAGIC_CHAT_COLORFUL:
                cost = 300;
                image = "https://assets.casterlabs.co/trovo/colorfulchat.png";
                currency = "TROVO_ELIXIR";
                name = "Colorful Chat";
                break;

            case MAGIC_CHAT_SPELL:
                cost = 500;
                image = "https://assets.casterlabs.co/trovo/spellchat.png";
                currency = "TROVO_ELIXIR";
                name = "Spell";
                break;

            case MAGIC_CHAT_SUPER_CAP:
                cost = 0;
                image = "https://assets.casterlabs.co/trovo/spellchat.png";
                currency = "TROVO_MANA";
                name = "Super Cap";
                break;

            default:
                break;
        }

        //@formatter:off
        List<Donation> donations = Arrays.asList(
                new Donation(
                    image, 
                    currency, 
                    cost, 
                    image, 
                    DonationType.TROVO_SPELL,
                    name
                )
        );
        //@formatter:on

        this.holder.broadcastEvent(new DonationEvent("chat:" + message.getMessageId() + ":" + message.getSenderId(), message.getMessage(), user, this.holder.getProfile(), donations));
    }

    private static SubscriptionLevel convertLevel(TrovoSubLevel level) {
        switch (level) {
            case L1:
                return SubscriptionLevel.TIER_1;

            case L2:
                return SubscriptionLevel.TIER_2;

            case L3:
                return SubscriptionLevel.TIER_3;

            case L4:
                return SubscriptionLevel.TIER_4;

            case L5:
                return SubscriptionLevel.TIER_5;

            default:
                return null;

        }
    }

    @Override
    public void onClose(boolean remote) {
        if (!this.holder.isExpired()) {
            this.isNew = true;
            this.connection.connect();
        }
    }

    @Override
    public void close() throws IOException {
        this.connection.disconnect();
    }

}
