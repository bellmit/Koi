package co.casterlabs.koi.events;

import com.google.gson.annotations.SerializedName;

import co.casterlabs.koi.user.SerializedUser;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.util.CurrencyUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
public class DonationEvent extends Event {
    private String id;
    private User streamer;
    private String message;
    private SerializedUser sender;
    private String image;
    private String currency;
    private String formatted;
    private double amount;
    @SerializedName("usd_equivalent")
    private double usdEquivalent;

    public DonationEvent(String id, String message, SerializedUser sender, User streamer, String image, String currency, double amount) {
        this.id = id;
        this.usdEquivalent = CurrencyUtil.translateCurrency(amount, currency);
        this.formatted = CurrencyUtil.formatCurrency(amount, currency);
        this.currency = currency.toUpperCase();
        this.streamer = streamer;
        this.message = message;
        this.sender = sender;
        this.amount = amount;
        this.image = image;
    }

    @Override
    public EventType getType() {
        return EventType.DONATION;
    }

}
