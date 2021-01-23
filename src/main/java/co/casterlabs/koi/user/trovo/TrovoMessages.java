package co.casterlabs.koi.user.trovo;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.apiutil.web.ApiException;
import co.casterlabs.koi.events.ChatEvent;
import co.casterlabs.koi.events.DonationEvent;
import co.casterlabs.koi.events.DonationEvent.Donation;
import co.casterlabs.koi.events.DonationEvent.DonationType;
import co.casterlabs.koi.events.FollowEvent;
import co.casterlabs.koi.events.ViewerJoinEvent;
import co.casterlabs.koi.user.ConnectionHolder;
import co.casterlabs.koi.user.User;
import co.casterlabs.trovoapi.chat.ChatListener;
import co.casterlabs.trovoapi.chat.TrovoChat;
import co.casterlabs.trovoapi.chat.TrovoSpell;
import co.casterlabs.trovoapi.chat.TrovoSpell.TrovoSpellCurrency;
import co.casterlabs.trovoapi.chat.messages.TrovoChatMessage;
import co.casterlabs.trovoapi.chat.messages.TrovoFollowMessage;
import co.casterlabs.trovoapi.chat.messages.TrovoGiftSubMessage;
import co.casterlabs.trovoapi.chat.messages.TrovoGiftSubRandomlyMessage;
import co.casterlabs.trovoapi.chat.messages.TrovoMagicChatMessage;
import co.casterlabs.trovoapi.chat.messages.TrovoMessage;
import co.casterlabs.trovoapi.chat.messages.TrovoPlatformEventMessage;
import co.casterlabs.trovoapi.chat.messages.TrovoRaidWelcomeMessage;
import co.casterlabs.trovoapi.chat.messages.TrovoSpellMessage;
import co.casterlabs.trovoapi.chat.messages.TrovoSubscriptionMessage;
import co.casterlabs.trovoapi.chat.messages.TrovoWelcomeMessage;

public class TrovoMessages implements ChatListener, Closeable {
    private TrovoChat connection;
    private ConnectionHolder holder;

    private boolean isNew = true;

    public TrovoMessages(ConnectionHolder holder, TrovoUserAuth auth) throws ApiAuthException, ApiException, IOException {
        this.holder = holder;
        this.connection = new TrovoChat(auth);

        this.connection.connect();
    }

    @Override
    public void onBatchMessages(List<TrovoMessage> messages) {
        // Skip the initial message history.
        if (this.isNew) {
            this.isNew = false;
        } else {
            for (TrovoMessage message : messages) {
                switch (message.getType()) {
                    case CHAT:
                        this.onChatMessage((TrovoChatMessage) message);
                        break;

                    case FOLLOW:
                        this.onFollow((TrovoFollowMessage) message);
                        break;

                    case GIFT_SUB_RANDOM:
                        this.onGiftSubRandomly((TrovoGiftSubRandomlyMessage) message);
                        break;

                    case GIFT_SUB_USER:
                        this.onGiftSub((TrovoGiftSubMessage) message);
                        break;

                    case MAGIC_CHAT_BULLET_SCREEN:
                    case MAGIC_CHAT_COLORFUL:
                    case MAGIC_CHAT_SPELL:
                    case MAGIC_CHAT_SUPER_CAP:
                        this.onMagicChat((TrovoMagicChatMessage) message);
                        break;

                    case SPELL:
                        this.onSpell((TrovoSpellMessage) message);
                        break;

                    case PLATFORM_EVENT:
                        this.onPlatformEvent((TrovoPlatformEventMessage) message);
                        break;

                    case RAID_WELCOME:
                        this.onRaidWelcome((TrovoRaidWelcomeMessage) message);
                        break;

                    case SUBSCRIPTION:
                        this.onSubscription((TrovoSubscriptionMessage) message);
                        break;

                    case WELCOME:
                        this.onWelcome((TrovoWelcomeMessage) message);
                        break;

                    case UNKNOWN:
                    default:
                        break;
                }
            }
        }
    }

    @Override
    public void onChatMessage(TrovoChatMessage message) {
        User user = TrovoUserConverter.getInstance().get(message.getSenderNickname());

        user.setImageLink(message.getSenderAvatar());

        this.holder.broadcastEvent(new ChatEvent("-1", message.getMessage(), user, this.holder.getProfile()));
    }

    @Override
    public void onFollow(TrovoFollowMessage message) {
        User user = TrovoUserConverter.getInstance().get(message.getFollowerNickname());

        user.setImageLink(message.getFollowerAvatar());

        this.holder.broadcastEvent(new FollowEvent(user, this.holder.getProfile()));
    }

    @Override
    public void onSpell(TrovoSpellMessage message) {
        User user = TrovoUserConverter.getInstance().get(message.getSenderNickname());

        user.setImageLink(message.getSenderAvatar());

        TrovoSpell spell = message.getSpell();

        //@formatter:off
        List<Donation> donations = Arrays.asList(
                new Donation(
                    spell.getAnimatedImage(), 
                    "TROVO_" + spell.getCurrency(), 
                    (spell.getCurrency() == TrovoSpellCurrency.MANA) ? 0 : spell.getCost(), 
                    spell.getStaticImage(), 
                    DonationType.TROVO_SPELL
                )
        );
        //@formatter:on

        this.holder.broadcastEvent(new DonationEvent("-1", "", user, this.holder.getProfile(), donations));
    }

    @Override
    public void onWelcome(TrovoWelcomeMessage message) {
        User user = TrovoUserConverter.getInstance().get(message.getViewerNickname());

        user.setImageLink(message.getViewerAvatar());

        this.holder.broadcastEvent(new ViewerJoinEvent(user, this.holder.getProfile()));
    }

    @Override
    public void onGiftSub(TrovoGiftSubMessage message) {
        // TODO
    }

    @Override
    public void onGiftSubRandomly(TrovoGiftSubRandomlyMessage message) {
        // TODO
    }

    @Override
    public void onSubscription(TrovoSubscriptionMessage message) {
        // TODO
    }

    @Override
    public void onMagicChat(TrovoMagicChatMessage message) {
        // TODO
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
