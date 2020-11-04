package co.casterlabs.koi;

import java.util.Collection;

import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.koi.user.UserPolyFill;
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

    @SneakyThrows
    @Command(name = "addbadge", description = "Reloads the preferences", minimumArguments = 3)
    public void preferencesreload(CommandEvent<Void> event) {
        UserPolyFill.get(UserPlatform.valueOf(event.getArgs()[1]), event.getArgs()[0]).getForcedBadges().add(event.getArgs()[2]);
        Koi.getInstance().getLogger().info("Added %s to %s;%s", event.getArgs()[2], event.getArgs()[0], event.getArgs()[1]);
    }

}
