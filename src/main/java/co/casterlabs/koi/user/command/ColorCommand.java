package co.casterlabs.koi.user.command;

import com.google.gson.JsonObject;

import co.casterlabs.koi.user.PlatformException;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.koi.user.UserPreferences;
import co.casterlabs.koi.util.MiscUtil;
import javafx.scene.paint.Color;
import xyz.e3ndr.endersutil.input.CommandExecutor;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

@SuppressWarnings("restriction")
public class ColorCommand implements CommandExecutor<JsonObject> {

    @Override
    public String onCommand(String[] args, String input, JsonObject executor) {
        try {
            UserPlatform platform = UserPlatform.parse(executor.get("platform"));

            if (platform == UserPlatform.CAFFEINE) {
                try {
                    Color color = Color.web(args[1].toLowerCase());
                    String hex = MiscUtil.getHexForColor(color);
                    UserPreferences preferences = UserPreferences.get(platform, executor.get("UUID").getAsString());

                    preferences.setColor(hex);
                    FastLogger.logStatic(LogLevel.INFO, "%s changed their color to %s", executor.get("UUID").getAsString(), hex);
                } catch (Exception ignored) {}
            }

        } catch (PlatformException e) {
            e.printStackTrace();
        }

        return null;
    }

}
