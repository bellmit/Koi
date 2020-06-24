package co.casterlabs.koi.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonObject;

import co.casterlabs.koi.Koi;
import xyz.e3ndr.FastLoggingFramework.Logging.FastLogger;

public class CurrencyUtil {
    public static final String CURRENCY_LINK = "https://www.localeplanet.com/api/auto/currencymap.json?name=Y"; // https://www.localeplanet.com/api/auto/currencymap.html
    public static final String CURRENCY_RATES = "https://api.exchangeratesapi.io/latest?symbols=USD,%s&base=%s"; // https://exchangeratesapi.io/
    public static final String DIGIE_SYMBOL = String.valueOf('\u2022');

    private static Map<String, JsonObject> conversions = new ConcurrentHashMap<>();
    private static JsonObject currencies = new JsonObject();

    public static JsonObject getCurrencyJson() {
        return currencies.deepCopy();
    }

    public static void init() {
        Koi.getMiscThreadPool().submit(() -> {
            currencies = WebUtil.jsonSendHttpGet(CURRENCY_LINK, null, JsonObject.class);
            new FastLogger().info("Fished grabbing currency info.");
        });
    }

    private static JsonObject getCurrrencyConversion(String currency) {
        JsonObject json = conversions.get(currency.toUpperCase());
        long current = System.currentTimeMillis();

        if ((json == null) || ((current - json.get("time").getAsInt()) > TimeUnit.HOURS.toMillis(1))) {
            String url = String.format(CURRENCY_RATES, currency, currency);

            json = WebUtil.jsonSendHttpGet(url, null, JsonObject.class);
            json.addProperty("time", current);

            conversions.put(currency.toUpperCase(), json);
        }

        return json;
    }

    public static double translateCurrency(double amount, String currency) {
        if (amount == 0) {
            return 0;
        } else if (currency.equalsIgnoreCase("BITS")) {
            return (amount / 100) * 1.40;
        } else if (currency.equalsIgnoreCase("DIGIES") || currency.equalsIgnoreCase("DIGIE")) {
            return amount / 91;
        } else {
            JsonObject json = getCurrrencyConversion(currency);
            JsonObject rates = json.getAsJsonObject("rates");
            int usdConversion = rates.get("USD").getAsInt();

            return amount * usdConversion;
        }
    }

    public static String formatCurrency(double amount, String currency) {
        int fractionalDigits = 0;
        String symbol = "?";

        if (currency.equalsIgnoreCase("BITS")) {
            symbol = "";
        } else if (currency.equalsIgnoreCase("DIGIES") || currency.equalsIgnoreCase("DIGIE")) {
            symbol = DIGIE_SYMBOL;
        } else if (currencies.has(currency.toUpperCase())) {
            JsonObject json = currencies.getAsJsonObject(currency.toUpperCase());

            symbol = json.get("symbol").getAsString();
            fractionalDigits = json.get("decimal_digits").getAsInt();
        }

        return String.format("%s%s", symbol, formatDouble(amount, fractionalDigits));
    }

    public static String formatDouble(double value, int fractionalDigits) {
        if (Math.floor(value) == value) {
            return String.valueOf(Math.floor(value));
        } else {
            return String.format("%." + fractionalDigits + "f", value);
        }
    }

}
