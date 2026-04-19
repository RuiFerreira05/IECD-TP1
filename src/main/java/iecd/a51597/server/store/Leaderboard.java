package iecd.a51597.server.store;

import java.util.Comparator;
import java.util.List;

public class Leaderboard {

    public record Entry(String username, int gamesWon, int gamesLost, double totalPlayTimeSecs) {}

    private final UserStore userStore;

    public Leaderboard(UserStore userStore) {
        this.userStore = userStore;
    }

    public List<Entry> getTopPlayers(int limit) {
        return userStore.getAllUsers().stream()
                .map(u -> new Entry(
                        u.getUsername(),
                        u.getStats().gamesWon(),
                        u.getStats().gamesLost(),
                        u.getStats().totalPlayTimeSecs()
                ))
                .sorted(Comparator
                        .comparingInt(Entry::gamesWon).reversed()
                        .thenComparingDouble(Entry::totalPlayTimeSecs))
                .limit(limit)
                .toList();
    }

    public List<Entry> getAll() {
        return getTopPlayers(Integer.MAX_VALUE);
    }
}