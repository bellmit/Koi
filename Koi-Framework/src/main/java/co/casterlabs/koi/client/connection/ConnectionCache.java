package co.casterlabs.koi.client.connection;

import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.koi.client.ClientAuthProvider;
import co.casterlabs.koi.client.SimpleProfile;
import lombok.NonNull;
import xyz.e3ndr.watercache.WaterCache;

@SuppressWarnings("deprecation")
public abstract class ConnectionCache {
    private WaterCache cache = new WaterCache();

    public ConnectionCache(@NonNull TimeUnit unit, long interval) {
        this.cache.start(unit, interval);
    }

    public ConnectionHolder get(@NonNull String key, @Nullable ClientAuthProvider auth, @NonNull SimpleProfile simpleProfile) {
        ConnectionHolder holder = (ConnectionHolder) this.cache.getItemById(key);

        if (holder == null) {
            holder = new ConnectionHolder(key, simpleProfile);

            holder.setConn(this.createConn(holder, key, auth));

            this.cache.registerItem(key, holder);
        }

        return holder;
    }

    /**
     * @deprecated In the future, hooking a stream should NOT require authentication
     *             wherever possible (and should lookup a fresh token rather than
     *             use the one from the client)
     */
    @Deprecated
    public abstract Connection createConn(@NonNull ConnectionHolder holder, @NonNull String key, @Nullable ClientAuthProvider auth);

    public void shutdown() {
        this.cache.stop();
    }

}
