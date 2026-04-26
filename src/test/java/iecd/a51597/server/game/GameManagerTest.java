package iecd.a51597.server.game;

import iecd.a51597.common.game.Game;
import iecd.a51597.common.game.dotsandboxes.DotsAndBoxesGameFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GameManagerTest {

    private GameManager manager;
    private final UUID p1 = UUID.randomUUID();
    private final UUID p2 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        manager = new GameManager();
        manager.registerFactory(new DotsAndBoxesGameFactory());
    }

    @Test
    void createPendingGame_addsToPendingMap() {
        Game game = manager.createPendingGame(p1, p2);
        assertNotNull(game);
        assertTrue(manager.getPendingGame(game.getGameId()).isPresent());
        assertFalse(manager.getGame(game.getGameId()).isPresent());
    }

    @Test
    void acceptGame_movesToActiveMap() {
        Game pending = manager.createPendingGame(p1, p2);
        manager.acceptGame(pending.getGameId());

        assertFalse(manager.getPendingGame(pending.getGameId()).isPresent());
        assertTrue(manager.getGame(pending.getGameId()).isPresent());
        assertTrue(manager.isInGame(p1));
        assertTrue(manager.isInGame(p2));
    }

    @Test
    void declineGame_removesFromPending() {
        Game pending = manager.createPendingGame(p1, p2);
        manager.declineGame(pending.getGameId());

        assertFalse(manager.getPendingGame(pending.getGameId()).isPresent());
    }

    @Test
    void endGame_removesFromActive() {
        Game pending = manager.createPendingGame(p1, p2);
        manager.acceptGame(pending.getGameId());
        manager.endGame(pending.getGameId());

        assertFalse(manager.getGame(pending.getGameId()).isPresent());
        assertFalse(manager.isInGame(p1));
        assertFalse(manager.isInGame(p2));
    }

    @Test
    void registerFactory_updatesHasFactory() {
        GameManager fresh = new GameManager();
        assertFalse(fresh.hasFactory());
        fresh.registerFactory(new DotsAndBoxesGameFactory());
        assertTrue(fresh.hasFactory());
    }
}
