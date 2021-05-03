package co.casterlabs.koi.integration.caffeine.user;

import java.util.concurrent.TimeUnit;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.caffeineapi.requests.CaffeineUser;
import co.casterlabs.caffeineapi.requests.CaffeineUserInfoRequest;
import co.casterlabs.koi.client.Client;
import co.casterlabs.koi.client.ConnectionHolder;
import co.casterlabs.koi.client.SimpleProfile;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.util.RepeatingThread;

public class CaffeineProfileUpdater {

    public static ConnectionHolder get(Client client, CaffeineAuth caffeineAuth) {
        SimpleProfile profile = client.getSimpleProfile();

        ConnectionHolder holder = new ConnectionHolder(profile.getChannelId() + ":profile", profile);

        co.casterlabs.koi.util.RepeatingThread thread = new RepeatingThread("Caffeine Profile Updater - " + profile.getChannelId(), TimeUnit.MINUTES.toMillis(2), () -> {
            try {
                CaffeineUserInfoRequest request = new CaffeineUserInfoRequest().setCAID(profile.getChannelId());
                CaffeineUser updatedProfile = request.send();

                User result = CaffeineUserConverter.getInstance().transform(updatedProfile);

                result.setFollowersCount(updatedProfile.getFollowersCount());

                holder.updateProfile(result);
            } catch (ApiAuthException e) {
                client.notifyCredentialExpired();
            } catch (Exception ignored) {}
        });

        thread.start();

        holder.setCloseable(thread);

        return holder;
    }

}
