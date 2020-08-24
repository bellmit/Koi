package co.casterlabs.koi.user;

import com.google.gson.annotations.SerializedName;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class SerializedUser {
    private final UserPlatform platform;
    private String UUID;
    private String username;
    private String displayname;
    @SerializedName("image_link")
    private String imageLink;
    @SerializedName("follower_count")
    private long followerCount;
    @SerializedName("following_count")
    private long followingCount;
    private String color;

}
