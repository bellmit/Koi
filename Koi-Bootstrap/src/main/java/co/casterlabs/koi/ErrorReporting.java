package co.casterlabs.koi;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;

import org.jetbrains.annotations.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import lombok.NonNull;
import lombok.SneakyThrows;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LoggingUtil;

public class ErrorReporting {
    private static final File FILE = new File("errors.json");
    private static final FastLogger logger = new FastLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static JsonObject json = new JsonObject();

    static {
        try {
            if (FILE.exists()) {
                FILE.delete();
                FILE.createNewFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void uncaughterror(@NonNull Throwable t) {
        Class<?> calling = LoggingUtil.getCallingClass();
        JsonObject error = new JsonObject();

        error.add("data", null);
        error.addProperty("stack", LoggingUtil.getExceptionStack(t));
        error.addProperty("timestamp", Instant.now().toString());

        JsonArray section = getSection(calling.getPackage().getName() + "." + calling.getSimpleName());

        section.add(error);

        logger.severe("Logged an error from %s.%s", calling.getPackage().getName(), calling.getSimpleName());

        save();
    }

    public static void genericerror(@Nullable String data, @NonNull Throwable t) {
        Class<?> calling = LoggingUtil.getCallingClass();
        JsonObject error = new JsonObject();

        error.addProperty("data", data);
        error.addProperty("stack", LoggingUtil.getExceptionStack(t));
        error.addProperty("timestamp", Instant.now().toString());

        JsonArray section = getSection(calling.getPackage().getName() + "." + calling.getSimpleName());

        section.add(error);

        logger.severe("Logged an error from %s.%s", calling.getPackage().getName(), calling.getSimpleName());

        save();
    }

    public static void webhookerror(@NonNull String url, @Nullable String body, @Nullable Object headers, @NonNull Throwable t) {
        Class<?> calling = LoggingUtil.getCallingClass();
        JsonObject error = new JsonObject();
        JsonObject data = new JsonObject();

        data.addProperty("url", url);
        data.addProperty("body", body);
        data.addProperty("headers", String.valueOf(headers));

        error.add("data", data);
        error.addProperty("stack", LoggingUtil.getExceptionStack(t));
        error.addProperty("timestamp", Instant.now().toString());

        JsonArray section = getSection(calling.getPackage().getName() + "." + calling.getSimpleName());

        section.add(error);

        logger.severe("Logged an error from %s.%s", calling.getPackage().getName(), calling.getSimpleName());

        save();
    }

    public static void apierror(@NonNull Class<?> calling, @NonNull String url, @Nullable String sentBody, @Nullable Object sentHeaders, @Nullable String recBody, @Nullable Object recHeaders, @NonNull Throwable t) {
        JsonObject error = new JsonObject();
        JsonObject data = new JsonObject();

        data.addProperty("url", url);
        data.addProperty("sent_body", sentBody);
        data.addProperty("sent_headers", String.valueOf(sentHeaders));
        data.addProperty("recieved_body", recBody);
        data.addProperty("recieved_headers", String.valueOf(recHeaders));

        error.add("data", data);
        error.addProperty("stack", LoggingUtil.getExceptionStack(t));
        error.addProperty("timestamp", Instant.now().toString());

        JsonArray section = getSection(calling.getPackage().getName() + "." + calling.getSimpleName());

        section.add(error);

        logger.severe("Logged an error from %s.%s", calling.getPackage().getName(), calling.getSimpleName());

        save();
    }

    @SneakyThrows
    private static synchronized void save() {
        Files.write(FILE.toPath(), GSON.toJson(json).getBytes());
    }

    private static synchronized JsonArray getSection(String clazz) {
        JsonElement e = json.get(clazz);

        if (e != null) {
            return e.getAsJsonArray();
        } else {
            JsonArray arr = new JsonArray();

            json.add(clazz, arr);

            return arr;
        }
    }

}
