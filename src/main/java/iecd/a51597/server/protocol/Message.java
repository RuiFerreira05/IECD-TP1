package iecd.a51597.server.protocol;

import iecd.a51597.server.protocol.types.ActionType;
import iecd.a51597.server.protocol.types.BodyKey;
import iecd.a51597.server.protocol.types.MessageType;

import java.util.Map;
import java.util.UUID;
import java.util.Optional;

public record Message(
        UUID messageId,
        MessageType messageType,
        ActionType actionType,
        Optional<UUID> sessionToken,
        Map<BodyKey, String> Body) {
}
