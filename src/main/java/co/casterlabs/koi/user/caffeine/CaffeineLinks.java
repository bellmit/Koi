package co.casterlabs.koi.user.caffeine;

import java.net.URI;

import lombok.SneakyThrows;

public class CaffeineLinks {

    // DATA
    public static String getFollowersLink(String caid) {
        return "https://api.caffeine.tv/v2/users/" + caid + "/followers?limit=20";
    }

    public static String getUsersLink(String caid) {
        return "https://api.caffeine.tv/v1/users/" + caid;
    }

    // REALTIME
    @SneakyThrows
    public static URI getMessagesLink(String stageId) {
        return new URI("wss://realtime.caffeine.tv/v2/reaper/stages/" + stageId + "/messages");
    }

    @SneakyThrows
    public static URI getQueryLink() {
        return new URI("wss://realtime.caffeine.tv/public/graphql/query");
    }

    // ASSETS
    public static String getAvatarLink(String path) {
        return "https://images.caffeine.tv" + path;
    }

    public static String getImageLink(String path) {
        return "https://assets.caffeine.tv" + path;
    }

    // AUTH
    public static String getAnonymousCredentialLink() {
        return "https://api.caffeine.tv/v1/credentials/anonymous";
    }

    public static String getTokenLink() {
        return "https://api.caffeine.tv/v1/account/token";
    }

    public static String getTokenSigningLink(String caid) {
        return "https://api.caffeine.tv/v1/users/" + caid + "/signed";
    }

}
