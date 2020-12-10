package co.casterlabs.koi;

import java.util.Collection;
import java.util.List;

import co.casterlabs.koi.user.SerializedUser;
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
    @Command(name = "addbadge", description = "Adds a badge to a user", minimumArguments = 3)
    public void addbadge(CommandEvent<Void> event) {
        SerializedUser user = Koi.getInstance().getUserSerialized(event.getArgs()[0], UserPlatform.valueOf(event.getArgs()[1].toUpperCase()));
        List<String> badges = UserPolyFill.get(user.getPlatform(), user.getUUID()).getForcedBadges();

        badges.add(event.getArgs()[2]);

        Koi.getInstance().getLogger().info("Added %s to %s;%s", event.getArgs()[2], event.getArgs()[0], event.getArgs()[1]);
    }

    @SneakyThrows
    @Command(name = "getbadges", description = "Gets all badges applied to a user", minimumArguments = 2)
    public void getbadges(CommandEvent<Void> event) {
        SerializedUser user = Koi.getInstance().getUserSerialized(event.getArgs()[0], UserPlatform.valueOf(event.getArgs()[1].toUpperCase()));
        List<String> badges = UserPolyFill.get(user.getPlatform(), user.getUUID()).getForcedBadges();

        Koi.getInstance().getLogger().info("Badges:\n", String.join("\n", badges));
    }

    @SneakyThrows
    @Command(name = "removebadge", description = "Gets all badges applied to a user", minimumArguments = 3)
    public void removebadge(CommandEvent<Void> event) {
        SerializedUser user = Koi.getInstance().getUserSerialized(event.getArgs()[0], UserPlatform.valueOf(event.getArgs()[1].toUpperCase()));
        List<String> badges = UserPolyFill.get(user.getPlatform(), user.getUUID()).getForcedBadges();

        badges.remove(event.getArgs()[2]);

        Koi.getInstance().getLogger().info("Removed %s from %s;%s", event.getArgs()[2], event.getArgs()[0], event.getArgs()[1]);
    }

}
