import iecd.a51597.server.config.ServerConfiguration;
import iecd.a51597.server.persistence.PersistenceManager;
import iecd.a51597.server.store.User;
import iecd.a51597.server.store.UserStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PersistenceManagerTest {

    @TempDir
    Path tempDir;

    private UserStore store;
    private PersistenceManager pm;

    @BeforeEach
    void setUp() {
        ServerConfiguration.USER_STORE = tempDir.resolve("users.xml").toString();
        store = new UserStore();
        pm = new PersistenceManager(store);
    }

    // ── empty store ──────────────────────────────────────────────────────────

    @Test
    void save_emptyStore_noException() {
        assertDoesNotThrow(pm::save);
        assertTrue(Files.exists(Path.of(ServerConfiguration.USER_STORE)));
    }

    @Test
    void load_missingFile_storeRemainsEmpty() {
        pm.load();
        assertEquals(0, store.getAllUsers().size());
    }

    // ── round-trip ────────────────────────────────────────────────────────────

    @Test
    void saveAndLoad_preservesUsernameAndHash() {
        User alice = store.register("alice", "secret");
        pm.save();

        UserStore fresh = new UserStore();
        new PersistenceManager(fresh).load();

        User loaded = fresh.findByUsername("alice").orElseThrow();
        assertEquals(alice.getUserId(),       loaded.getUserId());
        assertEquals(alice.getPasswordHash(), loaded.getPasswordHash());
    }

    @Test
    void saveAndLoad_preservesOptionalFields() {
        User alice = store.register("alice", "pass");
        alice.setNationality("PT");
        alice.setDob(LocalDate.of(1990, 5, 14));
        alice.setPhoto("photo_data");
        store.updatePhoto(alice, "photo_data");
        pm.save();

        UserStore fresh = new UserStore();
        new PersistenceManager(fresh).load();
        User loaded = fresh.findByUsername("alice").orElseThrow();

        assertEquals("PT",                  loaded.getNationality());
        assertEquals(LocalDate.of(1990, 5, 14), loaded.getDob());
        assertEquals("photo_data",           loaded.getPhoto());
    }

    @Test
    void saveAndLoad_nullOptionalFields_noException() {
        // User with all optional fields null (nationality, dob, photo)
        User user = store.register("minimal", "pass");
        assertNull(user.getNationality());
        assertNull(user.getDob());
        assertNull(user.getPhoto());

        pm.save();

        UserStore fresh = new UserStore();
        assertDoesNotThrow(() -> new PersistenceManager(fresh).load());
        assertEquals(1, fresh.getAllUsers().size());
        User loaded = fresh.findByUsername("minimal").orElseThrow();
        assertEquals(user.getUserId(), loaded.getUserId());
        assertNull(loaded.getNationality());
        assertNull(loaded.getDob());
        assertNull(loaded.getPhoto());
    }

    @Test
    void saveAndLoad_preservesMatchHistory() {
        User alice = store.register("alice", "pass");
        UUID oppId = UUID.randomUUID();
        alice.setStats(alice.getStats().withMatch(true, 120.5, oppId, "bob")
                                       .withMatch(false, 80.0, oppId, "bob"));
        pm.save();

        UserStore fresh = new UserStore();
        new PersistenceManager(fresh).load();
        User loaded = fresh.findByUsername("alice").orElseThrow();

        assertEquals(2, loaded.getStats().gamesPlayed());
        assertEquals(1, loaded.getStats().gamesWon());
        assertEquals(1, loaded.getStats().gamesLost());
        assertEquals(200.5, loaded.getStats().totalPlayTimeSecs(), 0.001);
    }

    @Test
    void saveAndLoad_multipleUsers_allPreserved() {
        store.register("alice", "a");
        store.register("bob",   "b");
        store.register("carol", "c");
        pm.save();

        UserStore fresh = new UserStore();
        new PersistenceManager(fresh).load();

        assertEquals(3, fresh.getAllUsers().size());
        assertTrue(fresh.findByUsername("alice").isPresent());
        assertTrue(fresh.findByUsername("bob").isPresent());
        assertTrue(fresh.findByUsername("carol").isPresent());
    }

    @Test
    void loadAfterSave_passwordStillVerifiable() {
        store.register("alice", "mypassword");
        pm.save();

        UserStore fresh = new UserStore();
        new PersistenceManager(fresh).load();

        assertTrue(fresh.findByCredentials("alice", "mypassword").isPresent());
    }

    // ── multiple save cycles ──────────────────────────────────────────────────

    @Test
    void multipleSaves_lastStateWins() {
        store.register("alice", "pass");
        pm.save();

        // Add a second user and save again
        store.register("bob", "pass");
        pm.save();

        UserStore fresh = new UserStore();
        new PersistenceManager(fresh).load();

        assertEquals(2, fresh.getAllUsers().size());
        Set<String> usernames = fresh.getAllUsers().stream()
                .map(User::getUsername)
                .collect(java.util.stream.Collectors.toSet());
        assertEquals(Set.of("alice", "bob"), usernames);
    }
}
