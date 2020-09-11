package co.casterlabs.koi.user.command;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import co.casterlabs.koi.events.ChatEvent;
import co.casterlabs.koi.user.SerializedUser;
import co.casterlabs.koi.user.UserPlatform;
import xyz.e3ndr.endersutil.input.Command;
import xyz.e3ndr.endersutil.input.CommandResponse;

public class CommandsRegistry {
    private static final List<UserPlatform> polyfillPlatforms = Arrays.asList(UserPlatform.CAFFEINE);
    public static final char FLAG = '!';

    private static Set<Command<SerializedUser>> commands = new HashSet<>();

    static {
        commands.add(new Command<>(2, new ColorCommand(), FLAG + "color", "*"));
    }

    public static void triggerCommand(ChatEvent message) {
        if (polyfillPlatforms.contains(message.getSender().getPlatform())) {
            for (Command<SerializedUser> command : commands) {
                CommandResponse response = command.execute(message.getMessage(), message.getSender());

                if (!response.hasError()) {
                    return;
                }
            }
        }
    }

}
