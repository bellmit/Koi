package co.casterlabs.koi.user.twitch;

import java.util.List;

import com.gikk.twirk.types.users.TwitchUser;

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
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;

public class TwitchUserConverter implements UserConverter<com.gikk.twirk.types.users.TwitchUser> {
    private static @Getter UserConverter<com.gikk.twirk.types.users.TwitchUser> instance = new TwitchUserConverter();

    @Override
    public SerializedUser transform(TwitchUser user) {
        SerializedUser result = new SerializedUser(UserPlatform.TWITCH);

        UserPolyFill preferences = UserPolyFill.get(UserPlatform.TWITCH, String.valueOf(user.getUserID()));

        result.setDisplayname(user.getDisplayName());
        result.setUUID(String.valueOf(user.getUserID()));
        result.setUsername(user.getDisplayName());
        result.setColor("#" + Integer.toHexString(user.getColor()).toUpperCase());
        result.setImageLink(preferences.get(PolyFillRequirements.PROFILE_PICTURE)); // TODO better way for image link

        preferences.set(PolyFillRequirements.COLOR, result.getColor()); // Set color for when the user is streaming, as this information is only present in Chat.

        return result;
    }

    @Override
    public SerializedUser get(String UUID) throws IdentifierException {
        HelixGetUsersRequest request = new HelixGetUsersRequest((TwitchAuth) Koi.getInstance().getAuthProvider(UserPlatform.TWITCH));

        // request.addId(UUID); // TODO a check, as somebody could have a username that also matches an id
        request.addLogin(UUID);

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

        result.setDisplayname(helix.getDisplayName());
        result.setUsername(helix.getDisplayName()); // Intentional.
        result.setUUID(helix.getId());
        result.setImageLink(helix.getProfileImageUrl());

        return result;
    }

    @Data
    @Accessors(chain = true)
    public static class UserImageHolder {
        private String image;
        private long lastUpdated;

        public void check() {

        }

    }

}
