package co.casterlabs.koi.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import com.google.gson.JsonElement;

import co.casterlabs.koi.Koi;
import lombok.SneakyThrows;

public class FileUtil {

    @SneakyThrows
    public static String[] readAllLines(File file) {
        return Files.readAllLines(file.toPath()).toArray(new String[0]);
    }

    @SneakyThrows
    public static void writeJson(File file, JsonElement json) {
        if (!file.exists()) {
            file.createNewFile();
        }

        Files.write(file.toPath(), json.toString().getBytes(StandardCharsets.UTF_8));
    }

    @SneakyThrows
    public static void write(File file, String content) {
        if (!file.exists()) {
            file.createNewFile();
        }

        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
    }

    public static <T> T readJson(File file, Type type) throws IOException {
        byte[] bytes = Files.readAllBytes(file.toPath());
        String str = new String(bytes, StandardCharsets.UTF_8);

        return Koi.GSON.fromJson(str, type);
    }

    @SuppressWarnings("unchecked")
    public static <T> T readJsonOrDefault(File file, T def) {
        try {
            return (T) readJson(file, def.getClass());
        } catch (Exception e) {
            return def;
        }
    }

}
