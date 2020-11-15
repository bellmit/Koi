package co.casterlabs.koi.user.command;

import co.casterlabs.koi.user.PolyFillRequirements;
import co.casterlabs.koi.user.SerializedUser;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.koi.user.UserPolyFill;
import co.casterlabs.koi.util.MiscUtil;
import javafx.scene.paint.Color;
import xyz.e3ndr.consolidate.CommandEvent;
import xyz.e3ndr.consolidate.command.Command;
import xyz.e3ndr.consolidate.command.CommandListener;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

@SuppressWarnings("restriction")
public class ColorCommand implements CommandListener<SerializedUser> {

    @Command(name = "!color", description = "Sets your color.", minimumArguments = 1)
    public void onCommand(CommandEvent<SerializedUser> event) {
        UserPlatform platform = event.getExecutor().getPlatform();

        if (PolyFillRequirements.getPolyFillForPlatform(platform).contains(PolyFillRequirements.COLOR)) {
            try {
                Color color = Color.web(event.getArgs()[0].toLowerCase());
                String hex = MiscUtil.getHexForColor(color);
                UserPolyFill preferences = UserPolyFill.get(platform, event.getExecutor().getUUID());

                preferences.set(PolyFillRequirements.COLOR, hex);

                FastLogger.logStatic(LogLevel.INFO, "%s changed their color to %s", event.getExecutor().getUUID(), hex);
            } catch (Exception ignored) {} // Invalid color
        }
    }

}
