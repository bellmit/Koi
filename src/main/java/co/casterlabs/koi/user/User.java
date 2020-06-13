package co.casterlabs.koi.user;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import co.casterlabs.koi.Koi;
import co.casterlabs.koi.events.DonationEvent;
import co.casterlabs.koi.events.Event;
import co.casterlabs.koi.events.EventListener;
import co.casterlabs.koi.events.EventType;
import co.casterlabs.koi.events.FollowEvent;
import co.casterlabs.koi.events.InfoEvent;
import co.casterlabs.koi.events.UserUpdateEvent;
import co.casterlabs.koi.util.DebugStat;
import co.casterlabs.koi.util.FileUtil;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public abstract class User {
    private static final long UPDATE_AGE = TimeUnit.MINUTES.toMillis(1);
    private static @Getter DebugStat eventStat = new DebugStat("UserEvents");

    private UserPlatform platform;
    private String username;
    private long lastWake;

    protected Map<EventType, Event> dataEvents = new ConcurrentHashMap<>();
    protected String displayname;
    protected String UUID;
    protected long followerCount;
    protected long followingCount;

    // ToString Excludes
    protected @ToString.Exclude Set<EventListener> eventListeners = Collections.synchronizedSet(new HashSet<>());
    protected @ToString.Exclude Set<String> followers = Collections.synchronizedSet(new HashSet<>());
    protected @ToString.Exclude String imageLink = "https://via.placeholder.com/100";

    public User(UserPlatform platform) {
        this.platform = platform;

        this.wake();
    }

    protected void load() {
        File file = new File(this.platform.getUserDir(), this.UUID);

        if (file.exists()) {
            JsonObject json = FileUtil.readJsonOrDefault(file, new JsonObject());
            JsonObject infoEvents = json.getAsJsonObject("info_events");

            for (String type : infoEvents.keySet()) {
                JsonObject event = infoEvents.getAsJsonObject(type);

                this.dataEvents.put(EventType.valueOf(type), InfoEvent.fromJson(event, this));
            }

            if (json.has("followers")) {
                JsonArray followers = json.getAsJsonArray("followers");

                for (JsonElement follower : followers) {
                    this.followers.add(follower.getAsString());
                }
            }
        }
    }

    public void close() {
        if ((this.followers.size() != 0) || (this.dataEvents.size() != 0)) {
            File file = new File(Koi.getInstance().getDir(), "/users/" + this.platform + "/" + this.UUID);
            JsonObject json = new JsonObject();
            JsonObject infoEvents = new JsonObject();
            JsonArray followers = new JsonArray();

            this.followers.forEach((follower) -> followers.add(follower));

            for (Map.Entry<EventType, Event> event : this.dataEvents.entrySet()) {
                if (event.getKey().isData()) {
                    infoEvents.add(event.getKey().name(), event.getValue().serialize());
                }
            }

            json.add("followers", followers);
            json.add("info_events", infoEvents);

            FileUtil.writeJson(file, json);

            this.close0();
        }
    }

    protected void setUsername(String newUsername) {
        if (this.username == null) {
            this.username = newUsername;
        } else if (!this.username.equals(newUsername)) {
            this.platform.getUserCache().remove(this.username);
            this.platform.getUserCache().put(newUsername, this);

            this.username = newUsername;
        }
    }

    public User wake() {
        this.lastWake = System.currentTimeMillis();

        return this;
    }

    public void broadcastEvent(Event e) {
        if (e.getType() == EventType.FOLLOW) {
            FollowEvent event = (FollowEvent) e;

            this.followers.add(event.getFollower().getUUID());
            InfoEvent recentFollow = new InfoEvent(event);

            this.dataEvents.put(EventType.FOLLOW, recentFollow);
            this.broadcastEvent(recentFollow);
        } else if (e.getType() == EventType.DONATION) {
            DonationEvent event = (DonationEvent) e;
            InfoEvent top = (InfoEvent) this.dataEvents.get(EventType.DONATION);

            if (event.getAmount() == 0) {
                // It's a dummy event.
            } else if ((top == null) || (top.getEvent().get("usd_equalivant").getAsDouble() < event.getUsdEquivalent())) {
                InfoEvent topDonation = new InfoEvent(event);

                this.dataEvents.put(EventType.DONATION, topDonation);
                this.broadcastEvent(topDonation);
            }
        } else if (e.getType() == EventType.STREAM_STATUS) {
            this.dataEvents.put(EventType.STREAM_STATUS, e);
        }

        for (EventListener listener : this.eventListeners) {
            listener.onEvent(e);
        }

        eventStat.tick();
    }

    public void update() {
        long updateAge = Koi.getInstance().isDebug() ? TimeUnit.SECONDS.toMillis(5) : UPDATE_AGE;
        
        if (updateAge < (System.currentTimeMillis() - this.lastWake)) {
            this.wake();
            
            Koi.getMiscThreadPool().submit(() -> {
                this.updateUser();
                
                this.broadcastEvent(new UserUpdateEvent(this));
            });
        }

        this.update0();
    }

    public boolean hasListeners() {
        return this.eventListeners.size() != 0;
    }

    public void tryExternalHook() {}

    protected abstract void update0();

    protected abstract void updateUser();

    protected abstract void close0();

}
