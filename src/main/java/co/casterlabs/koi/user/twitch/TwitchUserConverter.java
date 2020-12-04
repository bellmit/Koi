package co.casterlabs.koi.user.twitch;

import java.util.List;

import co.casterlabs.apiutil.web.ApiException;
import co.casterlabs.koi.Koi;
import co.casterlabs.koi.user.IdentifierException;
import co.casterlabs.koi.user.PolyFillRequirements;
import co.casterlabs.koi.user.SerializedUser;
import co.casterlabs.koi.user.UserConverter;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.koi.user.UserPolyFill;
import co.casterlabs.twitchapi.helix.HelixGetUsersRequest;
import co.casterlabs.twitchapi.helix.HelixGetUsersRequest.HelixUser;
import lombok.Getter;

public class TwitchUserConverter implements UserConverter<com.gikk.twirk.types.users.TwitchUser> {
    private static @Getter TwitchUserConverter instance = new TwitchUserConverter();

    @Override
    public SerializedUser transform(com.gikk.twirk.types.users.TwitchUser user) {
        SerializedUser result = new SerializedUser(UserPlatform.TWITCH);
        UserPolyFill preferences = UserPolyFill.get(UserPlatform.TWITCH, String.valueOf(user.getUserID()));

        result.getBadges().addAll(preferences.getForcedBadges());

        result.setUUID(String.valueOf(user.getUserID()));
        result.setUsername(user.getDisplayName());
        result.setColor("#" + Integer.toHexString(user.getColor()).toUpperCase());
        result.setImageLink(preferences.get(PolyFillRequirements.PROFILE_PICTURE));

        preferences.set(PolyFillRequirements.COLOR, result.getColor()); // Set color for when the user is streaming, as this information is only present in Chat.

        return result;
    }

    @Override
    public SerializedUser get(String UUID) throws IdentifierException {
        HelixGetUsersRequest request = new HelixGetUsersRequest((TwitchAuth) Koi.getInstance().getAuthProvider(UserPlatform.TWITCH));

        request.addId(UUID);

        try {
            List<HelixUser> users = request.send();

            if (!users.isEmpty()) {
                return convert(users.get(0));
            } else {
                throw new IdentifierException();
            }
        } catch (ApiException e) {
            e.printStackTrace();
            throw new IdentifierException();
        }
    }

    public SerializedUser getByLogin(String login) throws IdentifierException {
        HelixGetUsersRequest request = new HelixGetUsersRequest((TwitchAuth) Koi.getInstance().getAuthProvider(UserPlatform.TWITCH));

        request.addLogin(login.toLowerCase());

        try {
            List<HelixUser> users = request.send();

            if (!users.isEmpty()) {
                return convert(users.get(0));
            } else {
                throw new IdentifierException();
            }
        } catch (ApiException e) {
            e.printStackTrace();
            throw new IdentifierException();
        }
    }

    public static SerializedUser convert(HelixUser helix) {
        SerializedUser result = new SerializedUser(UserPlatform.TWITCH);

        result.setUsername(helix.getDisplayName()); // Intentional.
        result.setUUID(helix.getId());
        result.setImageLink(helix.getProfileImageUrl());

        return result;
    }

}
