package co.casterlabs.koi.user;

import co.casterlabs.koi.user.caffeine.CaffeineProvider;
import co.casterlabs.koi.user.twitch.TwitchProvider;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@AllArgsConstructor
public enum UserPlatform {
    CAFFEINE("https://caffeine.tv/%s", new CaffeineProvider()),
    TWITCH("https://twitch.tv/%s", new TwitchProvider());

    private @NonNull String platformLink;
    private @Getter @NonNull UserProvider provider;

    public String getLinkForUser(String username) {
        return String.format(this.platformLink, username);
    }

}
