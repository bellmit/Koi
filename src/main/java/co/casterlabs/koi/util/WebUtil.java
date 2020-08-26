package co.casterlabs.koi.util;

import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import co.casterlabs.koi.Koi;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

public class WebUtil {
    private static @NonNull @Getter @Setter Proxy proxy = Proxy.NO_PROXY;

    public static boolean isUsingProxy() {
        return proxy != Proxy.NO_PROXY;
    }

    @SneakyThrows
    public static <T> T jsonSendHttp(String body, String address, Map<String, String> headers, Class<T> clazz) {
        String json = sendHttp(body, address, headers);

        try {
            return Koi.GSON.fromJson(json, clazz);
        } catch (Exception e) {
            FastLogger.logStatic(LogLevel.SEVERE, "Invalid json: (%s)\n%s", address, json);

            throw e;
        }
    }

    public static <T> T jsonSendHttpGet(String address, Map<String, String> header, Class<T> clazz) {
        return jsonSendHttp(null, address, header, clazz);
    }

    public static String sendHttpGet(String address, Map<String, String> header) {
        return sendHttp(null, address, header);
    }

    @SneakyThrows
    public static String sendHttp(String body, String address, Map<String, String> headers) {
        OkHttpClient client = new OkHttpClient().newBuilder().proxy(proxy).build();
        Request.Builder builder = new Request.Builder().url(address);

        if (body != null) {
            builder.post(RequestBody.create(body.getBytes(StandardCharsets.UTF_8)));
        }

        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }

        Request request = builder.build();
        Response response = client.newCall(request).execute();

        return response.body().string();
    }

}
