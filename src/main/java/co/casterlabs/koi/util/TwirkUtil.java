package co.casterlabs.koi.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.gikk.twirk.Twirk;

import lombok.SneakyThrows;

public class TwirkUtil {

    @SneakyThrows
    public static void close(Twirk twirk) { // Removes disconnecting message
        if (twirk.isDisposed()) {
            return;
        }

        // isConnected = false;
        set("isConnected", twirk, false);
        // isDisposed = true;
        set("isDisposed", twirk, true);

        // releaseResources();
        invoke("releaseResouces", twirk);
    }

    @SneakyThrows
    private static void invoke(String name, Twirk twirk, Object... params) {
        Class<?>[] clazzes = new Class<?>[params.length];

        for (int i = 0; i != params.length; i++) {
            clazzes[i] = params[i].getClass();
        }

        Method method = twirk.getClass().getDeclaredMethod("releaseResources", clazzes);

        method.setAccessible(true);
        method.invoke(twirk, params);
    }

    @SneakyThrows
    private static void set(String field, Twirk twirk, Object value) {
        Field f = twirk.getClass().getDeclaredField(field);

        f.setAccessible(true);
        f.set(twirk, value);
    }

}
