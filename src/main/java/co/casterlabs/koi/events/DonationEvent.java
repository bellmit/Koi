package co.casterlabs.koi.events;

import com.google.gson.annotations.SerializedName;

import co.casterlabs.koi.user.SerializedUser;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.util.CurrencyUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class DonationEvent extends ChatEvent {
    @SerializedName("usd_equivalent")
    private double usdEquivalent;
    private String formatted;
    private String currency;
    private double amount;
    private String image;

    public DonationEvent(String id, String message, SerializedUser sender, User streamer, String image, String currency, double amount) {
        super(id, message, sender, streamer);
        this.usdEquivalent = CurrencyUtil.translateCurrencyToUSD(amount, currency);
        this.formatted = CurrencyUtil.formatCurrency(amount, currency);
        this.currency = currency.toUpperCase();
        this.amount = amount;
        this.image = image;
    }

    @Override
    public EventType getType() {
        return EventType.DONATION;
    }

}
