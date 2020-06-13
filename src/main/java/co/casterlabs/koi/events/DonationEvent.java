package co.casterlabs.koi.events;

import com.google.gson.JsonObject;

import co.casterlabs.koi.user.User;
import co.casterlabs.koi.util.CurrencyUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
public class DonationEvent extends Event {
    private String message;
    private User sender;
    private String image;
    private String currency;
    private String formatted;
    private double amount;
    private double usdEquivalent;

    public DonationEvent(String message, User sender, User streamer, String image, String currency, double amount) {
        this.formatted = CurrencyUtil.formatCurrency(amount, currency);
        this.usdEquivalent = CurrencyUtil.translateCurrency(amount, currency);
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

    @Override
    protected void serialize0(JsonObject json) {
        json.addProperty("usd_equivalent", this.usdEquivalent);
        json.addProperty("formatted", this.formatted);
        json.addProperty("currency", this.currency);
        json.addProperty("message", this.message);
        json.addProperty("amount", this.amount);
        json.addProperty("image", this.image);
        json.add("sender", this.sender.serialize());
    }

}
