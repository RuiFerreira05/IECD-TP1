package iecd.a51597.server.protocol;

import org.w3c.dom.Element;

import java.util.UUID;

public sealed interface MessageBody {
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

    record GameOver(UUID gameId, UUID winnerId, String winnerUsername) implements MessageBody {}

    record Unknown() implements MessageBody {
    }
}
