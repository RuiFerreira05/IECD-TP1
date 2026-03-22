package iecd.a51597.server.game;

import iecd.a51597.server.store.User;

import java.util.UUID;

public interface GameFactory {
    Game createGame(UUID gameId, User player1, User player2);
}