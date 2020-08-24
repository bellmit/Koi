package co.casterlabs.koi.user.command;

import co.casterlabs.koi.user.SerializedUser;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.koi.user.UserPreferences;
import co.casterlabs.koi.util.MiscUtil;
import javafx.scene.paint.Color;
import xyz.e3ndr.endersutil.input.CommandExecutor;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

@SuppressWarnings("restriction")
public class ColorCommand implements CommandExecutor<SerializedUser> {

    @Override
    public String onCommand(String[] args, String input, SerializedUser executor) {
        UserPlatform platform = executor.getPlatform();

        if (platform == UserPlatform.CAFFEINE) {
            try {
                Color color = Color.web(args[1].toLowerCase());
                String hex = MiscUtil.getHexForColor(color);
                UserPreferences preferences = UserPreferences.get(platform, executor.getUUID());

                preferences.setColor(hex);
                FastLogger.logStatic(LogLevel.INFO, "%s changed their color to %s", executor.getUUID(), hex);
            } catch (Exception ignored) {}
        }

        return null;
    }

}
