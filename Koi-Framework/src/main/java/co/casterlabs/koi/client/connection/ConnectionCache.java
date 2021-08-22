package co.casterlabs.koi.client.connection;

import java.util.concurrent.TimeUnit;

import co.casterlabs.koi.client.ClientAuthProvider;
import lombok.NonNull;
import xyz.e3ndr.watercache.WaterCache;

public abstract class ConnectionCache {
    private WaterCache cache = new WaterCache();

    public ConnectionCache(@NonNull TimeUnit unit, long interval) {
        this.cache.start(unit, interval);
    }

    public ConnectionHolder get(@NonNull String key, @NonNull ClientAuthProvider auth) {
        ConnectionHolder holder = (ConnectionHolder) this.cache.getItemById(key);

        if (holder == null) {
            holder = new ConnectionHolder(key, auth.getSimpleProfile());

            holder.setConn(this.createConn(holder, key, auth));

            this.cache.registerItem(key, holder);
        }

        return holder;
    }

    public abstract Connection createConn(ConnectionHolder holder, String key, @NonNull ClientAuthProvider auth);

    public void shutdown() {
        this.cache.stop();
    }

}
