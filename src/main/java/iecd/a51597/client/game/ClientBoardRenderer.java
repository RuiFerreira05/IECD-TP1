package iecd.a51597.client.game;

import iecd.a51597.common.game.dotsandboxes.DotsAndBoxesGame;
import iecd.a51597.common.game.dotsandboxes.DotsAndBoxesMove;

import java.util.Set;

public class ClientBoardRenderer {

    private static final int WIDTH = 5;
    private static final int HEIGHT = 5;

    public static void printBoard(GameController controller) {
        DotsAndBoxesGame state = controller.getState();
        Set<DotsAndBoxesMove> lines = state.getDrawnLines();

        // Determine who is P1 and P2 for the scoreboard
        boolean amIPlayer1 = state.getPlayer1Id().equals(controller.getMyUserId());
        int myScore = amIPlayer1 ? state.getPlayer1Score() : state.getPlayer2Score();
        int opponentScore = amIPlayer1 ? state.getPlayer2Score() : state.getPlayer1Score();

        System.out.println("\n=================================");
        System.out.printf(" %s: %d  |  %s: %d%n", controller.getMyUsername(), myScore, controller.getOpponentUsername(), opponentScore);
        System.out.println("=================================");
        System.out.println("    0   1   2   3   4  (X)");

        for (int y = 0; y < HEIGHT; y++) {
            // Print horizontal lines and dots
            System.out.print(" " + y + "  ");
            for (int x = 0; x < WIDTH; x++) {
                System.out.print("*");
                if (x < WIDTH - 1) {
                    boolean hasLine = lines.contains(new DotsAndBoxesMove(x, y, x + 1, y));
                    System.out.print(hasLine ? "---" : "   ");
                }
            }
            System.out.println();

            // Print vertical lines
            if (y < HEIGHT - 1) {
                System.out.print("    ");
                for (int x = 0; x < WIDTH; x++) {
                    boolean hasLine = lines.contains(new DotsAndBoxesMove(x, y, x, y + 1));
                    System.out.print(hasLine ? "|   " : "    ");
                }
                System.out.println();
            }
        }
        System.out.println("(Y)\n");
    }
}