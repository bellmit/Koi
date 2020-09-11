package co.casterlabs.koi.user.twitch;

import com.gikk.twirk.types.users.TwitchUser;

import co.casterlabs.koi.user.IdentifierException;
import co.casterlabs.koi.user.SerializedUser;
import co.casterlabs.koi.user.UserConverter;
import co.casterlabs.koi.user.UserPlatform;
import lombok.Getter;

public class TwitchUserConverter implements UserConverter<com.gikk.twirk.types.users.TwitchUser> {
    private static @Getter UserConverter<com.gikk.twirk.types.users.TwitchUser> instance = new TwitchUserConverter();

    @Override
    public SerializedUser transform(TwitchUser user) {
        SerializedUser result = new SerializedUser(UserPlatform.TWITCH);

        result.setDisplayname(user.getDisplayName());
        result.setUUID(String.valueOf(user.getUserID()));
        result.setUsername(user.getDisplayName());
        result.setColor("#" + Integer.toHexString(user.getColor()).toUpperCase());
        // TODO image link, follower information

        return result;
    }

    @Override
    public SerializedUser get(String UUID) throws IdentifierException {
        // TODO just pass the id from transform and poll https://dev.twitch.tv/docs/v5/reference/channels/#get-channel-followers and
        // https://dev.twitch.tv/docs/v5/reference/channels/#get-channel-by-id OR https://dev.twitch.tv/docs/v5/reference/channels/#get-channel
        throw new IdentifierException();
    }

}
