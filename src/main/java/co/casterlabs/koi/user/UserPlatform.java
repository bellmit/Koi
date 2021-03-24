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
        CaffeineUserConverter.getInstance(),
        "#000ff",
        "Caffeine-System",
        "https://assets.casterlabs.co/caffeine/logo.png"
    ),

    TWITCH(
        "https://twitch.tv/%s", 
        new TwitchProvider(), 
        TwitchUserConverter.getInstance(),
        "#7d2bf9",
        "Twitch-System",
        "https://assets.casterlabs.co/twitch/logo.png"
    ), 

    TROVO(
        "https://trovo.live/%s", 
        new TrovoProvider(), 
        TrovoUserConverter.getInstance(),
        "#088942",
        "Trovo-System",
        "https://assets.casterlabs.co/trovo/logo.png"
    ), 

    GLIMESH(
        "https://glimesh.tv/%s", 
        new GlimeshProvider(), 
        GlimeshUserConverter.getInstance(),
        "#0e1726",
        "Glimesh-System",
        "https://assets.casterlabs.co/glimesh/logo.png"
    ), 

    BRIME(
        "https://beta.brimelive.com/%s", 
        new BrimeProvider(), 
        BrimeUserConverter.getInstance(),
        "#fc3537",
        "Brime-System",
        "https://assets.casterlabs.co/brime/logo.png"
    ), 

    CASTERLABS_SYSTEM(null, null, null, "#ea4c4c", "Casterlabs-System", "https://assets.casterlabs.co/logo/casterlabs_icon.png");

    //@formatter:on

    private @Getter(AccessLevel.NONE) String platformLink;
    private UserConverter<?> converter;
    private UserProvider provider;
    private String username;
    private String color;
    private String image;

    private UserPlatform(String platformLink, UserProvider provider, UserConverter<?> converter, String color, String username, String image) {
        this.platformLink = platformLink;
        this.converter = converter;
        this.provider = provider;
        this.username = username;
        this.color = color;
        this.image = image;
    }

    public User getPlatformUser() {
        User user = new User(CASTERLABS_SYSTEM);

        user.setDisplayname(this.username);
        user.setUsername(this.username.toLowerCase());
        user.setUUID("CASTERLABS_SYSTEM");
        user.setColor(this.color);
        user.setImageLink(this.image);

        return user;
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
