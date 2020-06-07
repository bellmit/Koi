package co.casterlabs.koi.user.caffeine;

import co.casterlabs.koi.Koi;

public enum CaffeineAlertType {
    REACTION,
    DIGITAL_ITEM,
    SHARE,
    UNKNOWN;

    public static CaffeineAlertType valueOfString(String type) {
        switch (type.toUpperCase()) {
            case "REACTION":
                return REACTION;

            case "DIGITAL_ITEM":
                return DIGITAL_ITEM;

            case "RESCIND": // ?
                return UNKNOWN;

            case "SHARE":
                return SHARE;
        }

        Koi.getInstance().getLogger().debug("New alert type discovered! " + type);

        return UNKNOWN;
    }

}
