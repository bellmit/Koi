package co.casterlabs.koi.user;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import co.casterlabs.koi.Koi;
import co.casterlabs.koi.events.ChatEvent;
import co.casterlabs.koi.events.Event;
import co.casterlabs.koi.events.EventListener;
import co.casterlabs.koi.events.EventType;
import co.casterlabs.koi.events.FollowEvent;
import co.casterlabs.koi.events.InfoEvent;
import co.casterlabs.koi.events.UserUpdateEvent;
import co.casterlabs.koi.user.command.CommandsRegistry;
import co.casterlabs.koi.util.DebugStat;
import co.casterlabs.koi.util.FileUtil;
import lombok.Getter;
import lombok.ToString;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

@Getter
@ToString
public abstract class User {
    private static final long UPDATE_AGE = TimeUnit.MINUTES.toMillis(1);
    private static @Getter DebugStat eventStat = new DebugStat("UserEvents");

    private UserPlatform platform;
    private String username;
    private long lastWake;

    protected String displayname;
    protected String UUID;
    protected long followerCount;

    protected UserPolyFill preferences;

    // ToString Excludes
    protected @ToString.Exclude Map<EventType, Event> dataEvents = new ConcurrentHashMap<>();
    protected @ToString.Exclude Set<EventListener> eventListeners = Collections.synchronizedSet(new HashSet<>());
    protected @ToString.Exclude Set<String> followers = Collections.synchronizedSet(new HashSet<>());
    protected @ToString.Exclude String imageLink = "data:image/gif;base64,R0lGODlhAQABAIAAAP///wAAACH5BAEAAAAALAAAAAABAAEAAAICRAEAOw==";

    public User(UserPlatform platform) {
        this.platform = platform;

        this.wake();
    }

    protected void load() throws IdentifierException {
        if (this.username == null) {
            throw new IdentifierException(); // We don't know them
        }

        this.preferences = UserPolyFill.get(this.platform, this.UUID);

        Koi.getEventThreadPool().submit(() -> {
            try {
                File file = new File(this.platform.getUserDir(), this.UUID);

                if (file.exists()) {
                    JsonObject json = FileUtil.readJsonOrDefault(file, new JsonObject());

                    if (json.has("followers")) {
                        JsonArray followers = json.getAsJsonArray("followers");

                        for (JsonElement follower : followers) {
                            this.followers.add(follower.getAsString());
                        }
                    }

                    if (json.has("recent_follower")) {
                        try {
                            SerializedUser lastFollower = Koi.getInstance().getUserSerialized(json.get("recent_follower").getAsString(), this.platform);
                            InfoEvent recentFollow = new InfoEvent(new FollowEvent(lastFollower, this));

                            this.broadcastEvent(recentFollow);
                            this.dataEvents.put(EventType.FOLLOW, recentFollow);
                        } catch (IdentifierException e) {
                            // They don't exist anymore
                        }
                    }
                }

            } catch (Exception e) {
                FastLogger.logStatic(LogLevel.SEVERE, "Error while reading user file for %s.", this.username);
                FastLogger.logException(e);
            }
        });
    }

    public void close() {
        File file = new File(Koi.getInstance().getDir(), "/users/" + this.platform + "/" + this.UUID);
        JsonObject json = new JsonObject();
        JsonArray followers = new JsonArray();

        this.followers.forEach((follower) -> followers.add(follower));
        json.add("followers", followers);

        Event recentFollow = this.dataEvents.get(EventType.FOLLOW);
        if (recentFollow != null) {
            JsonObject event = recentFollow.serialize().getAsJsonObject("event");
            JsonObject follower = event.getAsJsonObject("follower");

            json.addProperty("recent_follower", follower.get("UUID").getAsString());
        }

        this.preferences.save();

        this.close0(json);

        FileUtil.writeJson(file, json);
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

    public void wake() {
        this.lastWake = System.currentTimeMillis();
    }

    public void broadcastEvent(Event e) {
        try {
            if (this.username.equalsIgnoreCase("Casterlabs") && (e.getType() == EventType.CHAT)) {
                CommandsRegistry.triggerCommand((ChatEvent) e);
            }

            for (EventListener listener : new ArrayList<>(this.eventListeners)) {
                listener.onEvent(e);
            }

            eventStat.tick();

            if (e.getType() == EventType.FOLLOW) {
                FollowEvent event = (FollowEvent) e;

                this.followers.add(event.getFollower().getUUID());
                InfoEvent recentFollow = new InfoEvent(event);

                this.dataEvents.put(EventType.FOLLOW, recentFollow);
                this.broadcastEvent(recentFollow);
            } else if (e.getType() == EventType.STREAM_STATUS) {
                this.dataEvents.put(EventType.STREAM_STATUS, e);
            }
        } catch (Exception ex) {
            FastLogger.logStatic(LogLevel.SEVERE, "An error occured whilst broadcasting event as %s//%s", this.UUID, this.username);
            FastLogger.logException(ex);
        }
    }

    public void update() throws IdentifierException {
        if (UPDATE_AGE < (System.currentTimeMillis() - this.lastWake)) {
            this.wake();
            this.updateUser();

            this.broadcastEvent(new UserUpdateEvent(this));
        }

        this.preferences.wake();

        this.update0();
    }

    public boolean hasListeners() {
        return this.eventListeners.size() != 0;
    }

    public void tryExternalHook() {}

    protected abstract void update0();

    protected abstract void updateUser() throws IdentifierException;

    public abstract void updateUser(@Nullable Object obj);

    protected abstract void close0(JsonObject save);

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof User) {
            User other = (User) obj;

            return (this.platform == other.platform) && (this.UUID.equals(other.UUID));
        } else {
            return false;
        }
    }

}
