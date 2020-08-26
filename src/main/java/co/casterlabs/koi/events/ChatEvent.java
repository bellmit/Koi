package co.casterlabs.koi.events;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import co.casterlabs.koi.Koi;
import co.casterlabs.koi.user.IdentifierException;
import co.casterlabs.koi.user.SerializedUser;
import co.casterlabs.koi.user.User;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class ChatEvent extends Event {
    private static final Pattern MENTION_PATTERN = Pattern.compile("\\B@\\w+");

    private List<Mention> mentions = new ArrayList<>();
    private SerializedUser sender;
    private String message;
    private User streamer;
    private String id;

    public ChatEvent(String id, String message, SerializedUser sender, User streamer) {
        this.streamer = streamer;
        this.message = message;
        this.sender = sender;
        this.id = id;

        Matcher m = MENTION_PATTERN.matcher(this.message);
        while (m.find()) {
            String mention = m.group();

            try {
                SerializedUser user = Koi.getInstance().getUserSerialized(mention.substring(1), this.streamer.getPlatform());

                this.mentions.add(new Mention(user, mention));
            } catch (IdentifierException ignored) {} // They don't exist
        }
    }

    @Override
    public EventType getType() {
        return EventType.CHAT;
    }

}
