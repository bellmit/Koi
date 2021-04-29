package co.casterlabs.koi;

import co.casterlabs.koi.config.ThirdPartyBannerConfig;
import co.casterlabs.koi.user.UserPlatform;

public class Test {

    public static void main(String[] args) throws Exception {
        System.out.println(ThirdPartyBannerConfig.getBanners(UserPlatform.BRIME));
    }

}
