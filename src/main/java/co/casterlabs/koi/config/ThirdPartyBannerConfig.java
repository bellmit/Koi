package co.casterlabs.koi.config;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.google.gson.reflect.TypeToken;

import co.casterlabs.koi.Koi;
import co.casterlabs.koi.RepeatingThread;
import co.casterlabs.koi.networking.outgoing.ClientBannerNotice;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.koi.util.FileUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

public class ThirdPartyBannerConfig {
    private static final long EXPIRE_TIME = TimeUnit.DAYS.toMillis(1);
    private static final File FILE = new File("thirdpartybanners.json");
    private static final Type TYPE = new TypeToken<Map<UserPlatform, List<Banner>>>() {
    }.getType();

    private static Map<UserPlatform, List<Banner>> platformBanners = new HashMap<>();

    static {
        for (UserPlatform platform : UserPlatform.values()) {
            platformBanners.put(platform, new ArrayList<>());
        }

        new RepeatingThread("ThirdPartyBannerConfig", TimeUnit.MINUTES.toMillis(1), () -> {
            for (List<Banner> entry : platformBanners.values()) {
                for (Banner banner : new ArrayList<>(entry)) {
                    if (banner.hasExpired()) {
                        entry.remove(banner);
                    }
                }
            }

            save();
        }).start();

        try {
            platformBanners = FileUtil.readJson(FILE, TYPE);
        } catch (IOException e) {
            // Save blank
            save();
        }
    }

    public static List<ClientBannerNotice> getBanners(@NonNull UserPlatform platform) {
        List<ClientBannerNotice> notices = new ArrayList<>();

        for (Banner banner : new ArrayList<>(platformBanners.get(platform))) {
            notices.add(banner.toNotice());
        }

        return notices;
    }

    public static Banner addBanner(@NonNull UserPlatform platform, @NonNull String message) {
        Banner banner = new Banner(UUID.randomUUID().toString(), platform.getColor(), message, System.currentTimeMillis());

        platformBanners.get(platform).add(banner);

        return banner;
    }

    private static void save() {
        FileUtil.write(FILE, Koi.GSON.toJson(platformBanners));
    }

    @Getter
    @AllArgsConstructor
    public static class Banner {
        private String id;
        private String color;
        private String message;
        private long createdAt;

        public ClientBannerNotice toNotice() {
            return new ClientBannerNotice(Koi.GSON.toJsonTree(this));
        }

        public boolean hasExpired() {
            return (System.currentTimeMillis() - this.createdAt) > EXPIRE_TIME;
        }

    }

}
