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
import co.casterlabs.koi.networking.JsonSerializer;
import co.casterlabs.koi.util.DebugStat;
import co.casterlabs.koi.util.FileUtil;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public abstract class User implements JsonSerializer {
    private static DebugStat eventStat = new DebugStat("UserEvents");

    private UserPlatform platform;
    private String username;
    private long lastWake;

    protected Map<EventType, Event> dataEvents = new ConcurrentHashMap<>();
    protected String displayname;
    protected String UUID;

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
                JsonObject event = json.getAsJsonObject(type);

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
        } else if (e.getType() == EventType.DONATE) {
            DonationEvent event = (DonationEvent) e;
            InfoEvent top = (InfoEvent) this.dataEvents.get(EventType.DONATE);

            if ((top == null) || (top.getEvent().get("usd_equalivant").getAsDouble() < event.getUsdEqualivant())) {
                InfoEvent topDonation = new InfoEvent(event);

                this.dataEvents.put(EventType.DONATE, topDonation);
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

    @Override
    public JsonObject serialize() {
        JsonObject json = new JsonObject();

        json.addProperty("UUID", this.UUID);
        json.addProperty("displayname", this.displayname);
        json.addProperty("username", this.username);
        json.addProperty("image_link", this.imageLink);
        json.addProperty("platform", this.platform.name());

        return json;
    }

    public void update() {
        if (this.lastWake > (System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(10))) {
            Koi.getEventThreadPool().submit(() -> {
                this.updateUser();
                Koi.getInstance().getLogger().debug("Updated " + this.platform + ":" + this.username);
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
