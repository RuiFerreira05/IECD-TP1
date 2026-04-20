package iecd.a51597.common.protocol;

import java.util.UUID;

/**
 * Marker interface for all supported protocol payload bodies.
 */
public sealed interface MessageBody {
    /**
     * Registration payload.
     *
     * @param username desired username
     * @param password plaintext password to be hashed by the server
     */
    record Register(String username, String password) implements MessageBody {
    }

    /**
     * Login payload.
     *
     * @param username existing username
     * @param password plaintext password
     */
    record Login(String username, String password) implements MessageBody {
    }

    /**
     * Logout payload.
     */
    record Logout() implements MessageBody {
    }

    /**
     * Profile update payload.
     *
     * @param username optional new username
     * @param password optional new plaintext password
     * @param photo optional profile photo reference
     */
    record UpdateProfile(String username, String password, String photo) implements MessageBody {
    }

    /**
     * User search payload.
     *
     * @param query search text applied to usernames
     */
    record SearchUsers(String query) implements MessageBody {
    }

    /**
     * Game invitation payload.
     *
     * @param targetUserId invited user's identifier
     */
    record GameInvite(UUID targetUserId) implements MessageBody {
    }

    /**
     * Invitation response payload.
     *
     * @param gameId pending game identifier
     * @param accept whether the invite is accepted
     */
    record GameInviteResponse(UUID gameId, boolean accept) implements MessageBody {
    }

    /**
     * Game move payload.
     *
     * @param gameId active game identifier
     * @param rawMove serialized move payload understood by the configured game codec
     */
    record GameMove(UUID gameId, String rawMove) implements MessageBody {
    }

    /**
     * Server-initiated game over payload.
     *
     * @param gameId game identifier
     * @param winnerId winning player's identifier
     * @param winnerUsername winning player's username
     */
    record GameOver(UUID gameId, UUID winnerId, String winnerUsername) implements MessageBody {}

    /**
     * Fallback payload for unknown/unmapped actions.
     */
    record Unknown() implements MessageBody {
    }
}
