package co.casterlabs.koi.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import co.casterlabs.koi.user.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Data
@EqualsAndHashCode(callSuper = false)
public class ChatEvent extends Event {
    private static final Pattern MENTION_PATTERN = Pattern.compile("\\B@\\w+");
    private static final Pattern LINK_PATTERN = Pattern.compile("(http(s)?:\\/\\/.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&//=]*)");

    private Map<String, String> emotes = new HashMap<>();
    private List<Mention> mentions = new ArrayList<>();
    private List<String> links = new ArrayList<>();
    private User streamer;
    private User sender;
    private String message;
    private String id;

    public ChatEvent(String id, String message, User sender, User streamer) {
        this.streamer = streamer;
        this.message = message;
        this.sender = sender;
        this.id = id;

        Matcher m = MENTION_PATTERN.matcher(this.message);
        while (m.find()) {
            try {
                String target = m.group().substring(1);
                User mentioned = sender.getPlatform().getConverter().get(target);

                if (mentioned != null) {
                    this.mentions.add(new Mention(target, mentioned));
                }
            } catch (Exception ignored) {}
        }

        Matcher l = LINK_PATTERN.matcher(this.message);
        while (l.find()) {
            this.links.add(l.group());
        }
    }

    @Override
    public EventType getType() {
        return EventType.CHAT;
    }

    @Getter
    @AllArgsConstructor
    public static class Mention {
        private String target;
        private User mentioned;

    }

}
