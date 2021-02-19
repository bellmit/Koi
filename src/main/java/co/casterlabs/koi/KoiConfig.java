package co.casterlabs.koi;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
public class KoiConfig {
    private String natsukashiiPrivateEndpoint;

    private boolean caffeineEnabled;

    private String trovoId;

    private String twitchId;
    private String twitchSecret;
    private String twitchAddress;
    private int twitchPort = 9098;

    private String host = "127.0.0.1";
    private int port = 8080;

    private List<String> nonLoggingClientIds = new ArrayList<>();

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
