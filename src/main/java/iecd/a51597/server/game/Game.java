package iecd.a51597.server.game;

import iecd.a51597.server.store.User;

import java.util.UUID;

/**
 * Abstraction of a two-player game instance managed by the server.
 */
public interface Game {
    /**
     * @return unique identifier for this game instance
     */
    UUID getGameId();

    /**
     * @return first player (inviter/origin player)
     */
    User getPlayer1();

    /**
     * @return second player (invitee)
     */
    User getPlayer2();

    /**
     * Applies a move for one player.
     *
     * @param player player issuing the move
     * @param move move to apply
     * @return domain result indicating acceptance, rejection, or game completion
     */
    MoveResult applyMove(User player, Move move);
}