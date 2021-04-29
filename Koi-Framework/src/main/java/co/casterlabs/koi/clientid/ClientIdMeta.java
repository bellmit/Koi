package co.casterlabs.koi.clientid;

import java.util.Collections;
import java.util.List;

import com.google.gson.annotations.SerializedName;

import co.casterlabs.koi.user.UserPlatform;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ClientIdMeta {
    public static final ClientIdMeta UNKNOWN = new ClientIdMeta(false, true, "UNKNOWN", null, Collections.emptyList(), Collections.emptyList());

    @SerializedName("non_logging")
    private boolean nonLogging = false;

    @SerializedName("show_public_stats")
    private boolean showingPublicStats = false;

    private String name = "UNKNOWN";

    @SerializedName("broadcast_platform")
    private UserPlatform broadcastPlatform;

    @SerializedName("allowed_stream_status_users")
    private List<String> allowedStreamStatusUsers = Collections.emptyList();

    private List<ClientIdScope> scopes = Collections.emptyList();

    /* ---------------- */
    /* Helpers          */
    /* ---------------- */

    public boolean hasScope(ClientIdScope scope) {
        return this.scopes.contains(scope);
    }

    public boolean hasStreamStatus(String username, UserPlatform platform) {
        if (this.scopes.contains(ClientIdScope.WILDCARD_STREAM_STATUS)) {
            return true;
        } else if (this.scopes.contains(ClientIdScope.STREAM_STATUS)) {
            return this.allowedStreamStatusUsers.contains(username.toLowerCase() + ";" + platform);
        } else {
            return false;
        }
    }

}
