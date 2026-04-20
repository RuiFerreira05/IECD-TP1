package iecd.a51597.server.game;

import iecd.a51597.server.store.User;

import java.util.UUID;

/**
 * Factory used to plug a concrete game implementation into the server.
 */
public interface GameFactory {
    /**
     * Creates a game instance for two players.
     *
     * @param gameId externally generated game id
     * @param player1 first player
     * @param player2 second player
     * @return created game instance
     */
    Game createGame(UUID gameId, User player1, User player2);

    /**
     * @return codec used to serialize and deserialize game moves
     */
    MoveCodec getMoveCodec();
}