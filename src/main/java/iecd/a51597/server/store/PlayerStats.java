package iecd.a51597.server.store;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record PlayerStats(List<MatchRecord> matches) {

    public record MatchRecord(boolean won, double playtimeSecs, UUID opponentId, String opponentUsername) {}

    public PlayerStats() {
        this(new ArrayList<>());
    }

    public PlayerStats withMatch(boolean won, double playtimeSecs, UUID opponentId, String opponentUsername) {
        var newMatches = new ArrayList<>(matches);
        newMatches.add(new MatchRecord(won, playtimeSecs, opponentId, opponentUsername));
        return new PlayerStats(newMatches);
    }

    public int gamesPlayed()          { return matches.size(); }
    public int gamesWon()             { return (int) matches.stream().filter(MatchRecord::won).count(); }
    public int gamesLost()            { return (int) matches.stream().filter(m -> !m.won()).count(); }
    public double totalPlayTimeSecs() { return matches.stream().mapToDouble(MatchRecord::playtimeSecs).sum(); }
}