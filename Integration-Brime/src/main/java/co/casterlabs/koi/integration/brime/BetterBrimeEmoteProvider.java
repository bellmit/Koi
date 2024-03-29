package co.casterlabs.koi.integration.brime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import co.casterlabs.koi.Koi;
import co.casterlabs.koi.client.emotes.ExternalEmote;
import co.casterlabs.koi.client.emotes.ExternalEmoteProvider;
import co.casterlabs.koi.events.ChatEvent;
import co.casterlabs.koi.user.User;
import co.casterlabs.koi.user.UserPlatform;
import co.casterlabs.koi.util.WebUtil;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

public class BetterBrimeEmoteProvider extends ExternalEmoteProvider {

    public BetterBrimeEmoteProvider() {
        super(UserPlatform.BRIME, "BETTER_BRIME", TimeUnit.MINUTES.toMillis(2));
        FastLogger.logStatic("Enabled BetterBrime emotes integration.");
    }

    @Override
    protected Map<String, ExternalEmote> detectEmotes(ChatEvent event, List<ExternalEmote> channelEmotes, List<ExternalEmote> globalEmotes) {
        Map<String, ExternalEmote> emotes = new HashMap<>();

        String message = event.getMessage();

        for (ExternalEmote globalEmote : globalEmotes) {
            if (message.contains(globalEmote.getName())) {
                emotes.put(globalEmote.getName(), globalEmote);
            }
        }

        for (ExternalEmote channelEmote : channelEmotes) {
            if (message.contains(channelEmote.getName())) {
                emotes.put(channelEmote.getName(), channelEmote);
            }
        }

        return emotes;
    }

    @Override
    protected List<ExternalEmote> getGlobalEmotes() {
        List<ExternalEmote> emotes = new ArrayList<>();

        JsonArray response = WebUtil.jsonSendHttpGet("https://api.betterbri.me/v2/brime/global/emotes", null, JsonArray.class);
        for (JsonElement e : response) {
            JsonObject emoteData = e.getAsJsonObject();

            String name = emoteData.get("code").getAsString();
            String imgurLink = String.format("https://i.imgur.com/%s.png", emoteData.get("id").getAsString());

            emotes.add(new ExternalEmote(name, imgurLink, "BetterBrime"));
        }

        return emotes;
    }

    @Override
    protected List<ExternalEmote> getChannelEmotes(User streamer) {
        JsonObject response = WebUtil.jsonSendHttpGet(String.format("https://api.betterbri.me/v2/user/channel?nickname=%s", streamer.getUsername()), null, JsonObject.class);

        if (response.get("emotes").getAsString().startsWith("[")) {
            List<ExternalEmote> emotes = new ArrayList<>();

            JsonArray array = Koi.GSON.fromJson(
                response
                    .get("emotes")
                    .getAsString(),
                JsonArray.class
            );

            for (JsonElement e : array) {
                JsonObject emoteData = e.getAsJsonObject();

                String name = emoteData.get("name").getAsString();
                String imgurLink = String.format("https://i.imgur.com/%s.png", emoteData.get("imgur_id").getAsString());

                emotes.add(new ExternalEmote(name, imgurLink, "BetterBrime"));
            }

            return emotes;
        } else {
            return Collections.emptyList();
        }
    }

}
