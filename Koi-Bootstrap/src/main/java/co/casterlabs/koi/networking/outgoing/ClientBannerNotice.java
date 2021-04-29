package co.casterlabs.koi.networking.outgoing;

import com.google.gson.JsonElement;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ClientBannerNotice {
    private JsonElement json;

    public JsonElement getAsJson() {
        return this.json;
    }

}
