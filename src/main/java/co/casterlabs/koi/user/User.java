package co.casterlabs.koi.user;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class User {
    private static MessageDigest digest;
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
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    private final UserPlatform platform;

    @SerializedName("image_link")
    private String imageLink = "data:image/gif;base64,R0lGODlhAQABAIAAAP///wAAACH5BAEAAAAALAAAAAABAAEAAAICRAEAOw==";

    @SerializedName("followers_count")
    private long followersCount = -1;

    private List<String> badges = new ArrayList<>();

    private String color;

    private String username = "?";

    private String UUID = "?";

    public void calculateColorFromUsername() {
        byte[] encodedhash = digest.digest(this.username.getBytes(StandardCharsets.UTF_8));
        int pointer = 0;

        for (byte b : encodedhash) {
            pointer += b;

            if (pointer < 0) {
                pointer = COLORS.length - 1;
            } else if (pointer >= COLORS.length) {
                pointer = 0;
            }
        }

        this.color = COLORS[pointer];
    }

}
