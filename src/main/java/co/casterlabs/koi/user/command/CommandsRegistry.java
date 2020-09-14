package co.casterlabs.koi.user.command;

import java.util.HashSet;
import java.util.Set;

import co.casterlabs.koi.events.ChatEvent;
import co.casterlabs.koi.user.SerializedUser;
import xyz.e3ndr.endersutil.input.Command;
import xyz.e3ndr.endersutil.input.CommandResponse;

public class CommandsRegistry {
    public static final char FLAG = '!';

    private static Set<Command<SerializedUser>> commands = new HashSet<>();

    static {
        commands.add(new Command<>(2, new ColorCommand(), FLAG + "color", "*"));
    }

    public static void triggerCommand(ChatEvent message) {
        for (Command<SerializedUser> command : commands) {
            CommandResponse response = command.execute(message.getMessage(), message.getSender());

            if (!response.hasError()) {
                return;
            }
        }
    }

}
