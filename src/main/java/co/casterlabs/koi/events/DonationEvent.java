package co.casterlabs.koi.events;

import java.util.List;

import com.google.gson.annotations.SerializedName;

import co.casterlabs.koi.user.SerializedUser;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class DonationEvent extends ChatEvent {
    private List<Donation> donations;

    public DonationEvent(String id, String message, SerializedUser sender, SerializedUser streamer, List<Donation> donations) {
        super(id, message, sender, streamer);

        this.donations = donations;
    }

    @Override
    public EventType getType() {
        return EventType.DONATION;
    }

    @Data
    @AllArgsConstructor
    public static class Donation {
        @SerializedName("animated_image")
        private String animatedImage;
        private String currency;
        private double amount;
        private String image;

    }

}
