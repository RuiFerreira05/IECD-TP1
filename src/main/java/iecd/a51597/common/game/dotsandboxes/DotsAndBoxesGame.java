package iecd.a51597.common.game.dotsandboxes;

import iecd.a51597.common.game.Game;
import iecd.a51597.common.game.Move;
import iecd.a51597.common.game.MoveResult;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DotsAndBoxesGame implements Game {
    private static final int WIDTH = 5;
    private static final int HEIGHT = 5;

    private final UUID gameId;
    private final UUID player1Id;
    private final UUID player2Id;
    private final long startTimeMillis;

    private final Set<DotsAndBoxesMove> drawnLines = new HashSet<>();
    private final Set<String> capturedBoxes = new HashSet<>();

    private UUID currentPlayerId;
    private int player1Score = 0;
    private int player2Score = 0;

    public DotsAndBoxesGame(UUID gameId, UUID player1Id, UUID player2Id) {
        this.gameId = gameId;
        this.player1Id = player1Id;
        this.player2Id = player2Id;
        this.currentPlayerId = player1Id;
        this.startTimeMillis = System.currentTimeMillis();
    }

    @Override public UUID getGameId() { return gameId; }
    @Override public UUID getPlayer1Id() { return player1Id; }
    @Override public UUID getPlayer2Id() { return player2Id; }
    @Override public long getStartTimeMillis() { return startTimeMillis; }

    // Client-side UI helper methods
    public UUID getCurrentPlayerId() { return currentPlayerId; }
    public boolean isGameOver() { return capturedBoxes.size() == (WIDTH - 1) * (HEIGHT - 1); }
    public Set<DotsAndBoxesMove> getDrawnLines() { return drawnLines; }

    @Override
    public synchronized MoveResult applyMove(UUID playerId, Move move) {
        if (!playerId.equals(currentPlayerId)) return new MoveResult.Rejected("It is not your turn.");
        if (!(move instanceof DotsAndBoxesMove dbMove)) return new MoveResult.Rejected("Invalid move format.");

        if (!isValidLine(dbMove)) return new MoveResult.Rejected("Invalid line coordinates.");
        if (drawnLines.contains(dbMove)) return new MoveResult.Rejected("Line already exists.");

        drawnLines.add(dbMove);
        boolean capturedAnyBox = checkAndCaptureBoxes(dbMove, playerId);

        if (isGameOver()) {
            UUID winnerId = null;
            if (player1Score > player2Score) winnerId = player1Id;
            else if (player2Score > player1Score) winnerId = player2Id;
            return new MoveResult.GameOver(winnerId);
        }

        if (!capturedAnyBox) {
            currentPlayerId = currentPlayerId.equals(player1Id) ? player2Id : player1Id;
        }

        return new MoveResult.Accepted();
    }

    private boolean isValidLine(DotsAndBoxesMove m) {
        if (m.x1() < 0 || m.x1() >= WIDTH || m.y1() < 0 || m.y1() >= HEIGHT ||
                m.x2() < 0 || m.x2() >= WIDTH || m.y2() < 0 || m.y2() >= HEIGHT) return false;
        int dx = Math.abs(m.x1() - m.x2());
        int dy = Math.abs(m.y1() - m.y2());
        return (dx == 1 && dy == 0) || (dx == 0 && dy == 1);
    }

    private boolean checkAndCaptureBoxes(DotsAndBoxesMove move, UUID playerId) {
        boolean captured = false;
        int x1 = move.x1(), y1 = move.y1();

        if (y1 == move.y2()) {
            if (y1 > 0 && checkBoxClosed(x1, y1 - 1)) captured |= captureBox(x1, y1 - 1, playerId);
            if (y1 < HEIGHT - 1 && checkBoxClosed(x1, y1)) captured |= captureBox(x1, y1, playerId);
        } else {
            if (x1 > 0 && checkBoxClosed(x1 - 1, y1)) captured |= captureBox(x1 - 1, y1, playerId);
            if (x1 < WIDTH - 1 && checkBoxClosed(x1, y1)) captured |= captureBox(x1, y1, playerId);
        }
        return captured;
    }

    private boolean checkBoxClosed(int x, int y) {
        return drawnLines.contains(new DotsAndBoxesMove(x, y, x + 1, y)) &&
                drawnLines.contains(new DotsAndBoxesMove(x, y + 1, x + 1, y + 1)) &&
                drawnLines.contains(new DotsAndBoxesMove(x, y, x, y + 1)) &&
                drawnLines.contains(new DotsAndBoxesMove(x + 1, y, x + 1, y + 1));
    }

    private boolean captureBox(int x, int y, UUID playerId) {
        if (capturedBoxes.add(x + "," + y)) {
            if (playerId.equals(player1Id)) player1Score++; else player2Score++;
            return true;
        }
        return false;
    }

    public int getPlayer1Score() {
        return player1Score;
    }

    public int getPlayer2Score() {
        return player2Score;
    }

    public UUID getWinnerId() {
        if (player1Score > player2Score) return player1Id;
        if (player2Score > player1Score) return player2Id;
        return null;
    }
}