package iecd.a51597.server.game;

import java.util.UUID;

public sealed interface MoveResult {
    record Accepted() implements MoveResult {}

    record Rejected(String reason) implements MoveResult {}

    record GameOver(UUID winnerId) implements MoveResult {}
}
