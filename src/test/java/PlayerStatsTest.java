import iecd.a51597.server.store.PlayerStats;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerStatsTest {

    private static final UUID OPP_ID = UUID.randomUUID();

    @Test
    void emptyStats_allCountersZero() {
        PlayerStats stats = new PlayerStats();

        assertEquals(0, stats.gamesPlayed());
        assertEquals(0, stats.gamesWon());
        assertEquals(0, stats.gamesLost());
        assertEquals(0.0, stats.totalPlayTimeSecs());
    }

    @Test
    void withMatch_win_incrementsWon() {
        PlayerStats stats = new PlayerStats().withMatch(true, 60.0, OPP_ID, "opp");

        assertEquals(1, stats.gamesPlayed());
        assertEquals(1, stats.gamesWon());
        assertEquals(0, stats.gamesLost());
    }

    @Test
    void withMatch_loss_incrementsLost() {
        PlayerStats stats = new PlayerStats().withMatch(false, 90.0, OPP_ID, "opp");

        assertEquals(1, stats.gamesPlayed());
        assertEquals(0, stats.gamesWon());
        assertEquals(1, stats.gamesLost());
    }

    @Test
    void withMatch_isImmutable_originalUnchanged() {
        PlayerStats original = new PlayerStats();
        PlayerStats updated = original.withMatch(true, 60.0, OPP_ID, "opp");

        assertEquals(0, original.gamesPlayed());
        assertEquals(1, updated.gamesPlayed());
    }

    @Test
    void totalPlayTimeSecs_sumsAllMatches() {
        PlayerStats stats = new PlayerStats()
                .withMatch(true,  100.0, OPP_ID, "a")
                .withMatch(false,  50.0, OPP_ID, "b")
                .withMatch(true,   25.5, OPP_ID, "c");

        assertEquals(175.5, stats.totalPlayTimeSecs(), 0.001);
    }

    @Test
    void gamesPlayed_equalsWonPlusLost() {
        PlayerStats stats = new PlayerStats()
                .withMatch(true,  10.0, OPP_ID, "a")
                .withMatch(false, 20.0, OPP_ID, "b")
                .withMatch(true,  30.0, OPP_ID, "c");

        assertEquals(stats.gamesWon() + stats.gamesLost(), stats.gamesPlayed());
    }

    @Test
    void matchRecord_fieldsAccessible() {
        UUID oppId = UUID.randomUUID();
        PlayerStats stats = new PlayerStats().withMatch(true, 120.5, oppId, "opponent");

        PlayerStats.MatchRecord match = stats.matches().getFirst();
        assertTrue(match.won());
        assertEquals(120.5, match.playtimeSecs(), 0.001);
        assertEquals(oppId, match.opponentId());
        assertEquals("opponent", match.opponentUsername());
    }

    @Test
    void multipleMatches_allPresent() {
        PlayerStats stats = new PlayerStats()
                .withMatch(true, 10.0, OPP_ID, "a")
                .withMatch(false, 20.0, OPP_ID, "b");

        assertEquals(2, stats.matches().size());
    }
}
