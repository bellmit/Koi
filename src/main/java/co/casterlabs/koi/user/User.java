package co.casterlabs.koi.user;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import com.google.gson.annotations.SerializedName;

import co.casterlabs.koi.RepeatingThread;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class User {
    private static MessageDigest digest;
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
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        new RepeatingThread("Color randomizer - Koi", TimeUnit.HOURS.toMillis(1), () -> {
            startingPoint = ThreadLocalRandom.current().nextInt(COLORS.length);
        }).start();
    }

    private final UserPlatform platform;
    private List<String> badges = new ArrayList<>();
    private String color;
    private String username;
    private String UUID;

    @SerializedName("image_link")
    private String imageLink;

    @SerializedName("followers_count")
    private long followersCount = -1;

    @SerializedName("subscriber_count")
    private long subCount = -1;

    public void calculateColorFromUsername() {
        if (this.color == null) {
            byte[] encodedhash = digest.digest(this.username.getBytes(StandardCharsets.UTF_8));
            int pointer = startingPoint;

            for (byte b : encodedhash) {
                pointer += b;

                while (pointer < 0) {
                    pointer += COLORS.length - 1;
                }

                while (pointer >= COLORS.length) {
                    pointer -= COLORS.length - 1;
                }
            }

            this.color = COLORS[pointer];
        }
    }

}
