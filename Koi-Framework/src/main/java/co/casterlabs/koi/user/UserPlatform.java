package co.casterlabs.koi.user;

import co.casterlabs.koi.Koi;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;

@Getter
@NonNull
public enum UserPlatform {
    //@formatter:off

    CAFFEINE(
        "https://caffeine.tv/%s", 
        "#000ff",
        "Caffeine-System",
        "https://assets.casterlabs.co/caffeine/logo.png"
    ),

    TWITCH(
        "https://twitch.tv/%s", 
        "#7d2bf9",
        "Twitch-System",
        "https://assets.casterlabs.co/twitch/logo.png"
    ), 

    TROVO(
        "https://trovo.live/%s", 
        "#088942",
        "Trovo-System",
        "https://assets.casterlabs.co/trovo/logo.png"
    ), 

    GLIMESH(
        "https://glimesh.tv/%s", 
        "#0e1726",
        "Glimesh-System",
        "https://assets.casterlabs.co/glimesh/logo.png"
    ), 

    BRIME(
        "https://beta.brimelive.com/%s", 
        "#fc3537",
        "Brime-System",
        "https://assets.casterlabs.co/brime/logo.png"
    ), 

    CASTERLABS_SYSTEM(null, "#ea4c4c", "Casterlabs-System", "https://assets.casterlabs.co/logo/casterlabs_icon.png");

    //@formatter:on

    private @Getter(AccessLevel.NONE) String platformLink;
    private String username;
    private String color;
    private String image;

    private UserPlatform(String platformLink, String color, String username, String image) {
        this.platformLink = platformLink;
        this.username = username;
        this.color = color;
        this.image = image;
    }

    public UserConverter<?> getConverter() {
        return Koi.getInstance().getUserConverter(this);
    }

    public UserProvider getProvider() {
        return Koi.getInstance().getUserProvider(this);
    }

    public User getPlatformUser() {
        User user = new User(CASTERLABS_SYSTEM);

        user.setDisplayname(this.username);
        user.setUsername(this.username.toLowerCase());
        user.setIdAndChannelId("CASTERLABS_SYSTEM");
        user.setColor(this.color);
        user.setImageLink(this.image);

        return user;
    }

    public boolean isEnabled() {
        try {
            this.getProvider();

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getLinkForUser(String username) {
        if (this.isEnabled()) {
            return String.format(this.platformLink, username);
        } else {
            return "https://casterlabs.co";
        }
    }

}
