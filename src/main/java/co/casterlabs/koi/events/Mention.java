package co.casterlabs.koi.events;

import co.casterlabs.koi.user.SerializedUser;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Mention {
    private SerializedUser user;
    private String text;

}
