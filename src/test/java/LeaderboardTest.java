import iecd.a51597.server.store.Leaderboard;
import iecd.a51597.common.store.PlayerStats;
import iecd.a51597.server.store.entities.User;
import iecd.a51597.server.store.UserStore;
import iecd.a51597.server.store.exceptions.UsernameAlreadyTakenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeaderboardTest {

    private UserStore store;
    private Leaderboard board;

    @BeforeEach
    void setUp() {
        store = new UserStore();
        board = new Leaderboard(store);
    }

    private User registerWith(String name, int wins, int losses, double playtime)
            throws UsernameAlreadyTakenException {
        User u = store.register(name, "pass");
        PlayerStats stats = u.getStats();
        UUID opp = UUID.randomUUID();
        for (int i = 0; i < wins;   i++) stats = stats.withMatch(true,  playtime, opp, "opp");
        for (int i = 0; i < losses; i++) stats = stats.withMatch(false, playtime, opp, "opp");
        u.setStats(stats);
        return u;
    }

    @Test
    void getAll_emptyStore_returnsEmptyList() {
        assertTrue(board.getAll().isEmpty());
    }

    @Test
    void getTopPlayers_orderedByWinsDescending() throws UsernameAlreadyTakenException {
        registerWith("alice", 5, 1, 60.0);
        registerWith("bob",   3, 2, 60.0);
        registerWith("carol", 8, 0, 60.0);

        List<Leaderboard.Entry> top = board.getTopPlayers(3);

        assertEquals("carol", top.get(0).username());
        assertEquals("alice", top.get(1).username());
        assertEquals("bob",   top.get(2).username());
    }

    @Test
    void getTopPlayers_tieOnWins_orderedByPlaytimeAscending() throws UsernameAlreadyTakenException {
        // Both have 3 wins; alice played faster (less total playtime)
        registerWith("alice", 3, 0, 30.0);   // total = 90 s
        registerWith("bob",   3, 0, 60.0);   // total = 180 s

        List<Leaderboard.Entry> top = board.getTopPlayers(2);

        assertEquals("alice", top.get(0).username());
        assertEquals("bob",   top.get(1).username());
    }

    @Test
    void getTopPlayers_limitRespected() throws UsernameAlreadyTakenException {
        registerWith("a", 1, 0, 10.0);
        registerWith("b", 2, 0, 10.0);
        registerWith("c", 3, 0, 10.0);

        assertEquals(2, board.getTopPlayers(2).size());
    }

    @Test
    void getTopPlayers_limitLargerThanUsers_returnsAll() throws UsernameAlreadyTakenException {
        registerWith("a", 1, 0, 10.0);
        registerWith("b", 2, 0, 10.0);

        assertEquals(2, board.getTopPlayers(100).size());
    }

    @Test
    void entry_containsCorrectStats() throws UsernameAlreadyTakenException {
        registerWith("alice", 4, 2, 50.0);

        Leaderboard.Entry entry = board.getAll().stream()
                .filter(e -> e.username().equals("alice"))
                .findFirst().orElseThrow();

        assertEquals(4,     entry.gamesWon());
        assertEquals(2,     entry.gamesLost());
        assertEquals(300.0, entry.totalPlayTimeSecs(), 0.001);  // 6 games × 50 s
    }
}
