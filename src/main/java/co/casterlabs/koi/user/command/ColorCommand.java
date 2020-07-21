package co.casterlabs.koi.user.command;

import co.casterlabs.koi.events.UserUpdateEvent;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.koi.util.MiscUtil;
import javafx.scene.paint.Color;
import xyz.e3ndr.endersutil.input.CommandExecutor;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

public class ColorCommand implements CommandExecutor<User> {

    @Override
    public String onCommand(String[] args, String input, User executor) {
        if (executor.getPlatform() == UserPlatform.CAFFEINE) {
            try {
                Color color = Color.web(args[1].toLowerCase());
                String hex = MiscUtil.getHexForColor(color);

                executor.setColor(hex);
                executor.broadcastEvent(new UserUpdateEvent(executor));
                FastLogger.logStatic(LogLevel.INFO, "%s changed their color to %s", executor.getUsername(), hex);
            } catch (Exception ignored) {}
        }

        return null;
    }

}
