package co.casterlabs.koi.user.twitch;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.apiutil.web.ApiException;
import co.casterlabs.koi.Koi;
import co.casterlabs.koi.user.IdentifierException;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserConverter;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.twitchapi.helix.HelixGetUsersRequest;
import co.casterlabs.twitchapi.helix.HelixGetUsersRequest.HelixUser;
import lombok.Getter;
import lombok.NonNull;

// TODO
public class TwitchUserConverter implements UserConverter<com.gikk.twirk.types.users.TwitchUser> {
    private static @Getter TwitchUserConverter instance = new TwitchUserConverter();

    @Override
    public @NonNull User transform(@NonNull com.gikk.twirk.types.users.TwitchUser user) {
        User result = new User(UserPlatform.TWITCH);
        // UserPolyFill preferences = UserPolyFill.get(UserPlatform.TWITCH,
        // String.valueOf(user.getUserID()));

        // result.getBadges().addAll(preferences.getForcedBadges());

        result.setUUID(String.valueOf(user.getUserID()));
        result.setUsername(user.getDisplayName());
        result.setColor("#" + Integer.toHexString(user.getColor()).toUpperCase());
        // result.setImageLink(preferences.get(PolyFillRequirements.PROFILE_PICTURE));

        // preferences.set(PolyFillRequirements.COLOR, result.getColor()); // Set color
        // for when the user is streaming, as this information is only present in Chat.

        return result;
    }

    public User getByLogin(String login) throws IdentifierException {
        HelixGetUsersRequest request = new HelixGetUsersRequest((TwitchCredentialsAuth) Koi.getInstance().getAuthProvider(UserPlatform.TWITCH));

        request.addLogin(login.toLowerCase());

        try {
            List<HelixUser> users = request.send();

            if (!users.isEmpty()) {
                return transform(users.get(0));
            } else {
                throw new IdentifierException();
            }
        } catch (ApiException e) {
            e.printStackTrace();
            throw new IdentifierException();
        }
    }

    public static User transform(HelixUser helix) {
        User result = new User(UserPlatform.TWITCH);

        result.setUsername(helix.getDisplayName()); // Intentional.
        result.setUUID(helix.getId());
        result.setImageLink(helix.getProfileImageUrl());

        return result;
    }

    @Override
    public @Nullable User get(@NonNull String username) {
        try {
            return this.getByLogin(username);
        } catch (IdentifierException e) {
            return null;
        }
    }

}
