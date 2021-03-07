package co.casterlabs.koi.config;

import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class ClientIdMeta {
    public static final ClientIdMeta UNKNOWN = new ClientIdMeta();

    @SerializedName("non_logging")
    private boolean nonLogging = false;

    @SerializedName("show_public_stats")
    private boolean showingPublicStats = false;

    private String displayname = "UNKNOWN";

}
