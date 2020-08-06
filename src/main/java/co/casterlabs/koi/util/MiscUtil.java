package co.casterlabs.koi.util;

import javafx.scene.paint.Color;

@SuppressWarnings("restriction")
public class MiscUtil {

    public static String getHex(int number) {
        String hex = Integer.toHexString(number).toUpperCase();

        return (hex.length() == 1) ? ("0" + hex) : hex;
    }

    public static String getHexForColor(Color color) {
        String red = getHex((int) (color.getRed() * 255));
        String green = getHex((int) (color.getGreen() * 255));
        String blue = getHex((int) (color.getBlue() * 255));
        String hex = String.format("#%s%s%s", red, green, blue);

        return hex;
    }

}
