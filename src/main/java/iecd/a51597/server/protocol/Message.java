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

    private sealed interface MessageBody {
        record Register(String username, String password) implements MessageBody {
        }

        record Login(String username, String password) implements MessageBody {
        }

        record Logout() implements MessageBody {
        }

        record UpdateProfile(String username, String password, String photo) implements MessageBody {
        }

        record SearchUsers(String query) implements MessageBody {
        }

        record GameInvite(UUID targetUserId) implements MessageBody {
        }

        record GameInviteResponse(UUID gameId, boolean accept) implements MessageBody {
        }

        record GameMove(UUID gameId, Element move) implements MessageBody {
        }

        record Unknown() implements MessageBody {
        }
    }
}