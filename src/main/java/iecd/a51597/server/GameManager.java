package iecd.a51597.server;

import iecd.a51597.server.game.Game;
import iecd.a51597.server.game.GameFactory;
import iecd.a51597.server.store.User;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GameManager {

    // GameId -> Game Object
    private final Map<UUID, Game> activeGames     = new ConcurrentHashMap<>();
    // UserId -> GameId
    private final Map<UUID, UUID> playerGameIndex = new ConcurrentHashMap<>(); // userId -> gameId
    private GameFactory factory;

    public void registerFactory(GameFactory factory) {
        this.factory = factory;
    }

    public boolean hasFactory() {
        return factory != null;
    }

    public Game createGame(User player1, User player2) {
        UUID gameId = UUID.randomUUID();
        Game game = factory.createGame(gameId, player1, player2);
        activeGames.put(gameId, game);
        playerGameIndex.put(player1.getUserId(), gameId);
        playerGameIndex.put(player2.getUserId(), gameId);
        return game;
    }

    public Optional<Game> getGame(UUID gameId) {
        return Optional.ofNullable(activeGames.get(gameId));
    }

    public Optional<UUID> getActiveGameId(UUID userId) {
        return Optional.ofNullable(playerGameIndex.get(userId));
    }

    public boolean isInGame(UUID userId) {
        return playerGameIndex.containsKey(userId);
    }

    public void endGame(UUID gameId) {
        Game game = activeGames.remove(gameId);
        if (game != null) {
            playerGameIndex.remove(game.getPlayer1().getUserId());
            playerGameIndex.remove(game.getPlayer2().getUserId());
        }
    }
}
