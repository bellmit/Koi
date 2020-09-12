package co.casterlabs.koi.user.twitch;

import co.casterlabs.koi.Koi;
import co.casterlabs.koi.user.UserPlatform;

public class TwitchLinks {
    private static TwitchAuth auth = (TwitchAuth) Koi.getInstance().getAuthProvider(UserPlatform.TWITCH);

    public static String getUserByLoginLink(String username) {
        return "https://api.twitch.tv/v5/users/?login=" + username + "&client_id=" + auth.getClientId();
    }

    public static String getUserByIdLink(String id) {
        return "https://api.twitch.tv/v5/users/" + id + "?client_id=" + auth.getClientId();
    }

    public static String getUserFollowers(String id) {
        return "https://api.twitch.tv/v5/channels/" + id + "/follows";
    }

    public static String getStreamInfo(String id) {
        return "https://api.twitch.tv/v5/streams/" + id;
    }

}
