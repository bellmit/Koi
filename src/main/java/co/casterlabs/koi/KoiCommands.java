package co.casterlabs.koi;

import java.util.Collection;

import co.casterlabs.koi.networking.SocketServer;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.koi.user.twitch.TwitchCredentialsAuth;
import co.casterlabs.twitchapi.helix.webhooks.HelixGetWebhookSubscriptionsRequest;
import co.casterlabs.twitchapi.helix.webhooks.HelixGetWebhookSubscriptionsRequest.WebhookSubscription;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import xyz.e3ndr.consolidate.CommandEvent;
import xyz.e3ndr.consolidate.CommandRegistry;
import xyz.e3ndr.consolidate.command.Command;
import xyz.e3ndr.consolidate.command.CommandListener;

@AllArgsConstructor
public class KoiCommands implements CommandListener<Void> {
    private CommandRegistry<Void> registry;

    @Command(name = "help", description = "Shows this page.")
    public void onHelpCommand(CommandEvent<Void> event) {
        Collection<Command> commands = registry.getCommands();
        StringBuilder sb = new StringBuilder();

        sb.append("All available commands:");

        for (Command c : commands) {
            sb.append("\n\t").append(c.name()).append(": ").append(c.description());
        }

        Koi.getInstance().getLogger().info(sb);
    }

    @SneakyThrows
    @Command(name = "stop", description = "Stops Koi.")
    public void onStopCommand(CommandEvent<Void> event) {
        TwitchCredentialsAuth auth = (TwitchCredentialsAuth) Koi.getInstance().getAuthProvider(UserPlatform.TWITCH);
        HelixGetWebhookSubscriptionsRequest request = new HelixGetWebhookSubscriptionsRequest(auth);

        request.sendAsync().thenAccept((subscriptions) -> {
            Koi.getInstance().getLogger().info("Removing %d webhooks.", subscriptions.size());

            for (WebhookSubscription sub : subscriptions) {
                try {
                    sub.remove(auth);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        Koi.getInstance().stop();
    }

    @Command(name = "broadcast", description = "Broadcasts a message to all Casterlabs users.", minimumArguments = 1)
    public void onBroadcastCommand(CommandEvent<Void> event) {
        SocketServer.getInstance().systemBroadcast(String.join(" ", event.getArgs()));
    }

    @Command(name = "reload", description = "Reloads *some* config files.")
    public void onReloadNoticesCommand(CommandEvent<Void> event) {
        Koi.getInstance().reloadBadges();
        Koi.getInstance().reloadNotices();
        Koi.getInstance().reloadClientIds();
    }

}
