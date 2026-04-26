package iecd.a51597.common.game.dotsandboxes;

import iecd.a51597.common.game.Game;
import iecd.a51597.common.game.Move;
import iecd.a51597.common.game.MoveResult;

import java.util.*;

/**
 * Implementation of the Dots and Boxes game logic.
 */
public class DotsAndBoxesGame implements Game {
    private static final int WIDTH = 5;
    private static final int HEIGHT = 5;

    private final UUID gameId;
    private final UUID player1Id;
    private final UUID player2Id;
    private final long startTimeMillis;

    private final Set<DotsAndBoxesMove> drawnLines = new HashSet<>();
    Map<String, UUID> capturedBoxes = new HashMap<>(); // "x,y" -> playerId

    private UUID currentPlayerId;
    private int player1Score = 0;
    private int player2Score = 0;

    private boolean isForcedGameOver = false;
    private UUID forcedWinnerId = null;

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

    /**
     * Gets the ID of the current player whose turn it is.
     * @return current player UUID
     */
    public UUID getCurrentPlayerId() { return currentPlayerId; }

    /**
     * Checks if the game is over.
     * @return true if the game is over, false otherwise
     */
    public boolean isGameOver() { return isForcedGameOver || capturedBoxes.size() == (WIDTH - 1) * (HEIGHT - 1); }

    /**
     * Gets the set of lines already drawn on the board.
     * @return set of drawn lines
     */
    public Set<DotsAndBoxesMove> getDrawnLines() { return drawnLines; }
    
    /**
     * Forces the game to end with a specific winner.
     * @param winnerId ID of the winning player
     */
    public void forceGameOver(UUID winnerId) {
        this.isForcedGameOver = true;
        this.forcedWinnerId = winnerId;
    }

    @Override
    public synchronized MoveResult applyMove(UUID playerId, Move move) {
        if (!playerId.equals(currentPlayerId)) return new MoveResult.Rejected("It is not your turn.");
        if (!(move instanceof DotsAndBoxesMove dbMove)) return new MoveResult.Rejected("Invalid move format.");

        if (!isValidLine(dbMove)) return new MoveResult.Rejected("Invalid line coordinates.");
        if (drawnLines.contains(dbMove)) return new MoveResult.Rejected("Line already exists.");

        drawnLines.add(dbMove);
        boolean capturedAnyBox = checkAndCaptureBoxes(dbMove, playerId);

        if (isGameOver()) {
            if (player1Score > player2Score) {
                return new MoveResult.GameOver(player1Id);
            } else if (player2Score > player1Score) {
                return new MoveResult.GameOver(player2Id);
            } else {
                return new MoveResult.Draw();
            }
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
        if (capturedBoxes.put(x + "," + y, playerId) == null) {
            if (playerId.equals(player1Id)) player1Score++; else player2Score++;
            return true;
        }
        return false;
    }

    /**
     * Gets the owner of a specific box.
     * @param x box x coordinate
     * @param y box y coordinate
     * @return UUID of the player who captured the box, or null if not captured
     */
    public UUID getBoxOwner(int x, int y) {
        return capturedBoxes.get(x + "," + y);
    }

    /**
     * Gets player 1's current score.
     * @return player 1 score
     */
    public int getPlayer1Score() {
        return player1Score;
    }

    /**
     * Gets player 2's current score.
     * @return player 2 score
     */
    public int getPlayer2Score() {
        return player2Score;
    }

    /**
     * Gets the ID of the winner if the game is over.
     * @return winning player UUID, or null if it's a draw or game not over
     */
    public UUID getWinnerId() {
        if (isForcedGameOver) return forcedWinnerId;
        if (player1Score > player2Score) return player1Id;
        if (player2Score > player1Score) return player2Id;
        return null;
    }
}