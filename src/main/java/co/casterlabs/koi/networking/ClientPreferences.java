package co.casterlabs.koi.networking;

import lombok.ToString;

@ToString
public class ClientPreferences {
    private String currency = "USD";

    // Some internal safety, incase a client tries to pull a fast one.
    public String getCurrency() {
        return ((this.currency == null) || (this.currency.isEmpty())) ? "USD" : this.currency;
    }

}
