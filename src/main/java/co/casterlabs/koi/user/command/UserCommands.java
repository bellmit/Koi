package co.casterlabs.koi.user.command;

import co.casterlabs.koi.events.ChatEvent;
import co.casterlabs.koi.user.SerializedUser;
import xyz.e3ndr.consolidate.CommandRegistry;
import xyz.e3ndr.consolidate.exception.ArgumentsLengthException;
import xyz.e3ndr.consolidate.exception.CommandExecutionException;
import xyz.e3ndr.consolidate.exception.CommandNameException;

public class UserCommands {

    private static CommandRegistry<SerializedUser> commands = new CommandRegistry<>();

    static {
        commands.addCommand(new ColorCommand());
    }

    public static void triggerCommand(ChatEvent message) throws CommandNameException, CommandExecutionException, ArgumentsLengthException {
        commands.execute(message.getMessage(), message.getSender());
    }

}
