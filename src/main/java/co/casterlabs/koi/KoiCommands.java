package co.casterlabs.koi;

import java.util.Collection;

import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.koi.user.twitch.TwitchAuth;
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
    public void help(CommandEvent<Void> event) {
        Collection<Command> commands = registry.getCommands();
        StringBuilder sb = new StringBuilder();

        sb.append("All available commands:");

        for (Command c : commands) {
            sb.append("\n\t").append(c.name()).append(": ").append(c.description());
        }

        Koi.getInstance().getLogger().info(sb);
    }

    @SneakyThrows
    @Command(name = "stop", description = "Stops all listening servers.")
    public void stop(CommandEvent<Void> event) {
        Koi.getInstance().stop();

        TwitchAuth auth = (TwitchAuth) Koi.getInstance().getAuthProvider(UserPlatform.TWITCH);
        HelixGetWebhookSubscriptionsRequest request = new HelixGetWebhookSubscriptionsRequest(auth);

        request.sendAsync().thenAccept((subscriptions) -> {
            for (WebhookSubscription sub : subscriptions) {
                try {
                    sub.remove(auth);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            Koi.getInstance().getLogger().info("Removed %d webhooks.", subscriptions.size());
        });

        Koi.getInstance().getLogger().info("Removed %d users.", UserPlatform.removeAll());
    }

    @Command(name = "broadcast", description = "Sends a message to all connected user's chat.", minimumArguments = 1)
    public void broadcast(CommandEvent<Void> event) {
        String message = String.join(" ", event.getArgs()).trim();

        if (message.length() > 58) {
            Koi.getInstance().getLogger().info("Message is too long (58 chars)");
        } else {
            for (User user : UserPlatform.getAll()) {
                Koi.getInstance().getAuthProvider(user.getPlatform()).sendChatMessage(user, String.format("@%s %s", user.getUsername(), message));
            }
        }
    }

}
