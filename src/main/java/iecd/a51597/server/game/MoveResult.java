package iecd.a51597.server.game;

import iecd.a51597.server.store.User;

public sealed interface MoveResult {
    record Accepted() implements MoveResult {}

    record Rejected(String reason) implements MoveResult {}

    record GameOver(User winner) implements MoveResult {}
}
