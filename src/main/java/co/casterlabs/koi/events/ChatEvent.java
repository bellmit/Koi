package co.casterlabs.koi.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import co.casterlabs.koi.user.SerializedUser;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class ChatEvent extends Event {
    private static final Pattern MENTION_PATTERN = Pattern.compile("\\B@\\w+");

    private Map<String, String> emotes = new HashMap<>();
    private List<String> mentions = new ArrayList<>();
    private SerializedUser streamer;
    private SerializedUser sender;
    private String message;
    private String id;

    public ChatEvent(String id, String message, SerializedUser sender, SerializedUser streamer) {
        this.streamer = streamer;
        this.message = message;
        this.sender = sender;
        this.id = id;

        Matcher m = MENTION_PATTERN.matcher(this.message);
        while (m.find()) {
            this.mentions.add(m.group());
        }
    }

    @Override
    public EventType getType() {
        return EventType.CHAT;
    }

}
