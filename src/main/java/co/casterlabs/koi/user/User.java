package co.casterlabs.koi.user;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class User {
    private final UserPlatform platform;

    @SerializedName("image_link")
    private String imageLink = "data:image/gif;base64,R0lGODlhAQABAIAAAP///wAAACH5BAEAAAAALAAAAAABAAEAAAICRAEAOw==";

    @SerializedName("followers_count")
    private long followersCount = -1;

    private List<String> badges = new ArrayList<>();
    private String color = "#ea4c4c";
    private String username = "?";
    private String UUID = "?";

}
