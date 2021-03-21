package co.casterlabs.koi.user;

import co.casterlabs.koi.user.brime.BrimeProvider;
import co.casterlabs.koi.user.brime.BrimeUserConverter;
import co.casterlabs.koi.user.caffeine.CaffeineProvider;
import co.casterlabs.koi.user.caffeine.CaffeineUserConverter;
import co.casterlabs.koi.user.glimesh.GlimeshProvider;
import co.casterlabs.koi.user.glimesh.GlimeshUserConverter;
import co.casterlabs.koi.user.trovo.TrovoProvider;
import co.casterlabs.koi.user.trovo.TrovoUserConverter;
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

    TROVO(
        "https://trovo.live/%s", 
        new TrovoProvider(), 
        TrovoUserConverter.getInstance()
    ), 

    GLIMESH(
        "https://glimesh.tv/%s", 
        new GlimeshProvider(), 
        GlimeshUserConverter.getInstance()
    ), 

    BRIME(
        "https://beta.brimelive.com/%s", 
        new BrimeProvider(), 
        BrimeUserConverter.getInstance()
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
