package co.casterlabs.koi.integration.caffeine.connections;

import java.util.concurrent.TimeUnit;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.caffeineapi.requests.CaffeineUserInfoRequest;
import co.casterlabs.caffeineapi.types.CaffeineUser;
import co.casterlabs.koi.client.Client;
import co.casterlabs.koi.client.SimpleProfile;
import co.casterlabs.koi.client.connection.ConnectionHolder;
import co.casterlabs.koi.integration.caffeine.data.CaffeineUserConverter;
import co.casterlabs.koi.integration.caffeine.impl.CaffeineIntegrationAuth;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.util.RepeatingThread;

public class CaffeineProfileUpdater {

    public static ConnectionHolder get(Client client, CaffeineIntegrationAuth caffeineAuth, SimpleProfile profile) {
        ConnectionHolder holder = new ConnectionHolder(profile.getChannelId(), profile);

        holder.setConn(new RepeatingThread("Caffeine Profile Updater - " + profile.getChannelId(), TimeUnit.MINUTES.toMillis(2), () -> {
            try {
                CaffeineUserInfoRequest request = new CaffeineUserInfoRequest().setCAID(profile.getChannelId());
                CaffeineUser updatedProfile = request.send();

                User result = CaffeineUserConverter.getInstance().transform(updatedProfile);

                result.setFollowersCount(updatedProfile.getFollowersCount());

                holder.updateProfile(result);
            } catch (ApiAuthException e) {
                client.notifyCredentialExpired();
            } catch (Exception ignored) {}
        }));

        return holder;
    }

}
