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
        CaffeineUserConverter.getInstance(), 
        
        PlatformFeatures.CHAT_UPVOTES
    ),
    
    TWITCH(
        "https://twitch.tv/%s", 
        new TwitchProvider(), 
        TwitchUserConverter.getInstance()
    );
    
    //@formatter:on

    private @Getter(AccessLevel.NONE) String platformLink;
    private PlatformFeatures[] features;
    private UserConverter<?> converter;
    private UserProvider provider;

    private UserPlatform(String platformLink, UserProvider provider, UserConverter<?> converter, PlatformFeatures... features) {
        this.platformLink = platformLink;
        this.converter = converter;
        this.features = features;
        this.provider = provider;
    }

    public String getLinkForUser(String username) {
        return String.format(this.platformLink, username);
    }

    public static enum PlatformFeatures {
        CHAT_UPVOTES,

    }

}
