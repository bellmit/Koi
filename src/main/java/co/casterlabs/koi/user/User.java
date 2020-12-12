package co.casterlabs.koi.user;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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
import co.casterlabs.koi.events.UserUpdateEvent;
import co.casterlabs.koi.networking.SocketClient;
import co.casterlabs.koi.user.command.UserCommands;
import co.casterlabs.koi.util.FileUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

@Getter
@ToString
public abstract class User {
    private static final long UPDATE_AGE = TimeUnit.MINUTES.toMillis(1);

    private @Setter boolean testing = false;

    private UserPlatform platform;
    private String username;
    private long lastWake;

    protected List<String> badges = new ArrayList<>();
    protected boolean slim = false;
    protected long followerCount;
    protected String UUID;

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
                }

            } catch (Exception e) {
                FastLogger.logStatic(LogLevel.SEVERE, "Error while reading user file for %s.", this.username);
                FastLogger.logException(e);
            }
        });
    }

    public void calculateScopes() {
        for (EventListener listener : this.eventListeners) {
            if (listener instanceof SocketClient) {
                if (((SocketClient) listener).isSlim()) {
                    this.slim = true;
                    return;
                }
            }
        }

        this.slim = false;
    }

    public void close() {
        File file = new File(Koi.getInstance().getDir(), "/users/" + this.platform + "/" + this.UUID);
        JsonObject json = new JsonObject();
        JsonArray followers = new JsonArray();

        this.followers.forEach((follower) -> followers.add(follower));

        json.add("followers", followers);

        this.preferences.save();

        this.close0();

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
            if (e instanceof ChatEvent) {
                try {
                    UserCommands.triggerCommand((ChatEvent) e);
                } catch (Exception ignored) {}
            }

            for (EventListener listener : new ArrayList<>(this.eventListeners)) {
                if (this.testing) {
                    for (int i = 0; i != 100; i++) {
                        listener.onEvent(e);
                    }
                } else {
                    listener.onEvent(e);
                }
            }

            if (e.getType() == EventType.STREAM_STATUS) {
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

            if (!this.slim) {
                this.updateUser();
                FastLogger.logStatic(LogLevel.DEBUG, "Polling %s/%s", this.UUID, this.getUsername());

                this.broadcastEvent(new UserUpdateEvent(this));
            }
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

    protected abstract void close0();

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
