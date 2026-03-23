package iecd.a51597.server.game;

import iecd.a51597.server.store.User;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GameManager {

    // GameId -> Game Object
    private final Map<UUID, Game> activeGames     = new ConcurrentHashMap<>();
    private final Map<UUID, Game> pendingGames = new ConcurrentHashMap<>();
    // UserId -> GameId
    private final Map<UUID, UUID> playerGameIndex = new ConcurrentHashMap<>(); // userId -> gameId
    private GameFactory factory;
    private MoveCodec codec;

    public void registerFactory(GameFactory factory) {
        this.factory = factory;
        this.codec = factory.getMoveCodec();
    }

    public boolean hasFactory() {
        return factory != null;
    }

    public Game createPendingGame(User player1, User player2) {
        UUID gameId = UUID.randomUUID();
        Game game = factory.createGame(gameId, player1, player2);
        pendingGames.put(gameId, game);
        // playerGameIndex intentionally not touched
        return game;
    }

    public Optional<Game> acceptGame(UUID gameId) {
        Game game = pendingGames.remove(gameId);
        if (game == null) return Optional.empty();
        if (isInGame(game.getPlayer1().getUserId()) || isInGame(game.getPlayer2().getUserId())) {
            return Optional.empty();
        }
        activeGames.put(gameId, game);
        playerGameIndex.put(game.getPlayer1().getUserId(), gameId);
        playerGameIndex.put(game.getPlayer2().getUserId(), gameId);
        return Optional.of(game);
    }

    public void declineGame(UUID gameId) {
        pendingGames.remove(gameId);
    }

    public Optional<Game> getPendingGame(UUID gameId) {
        return Optional.ofNullable(pendingGames.get(gameId));
    }

    public Collection<Game> getAllActiveGames() {
        return activeGames.values();
    }

    public Collection<Game> getAllPendingGames() {
        return pendingGames.values();
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

    public MoveCodec getCodec() {
        return codec;
    }
}
