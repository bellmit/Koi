package co.casterlabs.koi.user;

import co.casterlabs.koi.user.caffeine.CaffeineProvider;
import co.casterlabs.koi.user.caffeine.CaffeineUserConverter;
import co.casterlabs.koi.user.twitch.TwitchProvider;
import co.casterlabs.koi.user.twitch.TwitchUserConverter;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;

@Getter
@NonNull
public enum UserPlatform {
    //@formatter:off

    CAFFEINE(
        "https://caffeine.tv/%s", 
        new CaffeineProvider(), 
        CaffeineUserConverter.getInstance()
    ),

    TWITCH(
        "https://twitch.tv/%s", 
        new TwitchProvider(), 
        TwitchUserConverter.getInstance()
    ), 

    CASTERLABS_SYSTEM(null, null, null);

    //@formatter:on

    private @Getter(AccessLevel.NONE) String platformLink;
    private UserConverter<?> converter;
    private UserProvider provider;

    private UserPlatform(String platformLink, UserProvider provider, UserConverter<?> converter) {
        this.platformLink = platformLink;
        this.converter = converter;
        this.provider = provider;
    }

    public boolean isEnabled() {
        return this.provider != null;
    }

    public String getLinkForUser(String username) {
        if (this.isEnabled()) {
            return String.format(this.platformLink, username);
        } else {
            return "https://casterlabs.co";
        }
    }

}
