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

public class WebUtil {
    private static @NonNull @Getter @Setter Proxy proxy = Proxy.NO_PROXY;
    private static FastLogger logger = new FastLogger();

    public static boolean isUsingProxy() {
        return proxy != Proxy.NO_PROXY;
    }

    @SneakyThrows
    public static <T> T getJsonFromString(String json, Class<T> clazz) {
        try {
            return Koi.GSON.fromJson(json, clazz);
        } catch (Exception e) {
            logger.severe("Invalid json: " + e.getMessage());
            logger.debug(json.toString());

            return clazz.newInstance();
        }
    }

    public static <T> T jsonSendHttp(String body, String address, Map<String, String> headers, Class<T> clazz) {
        return getJsonFromString(sendHttp(body, address, headers), clazz);
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
