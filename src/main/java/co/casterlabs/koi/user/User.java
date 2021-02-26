package co.casterlabs.koi.user;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.annotations.SerializedName;

import co.casterlabs.koi.RepeatingThread;
import co.casterlabs.koi.client.SimpleProfile;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import xyz.e3ndr.javawebcolor.Color;

@Data
@RequiredArgsConstructor
public class User {
    private static final Pattern COLOR_PATTERN = Pattern.compile("(\\[color:.*\\])|(\\[c:.*\\])");

    private static int startingPoint = 0;
    private static final String[] COLORS = new String[] {
            "#FF0000",
            "#FF8000",
            "#FFFF00",
            "#80FF00",
            "#00FF00",
            "#00FF80",
            "#00FFFF",
            "#0080FF",
            "#0000FF",
            "#7F00FF",
            "#FF00FF",
            "#FF007F"
    };

    static {
        new RepeatingThread("Color randomizer - Koi", TimeUnit.HOURS.toMillis(2), () -> {
            startingPoint = ThreadLocalRandom.current().nextInt(COLORS.length);
        }).start();
    }

    private Set<UserRoles> roles = new HashSet<>();
    private Set<String> badges = new HashSet<>();
    private final UserPlatform platform;
    private String color;
    private String username;
    private String displayname;
    private String UUID;
    private String bio = "";

    @SerializedName("image_link")
    private String imageLink;

    @SerializedName("followers_count")
    private long followersCount = -1;

    @SerializedName("subscriber_count")
    private long subCount = -1;

    public SimpleProfile getSimpleProfile() {
        return new SimpleProfile(this.UUID, this.platform);
    }

    public void calculateColorFromUsername() {
        if (this.color == null) {
            int hashValue = this.username.hashCode();
            int index = startingPoint + hashValue;

            while (index < 0) {
                index += COLORS.length - 1;
            }

            while (index >= COLORS.length) {
                index -= COLORS.length;
            }

            try {
                this.color = COLORS[index];
            } catch (ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
                System.err.printf("Requested Index: %d, HashValue: %d\n", index, hashValue);
                this.color = COLORS[ThreadLocalRandom.current().nextInt(COLORS.length)];
            }
        }
    }

    public static enum UserRoles {
        BROADCASTER,
        SUBSCRIBER,
        FOLLOWER,
        MODERATOR,
        STAFF;

    }

    public void calculateColorFromBio() {
        if (this.bio != null) {
            Matcher m = COLOR_PATTERN.matcher(this.bio.toLowerCase());
            while (m.find()) {
                String group = m.group();
                String str = group.substring(group.indexOf(':') + 1, group.length() - 1);

                try {
                    Color color = Color.parseCSSColor(str);

                    this.color = color.toHexString();

                    return;
                } catch (Exception ignored) {}
            }
        }

        this.calculateColorFromUsername();
    }

}
