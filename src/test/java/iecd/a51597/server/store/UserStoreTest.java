package iecd.a51597.server.store;

import iecd.a51597.server.store.entities.User;
import iecd.a51597.server.store.UserStore;
import iecd.a51597.server.store.exceptions.UsernameAlreadyTakenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserStoreTest {

    private UserStore store;

    @BeforeEach
    void setUp() {
        store = new UserStore();
    }

    // ── register ────────────────────────────────────────────────────────────

    @Test
    void register_newUser_returnsUserWithHashedPassword() throws UsernameAlreadyTakenException {
        User user = store.register("alice", "secret");

        assertNotNull(user);
        assertEquals("alice", user.getUsername());
        assertNotNull(user.getUserId());
        // password must be stored as a hash, not plaintext
        assertNotEquals("secret", user.getPasswordHash());
        // hash must be consistent (BCrypt)
        assertTrue(UserStore.checkPassword("secret", user.getPasswordHash()));
    }

    @Test
    void register_duplicateUsername_throwsUsernameAlreadyTaken() throws UsernameAlreadyTakenException {
        store.register("alice", "pass1");
        assertThrows(UsernameAlreadyTakenException.class, () -> store.register("alice", "pass2"));
    }

    @Test
    void register_differentUsers_allRetrievable() throws UsernameAlreadyTakenException {
        store.register("alice", "a");
        store.register("bob",   "b");
        store.register("carol", "c");

        assertEquals(3, store.getAllUsers().size());
    }

    // ── findByCredentials ───────────────────────────────────────────────────

    @Test
    void findByCredentials_correctPassword_returnsUser() throws UsernameAlreadyTakenException {
        store.register("alice", "secret");
        Optional<User> found = store.findByCredentials("alice", "secret");

        assertTrue(found.isPresent());
        assertEquals("alice", found.get().getUsername());
    }

    @Test
    void findByCredentials_wrongPassword_returnsEmpty() throws UsernameAlreadyTakenException {
        store.register("alice", "secret");
        assertTrue(store.findByCredentials("alice", "wrong").isEmpty());
    }

    @Test
    void findByCredentials_unknownUsername_returnsEmpty() {
        assertTrue(store.findByCredentials("nobody", "pass").isEmpty());
    }

    // ── findById / findByUsername ───────────────────────────────────────────

    @Test
    void findById_knownId_returnsUser() throws UsernameAlreadyTakenException {
        User alice = store.register("alice", "pass");
        Optional<User> found = store.findById(alice.getUserId());

        assertTrue(found.isPresent());
        assertSame(alice, found.get());
    }

    @Test
    void findById_unknownId_returnsEmpty() {
        assertTrue(store.findById(UUID.randomUUID()).isEmpty());
    }

    @Test
    void findByUsername_knownUsername_returnsUser() throws UsernameAlreadyTakenException {
        store.register("alice", "pass");
        assertTrue(store.findByUsername("alice").isPresent());
    }

    @Test
    void findByUsername_unknownUsername_returnsEmpty() {
        assertTrue(store.findByUsername("nobody").isEmpty());
    }

    // ── updatePassword ──────────────────────────────────────────────────────

    @Test
    void updatePassword_newPasswordWorksOldDoesNot() throws UsernameAlreadyTakenException {
        User alice = store.register("alice", "oldpass");
        store.updatePassword(alice, "newpass");

        assertTrue(store.findByCredentials("alice", "newpass").isPresent());
        assertTrue(store.findByCredentials("alice", "oldpass").isEmpty());
    }

    @Test
    void updatePassword_storesHash_notPlaintext() throws UsernameAlreadyTakenException {
        User alice = store.register("alice", "pass");
        store.updatePassword(alice, "newpass");

        assertNotEquals("newpass", alice.getPasswordHash());
        assertTrue(UserStore.checkPassword("newpass", alice.getPasswordHash()));
    }

    // ── updateUsername ──────────────────────────────────────────────────────

    @Test
    void updateUsername_renamesUserSuccessfully() throws UsernameAlreadyTakenException {
        User alice = store.register("alice", "pass");
        store.updateUsername(alice, "alice2");

        assertEquals("alice2", alice.getUsername());
        assertTrue(store.findByUsername("alice2").isPresent());
        assertTrue(store.findByUsername("alice").isEmpty());
    }

    @Test
    void updateUsername_takenName_throwsAndLeavesOriginalIntact() throws UsernameAlreadyTakenException {
        User alice = store.register("alice", "pass");
        store.register("bob", "pass");

        assertThrows(UsernameAlreadyTakenException.class, () -> store.updateUsername(alice, "bob"));
        // alice's username must be unchanged
        assertEquals("alice", alice.getUsername());
        assertTrue(store.findByUsername("alice").isPresent());
    }

    // ── updatePhoto ─────────────────────────────────────────────────────────

    @Test
    void updatePhoto_setsNewPhoto() throws UsernameAlreadyTakenException {
        User alice = store.register("alice", "pass");
        store.updatePhoto(alice, "base64data");

        assertEquals("base64data", alice.getPhoto());
    }

    // ── searchByUsername ────────────────────────────────────────────────────

    @Test
    void searchByUsername_partialMatch_caseInsensitive() throws UsernameAlreadyTakenException {
        store.register("Alice", "a");
        store.register("alicia", "b");
        store.register("bob", "c");

        List<User> results = store.searchByUsername("ali");

        assertEquals(2, results.size());
        assertTrue(results.stream().noneMatch(u -> u.getUsername().equals("bob")));
    }

    @Test
    void searchByUsername_noMatch_returnsEmpty() throws UsernameAlreadyTakenException {
        store.register("alice", "a");
        assertTrue(store.searchByUsername("xyz").isEmpty());
    }

    @Test
    void searchByUsername_emptyQuery_matchesAll() throws UsernameAlreadyTakenException {
        store.register("alice", "a");
        store.register("bob", "b");

        assertEquals(2, store.searchByUsername("").size());
    }

    // ── delete ───────────────────────────────────────────────────────────────

    @Test
    void delete_removesFromBothIndexes() throws UsernameAlreadyTakenException {
        User alice = store.register("alice", "pass");
        store.delete(alice);

        assertTrue(store.findById(alice.getUserId()).isEmpty());
        assertTrue(store.findByUsername("alice").isEmpty());
        assertEquals(0, store.getAllUsers().size());
    }

    // ── loadUser ─────────────────────────────────────────────────────────────

    @Test
    void loadUser_makesUserRetrievable() {
        UUID id = UUID.randomUUID();
        User u = new User(id, "loaded", "hash", null);
        store.loadUser(u);

        assertTrue(store.findById(id).isPresent());
        assertTrue(store.findByUsername("loaded").isPresent());
    }

    // ── hash determinism ─────────────────────────────────────────────────────

    @Test
    void hash_differentInputs_produceDifferentOutputs() {
        assertNotEquals(UserStore.hash("password1"), UserStore.hash("password2"));
    }
}
