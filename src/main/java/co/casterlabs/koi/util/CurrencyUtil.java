package co.casterlabs.koi.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonObject;

import co.casterlabs.koi.Koi;
import lombok.SneakyThrows;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;
import xyz.e3ndr.fastloggingframework.logging.LogLevel;

public class CurrencyUtil {
    public static final String CURRENCY_LINK = "https://www.localeplanet.com/api/auto/currencymap.json?name=Y"; // https://www.localeplanet.com/api/auto/currencymap.html
    public static final String CURRENCY_RATES = "https://api.exchangeratesapi.io/latest?symbols=USD&base=%s"; // https://exchangeratesapi.io/
    public static final String DIGIE_SYMBOL = "\u2022";

    private static Map<String, JsonObject> conversions = new ConcurrentHashMap<>();
    private static JsonObject currencies = new JsonObject();

    public static void init() {
        Koi.getMiscThreadPool().submit(() -> {
            try {
                currencies = WebUtil.jsonSendHttpGet(CURRENCY_LINK, null, JsonObject.class);
                FastLogger.logStatic(LogLevel.INFO, "Fished grabbing currency info.");
            } catch (Exception e) {
                if (e.getMessage().contains("quota")) {
                    FastLogger.logStatic(LogLevel.WARNING, "Currency api over quota, retrying in 480 seconds!");
                    (new Thread() {
                        @SneakyThrows
                        @Override
                        public void run() {
                            Thread.sleep(TimeUnit.SECONDS.toMillis(480));
                            init();
                        }
                    }).start();
                } else {
                    FastLogger.logException(e);
                }
            }
        });
    }

    private static JsonObject getCurrrencyConversion(String currency) {
        currency = currency.toUpperCase();

        JsonObject json = conversions.get(currency);
        long current = System.currentTimeMillis();

        if ((json == null) || ((current - json.get("time").getAsInt()) > TimeUnit.HOURS.toMillis(1))) {
            String url = String.format(CURRENCY_RATES, currency, currency);

            json = WebUtil.jsonSendHttpGet(url, null, JsonObject.class);
            json.addProperty("time", current);

            conversions.put(currency, json);
        }

        return json;
    }

    public static double translateCurrencyToUSD(double usd, String currency) {
        if (usd == 0) {
            return 0;
        } else if (currency.equalsIgnoreCase("BITS")) {
            return (usd / 1.40) / 100;
        } else if (currency.equalsIgnoreCase("DIGIES") || currency.equalsIgnoreCase("DIGIE")) {
            return usd / 91;
        } else {
            JsonObject json = getCurrrencyConversion(currency);
            JsonObject rates = json.getAsJsonObject("rates");
            double conversion = rates.get(currency).getAsDouble();

            return usd * conversion;
        }
    }

    public static double translateCurrencyFromUSD(double amount, String currency) {
        if (amount == 0) {
            return 0;
        } else if (currency.equalsIgnoreCase("BITS")) {
            return (amount * 1.40) * 100;
        } else if (currency.equalsIgnoreCase("DIGIES") || currency.equalsIgnoreCase("DIGIE")) {
            return amount * 91;
        } else {
            JsonObject json = getCurrrencyConversion(currency);
            JsonObject rates = json.getAsJsonObject("rates");
            double usdConversion = rates.get("USD").getAsDouble();

            return amount * usdConversion;
        }
    }

    public static String formatCurrency(double amount, String currency) {
        int fractionalDigits = 2;
        String symbol = currency;

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
            return String.valueOf((int) Math.floor(value));
        } else {
            return String.format("%." + fractionalDigits + "f", value);
        }
    }

}
