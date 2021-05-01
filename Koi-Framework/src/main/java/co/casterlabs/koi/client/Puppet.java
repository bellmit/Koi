package co.casterlabs.koi.client;

import java.io.Closeable;
import java.io.IOException;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.koi.Natsukashii;
import co.casterlabs.koi.clientid.ClientIdMismatchException;
import co.casterlabs.koi.user.IdentifierException;
import co.casterlabs.koi.user.PlatformException;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

@Getter
public class Puppet {
    private @Nullable ClientAuthProvider auth;
    private boolean expired;
    private String token;

    private Client client;

    private @Setter Closeable closeable;

    public Puppet(@NonNull Client client, @NonNull String token, @NonNull String clientId) throws IdentifierException, PlatformException, ClientIdMismatchException {
        try {
            this.auth = Natsukashii.get(token, clientId);

            // Make sure the puppet is the same platform as
            // the client, otherwise the world melts.
            if (this.auth.getPlatform().isEnabled() && (this.auth.getPlatform() == client.getAuth().getPlatform())) {
                this.token = token;
                this.client = client;

                this.auth.getPlatform().getProvider().initializePuppet(this);
            } else {
                throw new PlatformException();
            }
        } catch (ApiAuthException e) {
            FastLogger.logStatic(LogLevel.DEBUG, e);
            throw new IdentifierException();
        }
    }

    public void chat(@NonNull String message) throws UnsupportedOperationException, ApiAuthException {
        this.auth.getPlatform().getProvider().chatAsPuppet(this, message);
    }

    public void close() {
        this.expired = false;

        if (this.closeable != null) {
            try {
                this.closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
