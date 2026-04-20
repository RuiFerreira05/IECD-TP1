package iecd.a51597.common.protocol;

import iecd.a51597.common.protocol.types.ActionType;
import iecd.a51597.common.protocol.types.MessageType;

import java.util.UUID;

public record Message(
        UUID messageId,
        MessageType messageType,
        String version,
        ActionType actionType,
        UUID sessionToken,
        MessageBody body) {
}