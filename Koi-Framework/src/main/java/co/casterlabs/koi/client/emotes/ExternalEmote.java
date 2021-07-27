package co.casterlabs.koi.client.emotes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor
public class ExternalEmote {
    private String name;
    private String imageLink;
    private String from;

}
