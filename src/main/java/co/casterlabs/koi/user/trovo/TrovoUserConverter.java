package co.casterlabs.koi.user.trovo;

import co.casterlabs.apiutil.web.ApiException;
import co.casterlabs.koi.Koi;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserConverter;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.trovoapi.requests.TrovoGetUsersRequest;
import co.casterlabs.trovoapi.requests.data.TrovoUser;
import lombok.Getter;
import lombok.NonNull;

public class TrovoUserConverter implements UserConverter<TrovoUser> {
    private static final @Getter TrovoUserConverter instance = new TrovoUserConverter();

    @Override
    public @NonNull User transform(@NonNull TrovoUser trovo) {
        User result = new User(UserPlatform.TROVO);

        result.setDisplayname(trovo.getNickname());
        result.setUsername(trovo.getUsername());
        result.setUUID(trovo.getUserId());

        result.calculateColorFromUsername();

        return result;
    }

    @Override
    public User get(@NonNull String username) {
        TrovoApplicationAuth trovoAuth = (TrovoApplicationAuth) Koi.getInstance().getAuthProvider(UserPlatform.TROVO);
        TrovoGetUsersRequest request = new TrovoGetUsersRequest(trovoAuth, username);

        try {
            return this.transform(request.send().get(0));
        } catch (ApiException e) {
            return null;
        }
    }

}
