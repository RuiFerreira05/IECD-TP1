import iecd.a51597.common.protocol.exceptions.MalformedMessageException;
import iecd.a51597.server.game.*;
import iecd.a51597.common.store.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GameManagerTest {

    private GameManager gm;
    private User player1;
    private User player2;

    // ── Minimal test-double implementations ──────────────────────────────────

    record DummyMove(String value) implements Move {}

    static class DummyCodec implements MoveCodec {
        @Override public String serialize(Move m) { return ((DummyMove) m).value(); }
        @Override public Move deserialize(String s) throws MalformedMessageException {
            if ("invalid".equals(s)) throw new MalformedMessageException("bad");
            return new DummyMove(s);
        }
    }

    static class DummyGame implements Game {
        private final UUID id;
        private final User p1;
        private final User p2;

        DummyGame(UUID id, User p1, User p2) { this.id = id; this.p1 = p1; this.p2 = p2; }

        @Override public UUID getGameId()  { return id; }
        @Override public User getPlayer1() { return p1; }
        @Override public User getPlayer2() { return p2; }
        @Override public MoveResult applyMove(User player, Move move) {
            return new MoveResult.Accepted();
        }
    }

    static class DummyFactory implements GameFactory {
        private final DummyCodec codec = new DummyCodec();
        @Override public Game createGame(UUID gameId, User p1, User p2) {
            return new DummyGame(gameId, p1, p2);
        }
        @Override public MoveCodec getMoveCodec() { return codec; }
    }

    @BeforeEach
    void setUp() {
        gm = new GameManager();
        player1 = new User(UUID.randomUUID(), "alice", "h", null);
        player2 = new User(UUID.randomUUID(), "bob",   "h", null);
    }

    // ── factory ──────────────────────────────────────────────────────────────

    @Test
    void hasFactory_falseBeforeRegistration() {
        assertFalse(gm.hasFactory());
    }

    @Test
    void hasFactory_trueAfterRegistration() {
        gm.registerFactory(new DummyFactory());
        assertTrue(gm.hasFactory());
    }

    @Test
    void getCodec_returnsFactoryCodec() {
        gm.registerFactory(new DummyFactory());
        assertNotNull(gm.getCodec());
    }

    // ── createPendingGame ─────────────────────────────────────────────────────

    @Test
    void createPendingGame_appearsinPendingNotActive() {
        gm.registerFactory(new DummyFactory());
        Game g = gm.createPendingGame(player1, player2);

        assertTrue(gm.getPendingGame(g.getGameId()).isPresent());
        assertTrue(gm.getGame(g.getGameId()).isEmpty());
    }

    @Test
    void createPendingGame_doesNotMarkPlayersAsInGame() {
        gm.registerFactory(new DummyFactory());
        gm.createPendingGame(player1, player2);

        assertFalse(gm.isInGame(player1.getUserId()));
        assertFalse(gm.isInGame(player2.getUserId()));
    }

    // ── acceptGame ────────────────────────────────────────────────────────────

    @Test
    void acceptGame_movesToActive() {
        gm.registerFactory(new DummyFactory());
        Game pending = gm.createPendingGame(player1, player2);

        Optional<Game> accepted = gm.acceptGame(pending.getGameId());

        assertTrue(accepted.isPresent());
        assertTrue(gm.getGame(pending.getGameId()).isPresent());
        assertTrue(gm.getPendingGame(pending.getGameId()).isEmpty());
    }

    @Test
    void acceptGame_marksPlayersAsInGame() {
        gm.registerFactory(new DummyFactory());
        Game pending = gm.createPendingGame(player1, player2);
        gm.acceptGame(pending.getGameId());

        assertTrue(gm.isInGame(player1.getUserId()));
        assertTrue(gm.isInGame(player2.getUserId()));
    }

    @Test
    void acceptGame_unknownId_returnsEmpty() {
        assertTrue(gm.acceptGame(UUID.randomUUID()).isEmpty());
    }

    @Test
    void getActiveGameId_returnsCorrectId() {
        gm.registerFactory(new DummyFactory());
        Game pending = gm.createPendingGame(player1, player2);
        gm.acceptGame(pending.getGameId());

        assertEquals(pending.getGameId(),
                gm.getActiveGameId(player1.getUserId()).orElseThrow());
        assertEquals(pending.getGameId(),
                gm.getActiveGameId(player2.getUserId()).orElseThrow());
    }

    // ── declineGame ───────────────────────────────────────────────────────────

    @Test
    void declineGame_removesFromPending() {
        gm.registerFactory(new DummyFactory());
        Game pending = gm.createPendingGame(player1, player2);
        gm.declineGame(pending.getGameId());

        assertTrue(gm.getPendingGame(pending.getGameId()).isEmpty());
    }

    @Test
    void declineGame_unknownId_noException() {
        assertDoesNotThrow(() -> gm.declineGame(UUID.randomUUID()));
    }

    // ── endGame ───────────────────────────────────────────────────────────────

    @Test
    void endGame_removesFromActiveAndClearsPlayerIndex() {
        gm.registerFactory(new DummyFactory());
        Game pending = gm.createPendingGame(player1, player2);
        gm.acceptGame(pending.getGameId());

        gm.endGame(pending.getGameId());

        assertTrue(gm.getGame(pending.getGameId()).isEmpty());
        assertFalse(gm.isInGame(player1.getUserId()));
        assertFalse(gm.isInGame(player2.getUserId()));
    }

    @Test
    void endGame_unknownId_noException() {
        assertDoesNotThrow(() -> gm.endGame(UUID.randomUUID()));
    }

    // ── collection views ─────────────────────────────────────────────────────

    @Test
    void getAllPendingGames_returnsAllPending() {
        gm.registerFactory(new DummyFactory());
        User p3 = new User(UUID.randomUUID(), "carol", "h", null);
        User p4 = new User(UUID.randomUUID(), "dave",  "h", null);

        gm.createPendingGame(player1, player2);
        gm.createPendingGame(p3, p4);

        assertEquals(2, gm.getAllPendingGames().size());
    }

    @Test
    void getAllActiveGames_returnsOnlyAccepted() {
        gm.registerFactory(new DummyFactory());
        Game g1 = gm.createPendingGame(player1, player2);
        User p3 = new User(UUID.randomUUID(), "carol", "h", null);
        User p4 = new User(UUID.randomUUID(), "dave",  "h", null);
        gm.createPendingGame(p3, p4);   // stays pending

        gm.acceptGame(g1.getGameId());

        assertEquals(1, gm.getAllActiveGames().size());
    }

    // ── codec round-trip ──────────────────────────────────────────────────────

    @Test
    void codec_serializeDeserialize_roundTrip() throws MalformedMessageException {
        gm.registerFactory(new DummyFactory());
        DummyMove original = new DummyMove("hello");
        String serialized = gm.getCodec().serialize(original);
        Move restored = gm.getCodec().deserialize(serialized);

        assertEquals(original.value(), ((DummyMove) restored).value());
    }

    @Test
    void codec_invalidPayload_throwsMalformedMessage() {
        gm.registerFactory(new DummyFactory());
        assertThrows(MalformedMessageException.class,
                () -> gm.getCodec().deserialize("invalid"));
    }
}
