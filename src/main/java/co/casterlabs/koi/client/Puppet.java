package co.casterlabs.koi.client;

import org.jetbrains.annotations.Nullable;

import co.casterlabs.apiutil.auth.ApiAuthException;
import co.casterlabs.koi.Natsukashii;
import co.casterlabs.koi.Natsukashii.AuthException;
import co.casterlabs.koi.user.IdentifierException;
import co.casterlabs.koi.user.PlatformException;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.koi.user.twitch.TwitchPuppetMessages;
import co.casterlabs.koi.user.twitch.TwitchTokenAuth;
import lombok.Getter;
import lombok.NonNull;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

public class Puppet {
    private @Nullable @Getter ClientAuthProvider auth;
    private @Getter boolean expired;
    private @Getter String token;

    private @Getter Client client;

    private TwitchPuppetMessages puppetMessages;

    public Puppet(@NonNull Client client, @NonNull String token) throws IdentifierException, PlatformException {
        try {
            this.auth = Natsukashii.get(token);

            // Make sure the puppet is the same platform as
            // the client, otherwise the world melts.
            if (this.auth.getPlatform().isEnabled() && (this.auth.getPlatform() == client.getAuth().getPlatform())) {
                this.token = token;
                this.client = client;

                if (this.auth.getPlatform() == UserPlatform.TWITCH) {
                    this.puppetMessages = new TwitchPuppetMessages(this, (TwitchTokenAuth) this.auth);
                }
            } else {
                throw new PlatformException();
            }
        } catch (AuthException e) {
            FastLogger.logStatic(LogLevel.DEBUG, e);
            throw new IdentifierException();
        }
    }

    public void chat(@NonNull String message) throws UnsupportedOperationException, ApiAuthException {
        this.auth.getPlatform().getProvider().chat(this.client, message, this.auth);
    }

    public void close() {
        this.expired = false;

        if (this.puppetMessages != null) {
            this.puppetMessages.close();
        }
    }

}
