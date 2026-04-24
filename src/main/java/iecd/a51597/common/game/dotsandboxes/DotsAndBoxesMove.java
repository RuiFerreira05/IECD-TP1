package iecd.a51597.common.game.dotsandboxes;

import iecd.a51597.common.game.Move;

/**
 * Represents a normalized line drawn between two adjacent dots.
 */
public record DotsAndBoxesMove(int x1, int y1, int x2, int y2) implements Move {
    public DotsAndBoxesMove(int x1, int y1, int x2, int y2) {
        // Normalize coordinates to ensure left-to-right or top-to-bottom
        // This guarantees that a line from (0,0) to (1,0) is equal to (1,0) to (0,0)
        if (x1 > x2 || (x1 == x2 && y1 > y2)) {
            this.x1 = x2;
            this.y1 = y2;
            this.x2 = x1;
            this.y2 = y1;
        } else {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }
    }
}