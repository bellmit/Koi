package co.casterlabs.koi.networking.incoming;

import lombok.Getter;
import xyz.e3ndr.eventapi.events.AbstractEvent;

@Getter
public class ChatRequest extends AbstractEvent<IncomingMessageType> {

    public ChatRequest() {
        super(IncomingMessageType.CHAT);
    }

    private Chatter chatter = Chatter.CLIENT;
    private String message;
    private String nonce;

    public static enum Chatter {
        CLIENT,
        PUPPET;

    }

}
