package co.casterlabs.koi.integration.glimesh.data;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.apiutil.web.ApiException;
import co.casterlabs.glimeshapijava.requests.GlimeshGetChannelRequest;
import co.casterlabs.glimeshapijava.requests.GlimeshGetUserRequest;
import co.casterlabs.glimeshapijava.types.GlimeshChannel;
import co.casterlabs.glimeshapijava.types.GlimeshUser;
import co.casterlabs.koi.integration.glimesh.GlimeshIntegration;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserConverter;
import co.casterlabs.koi.user.UserPlatform;
import lombok.Getter;
import lombok.NonNull;

public class GlimeshUserConverter implements UserConverter<GlimeshUser> {
    private static final @Getter GlimeshUserConverter instance = new GlimeshUserConverter();

    @Override
    public @NonNull User transform(@NonNull GlimeshUser glimesh) {
        User user = new User(UserPlatform.GLIMESH);

        user.setId(glimesh.getId());

        user.setUsername(glimesh.getUsername());
        user.setDisplayname(glimesh.getDisplayname());
        user.setImageLink(glimesh.getAvatarUrl());
        user.setBio(glimesh.getProfileContentMd());

        // :)
        user.calculateColorFromBio();

        return user;
    }

    @Override
    public User get(@NonNull String username) {
        GlimeshGetUserRequest request = new GlimeshGetUserRequest(GlimeshIntegration.getInstance().getAppAuth())
            .queryByUsername(username);

        try {
            return this.transform(request.send());
        } catch (ApiException e) {
            return null;
        }
    }

    public GlimeshChannel getChannelByUsername(String username) throws ApiAuthException, ApiException {
        GlimeshGetChannelRequest request = new GlimeshGetChannelRequest(GlimeshIntegration.getInstance().getAppAuth())
            .queryByUsername(username);

        return request.send();
    }

    public @Nullable GlimeshChannel getChannelByUserId(String id) {
        try {
            GlimeshUser user = new GlimeshGetUserRequest(GlimeshIntegration.getInstance().getAppAuth())
                .queryByUserId(id)
                .send();

            return getChannelByUsername(user.getUsername());
        } catch (ApiException e) {
            return null;
        }
    }

}
