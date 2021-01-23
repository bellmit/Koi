package co.casterlabs.koi;

import lombok.Getter;
import lombok.Setter;

@Getter
public class KoiConfig {
    private String natsukashiiEndpoint;

    private boolean caffeineEnabled;

    private String trovoId;

    private String twitchId;
    private String twitchSecret;
    private String twitchAddress;
    private int twitchPort = 9098;

    private String host = "127.0.0.1";
    private int port = 8080;

    private @Setter boolean debugModeEnabled;

    public boolean isTrovoEnabled() {
        return this.trovoId != null;
    }

    public boolean isTwitchEnabled() {
        return !anyNull(this.twitchId, this.twitchSecret, this.twitchAddress);
    }

    private static boolean anyNull(Object... objs) {
        for (Object o : objs) {
            if (o == null) {
                return true;
            }
        }

        return false;
    }

}
