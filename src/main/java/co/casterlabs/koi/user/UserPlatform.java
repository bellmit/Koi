package co.casterlabs.koi.user;

import co.casterlabs.koi.user.caffeine.CaffeineProvider;
import co.casterlabs.koi.user.caffeine.CaffeineUserConverter;
import co.casterlabs.koi.user.twitch.TwitchProvider;
import co.casterlabs.koi.user.twitch.TwitchUserConverter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@AllArgsConstructor
public enum UserPlatform {
    CAFFEINE("https://caffeine.tv/%s", new CaffeineProvider(), CaffeineUserConverter.getInstance()),
    TWITCH("https://twitch.tv/%s", new TwitchProvider(), TwitchUserConverter.getInstance());

    private @NonNull String platformLink;
    private @Getter @NonNull UserProvider provider;
    private @Getter @NonNull UserConverter<?> converter;

    public String getLinkForUser(String username) {
        return String.format(this.platformLink, username);
    }

}
