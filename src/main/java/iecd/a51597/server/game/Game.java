package iecd.a51597.server.game;

import java.util.UUID;

public interface Game {
    UUID getGameId();
    UUID getPlayer1Id();
    UUID getPlayer2Id();
    MoveResult applyMove(UUID playerId, String movePayload);
}