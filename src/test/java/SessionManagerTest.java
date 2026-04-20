import iecd.a51597.server.config.ServerConfiguration;
import iecd.a51597.server.network.Connection;
import iecd.a51597.server.session.Session;
import iecd.a51597.server.session.SessionManager;
import iecd.a51597.server.store.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class SessionManagerTest {

    private SessionManager manager;
    private Connection conn;
    private User alice;

    @BeforeEach
    void setUp() {
        // Ensure a reasonably long timeout so tests don't spuriously expire
        ServerConfiguration.SESSION_TIMEOUT_SECONDS = 1800;

        manager = new SessionManager();
        conn = mock(Connection.class);
        alice = new User(UUID.randomUUID(), "alice", "hash", null);
    }

    // ── createSession ────────────────────────────────────────────────────────

    @Test
    void createSession_returnsSessionBoundToUser() {
        Session session = manager.createSession(alice, conn);

        assertNotNull(session.getToken());
        assertSame(alice, session.getUser());
        assertSame(conn, session.getConnection());
        assertEquals(alice.getUserId(), session.getUserId());
    }

    @Test
    void createSession_invalidatesPreviousSession() {
        Session first = manager.createSession(alice, conn);
        UUID firstToken = first.getToken();

        manager.createSession(alice, conn);   // login again

        // first token must no longer be valid
        assertTrue(manager.validate(firstToken).isEmpty());
        assertEquals(1, manager.activeSessionCount());
    }

    // ── validate ─────────────────────────────────────────────────────────────

    @Test
    void validate_validToken_returnsSession() {
        Session created = manager.createSession(alice, conn);
        Optional<Session> found = manager.validate(created.getToken());

        assertTrue(found.isPresent());
        assertSame(created, found.get());
    }

    @Test
    void validate_unknownToken_returnsEmpty() {
        assertTrue(manager.validate(UUID.randomUUID()).isEmpty());
    }

    @Test
    void validate_nullToken_returnsEmpty() {
        assertTrue(manager.validate(null).isEmpty());
    }

    @Test
    void validate_expiredSession_returnsEmpty() {
        ServerConfiguration.SESSION_TIMEOUT_SECONDS = 0;   // expire immediately
        Session session = manager.createSession(alice, conn);

        assertTrue(manager.validate(session.getToken()).isEmpty());
        assertEquals(0, manager.activeSessionCount());
        assertTrue(manager.getSessionByUserId(alice.getUserId()).isEmpty());
    }

    @Test
    void validate_refreshesLastActivity() throws InterruptedException {
        ServerConfiguration.SESSION_TIMEOUT_SECONDS = 1800;
        Session session = manager.createSession(alice, conn);

        var before = session.getLastActivity();
        Thread.sleep(10);
        Optional<Session> validated = manager.validate(session.getToken());
        var after = session.getLastActivity();

        assertTrue(validated.isPresent());
        assertTrue(after.isAfter(before),
                "Expected refresh() to move lastActivity forward; before=%s after=%s".formatted(before, after));
    }

    // ── invalidate ───────────────────────────────────────────────────────────

    @Test
    void invalidate_removesSession() {
        Session session = manager.createSession(alice, conn);
        manager.invalidate(session.getToken());

        assertTrue(manager.validate(session.getToken()).isEmpty());
        assertEquals(0, manager.activeSessionCount());
    }

    @Test
    void invalidate_unknownToken_noException() {
        assertDoesNotThrow(() -> manager.invalidate(UUID.randomUUID()));
    }

    // ── invalidateByUserId ───────────────────────────────────────────────────

    @Test
    void invalidateByUserId_removesCorrectSession() {
        Session session = manager.createSession(alice, conn);
        manager.invalidateByUserId(alice.getUserId());

        assertTrue(manager.validate(session.getToken()).isEmpty());
    }

    @Test
    void invalidateByUserId_unknownUser_noException() {
        assertDoesNotThrow(() -> manager.invalidateByUserId(UUID.randomUUID()));
    }

    // ── invalidateByConnection ───────────────────────────────────────────────

    @Test
    void invalidateByConnection_removesSession() {
        Session session = manager.createSession(alice, conn);
        manager.invalidateByConnection(conn);

        assertTrue(manager.validate(session.getToken()).isEmpty());
    }

    @Test
    void invalidateByConnection_differentConnection_notAffected() {
        Connection other = mock(Connection.class);
        Session session = manager.createSession(alice, conn);
        manager.invalidateByConnection(other);   // unrelated connection

        assertTrue(manager.validate(session.getToken()).isPresent());
    }

    // ── queries ───────────────────────────────────────────────────────────────

    @Test
    void getSessionByUserId_returnsCorrectSession() {
        Session session = manager.createSession(alice, conn);
        Optional<Session> found = manager.getSessionByUserId(alice.getUserId());

        assertTrue(found.isPresent());
        assertSame(session, found.get());
    }

    @Test
    void getAllSessions_returnsAllActive() {
        User bob = new User(UUID.randomUUID(), "bob", "hash", null);
        manager.createSession(alice, conn);
        manager.createSession(bob, mock(Connection.class));

        assertEquals(2, manager.getAllSessions().size());
    }

    @Test
    void activeSessionCount_tracksCorrectly() {
        assertEquals(0, manager.activeSessionCount());

        manager.createSession(alice, conn);
        assertEquals(1, manager.activeSessionCount());

        User bob = new User(UUID.randomUUID(), "bob", "hash", null);
        manager.createSession(bob, mock(Connection.class));
        assertEquals(2, manager.activeSessionCount());

        manager.invalidateByUserId(alice.getUserId());
        assertEquals(1, manager.activeSessionCount());
    }
}
