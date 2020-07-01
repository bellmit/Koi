package co.casterlabs.koi.events;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.google.gson.annotations.SerializedName;

import co.casterlabs.koi.user.User;
import co.casterlabs.koi.util.CurrencyUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
public class DonationEvent extends Event {
    private User streamer;
    private String message;
    private User sender;
    private String image;
    private String currency;
    private String formatted;
    private double amount;
    @SerializedName("usd_equivalent")
    private double usdEquivalent;

    public DonationEvent(String message, User sender, User streamer, String image, String currency, double amount) {
        this.usdEquivalent = new BigDecimal(CurrencyUtil.translateCurrency(amount, currency)).setScale(2, RoundingMode.HALF_UP).doubleValue();
        this.formatted = CurrencyUtil.formatCurrency(amount, currency);
        this.streamer = streamer;
        this.currency = currency;
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
