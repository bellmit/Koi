package co.casterlabs.koi.client.emotes;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import co.casterlabs.koi.events.ChatEvent;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserPlatform;
import lombok.Getter;
import lombok.NonNull;
import xyz.e3ndr.watercache.WaterCache;
import xyz.e3ndr.watercache.cachable.Cachable;
import xyz.e3ndr.watercache.cachable.DisposeReason;

public abstract class ExternalEmoteProvider {
    private static Map<UserPlatform, Map<String, ExternalEmoteProvider>> providers = new HashMap<>();

    private WaterCache channelEmoteCache = new WaterCache();
    private long refreshInterval;

    private @Getter UserPlatform platform;
    private @Getter String id;

    static {
        for (UserPlatform platform : UserPlatform.values()) {
            providers.put(platform, new HashMap<>());
        }
    }

    public ExternalEmoteProvider(@NonNull UserPlatform platform, @NonNull String id, long refreshInterval) {
        this.platform = platform;
        this.id = id;
        this.refreshInterval = refreshInterval;

        this.channelEmoteCache.start(TimeUnit.MINUTES, 1);
    }

    private final Map<String, ExternalEmote> getEmotesInChat(ChatEvent event, User streamer) {
        try {
            List<ExternalEmote> channelEmotes;
            List<ExternalEmote> globalEmotes;

            // Channel Emotes
            {
                String cacheId = streamer.getChannelId();
                CachedChannelEmotes cached = (CachedChannelEmotes) this.channelEmoteCache.getItemById(cacheId);

                if (cached == null) {
                    channelEmotes = this.getChannelEmotes(streamer);

                    this.channelEmoteCache.registerItem(cacheId, new CachedChannelEmotes(channelEmotes));
                } else if (cached.isStale()) {
                    channelEmotes = this.getChannelEmotes(streamer);

                    cached.wake();
                    cached.update(channelEmotes);
                } else {
                    cached.wake();
                    channelEmotes = cached.emotes;
                }
            }

            // Glboal Emotes
            {
                String cacheId = "__globalEmotes";
                CachedChannelEmotes cached = (CachedChannelEmotes) this.channelEmoteCache.getItemById(cacheId);

                if (cached == null) {
                    globalEmotes = this.getGlobalEmotes();

                    this.channelEmoteCache.registerItem(cacheId, new CachedChannelEmotes(globalEmotes));
                } else if (cached.isStale()) {
                    globalEmotes = this.getGlobalEmotes();

                    cached.wake();
                    cached.update(globalEmotes);
                } else {
                    cached.wake();
                    globalEmotes = cached.emotes;
                }
            }

            Map<String, ExternalEmote> detectedEmotes = this.detectEmotes(event, channelEmotes, globalEmotes);

            return detectedEmotes;
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyMap();
        }
    }

    protected abstract Map<String, ExternalEmote> detectEmotes(ChatEvent event, List<ExternalEmote> channelEmotes, List<ExternalEmote> globalEmotes);

    protected abstract List<ExternalEmote> getGlobalEmotes();

    protected abstract List<ExternalEmote> getChannelEmotes(User streamer);

    public static Map<String, Map<String, ExternalEmote>> getEmotesInChat(ChatEvent event) {
        User streamer = event.getStreamer();

        Map<String, ExternalEmoteProvider> externalProviders = providers.get(streamer.getPlatform());

        if (!externalProviders.isEmpty()) {
            Map<String, Map<String, ExternalEmote>> result = new HashMap<>();

            for (Entry<String, ExternalEmoteProvider> external : externalProviders.entrySet()) {
                Map<String, ExternalEmote> detected = external.getValue().getEmotesInChat(event, streamer);

                result.put(external.getKey(), detected);
            }

            return result;
        } else {
            return Collections.emptyMap();
        }
    }

    private class CachedChannelEmotes extends Cachable {
        private List<ExternalEmote> emotes;
        private long lastWake;
        private long lastUpdate;

        private CachedChannelEmotes(List<ExternalEmote> emotes) {
            super(TimeUnit.HOURS, 1);

            this.update(emotes);
        }

        public void wake() {
            this.lastWake = System.currentTimeMillis();
        }

        public void update(List<ExternalEmote> emotes) {
            this.emotes = emotes;
            this.lastUpdate = System.currentTimeMillis();
        }

        public boolean isStale() {
            // Update the cache every x minutes.
            return (System.currentTimeMillis() - this.lastUpdate) > refreshInterval;
        }

        @Override
        public boolean onDispose(DisposeReason reason) {
            if ((System.currentTimeMillis() - this.lastWake) > TimeUnit.MINUTES.toMillis(15)) {
                this.life += TimeUnit.HOURS.toMillis(1);

                return false;
            } else {
                return true;
            }
        }

    }

}
