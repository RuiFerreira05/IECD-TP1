package iecd.a51597.server.protocol;

import iecd.a51597.server.protocol.types.ActionType;
import iecd.a51597.server.protocol.types.BodyKey;
import iecd.a51597.server.protocol.types.MessageType;
import org.w3c.dom.Element;

import java.util.Map;
import java.util.UUID;

public record Message(
        UUID messageId,
        MessageType messageType,
        String version,
        ActionType actionType,
        UUID sessionToken,
        MessageBody body) {
}