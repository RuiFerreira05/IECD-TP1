package iecd.a51597.server.game;

import iecd.a51597.server.store.entities.User;

/**
 * Outcome of applying a move in a game instance.
 */
public sealed interface MoveResult {
    /**
     * Move accepted and game continues.
     */
    record Accepted() implements MoveResult {}

    /**
     * Move rejected by game rules.
     *
     * @param reason rejection reason suitable for client feedback
     */
    record Rejected(String reason) implements MoveResult {}

    /**
     * Move ended the game with a winner.
     *
     * @param winner winning player
     */
    record GameOver(User winner) implements MoveResult {}
}
