package iecd.a51597.client.cli.screens.impl;

import iecd.a51597.client.Client;
import iecd.a51597.client.cli.StateMachine;
import iecd.a51597.client.cli.screens.Screen;
import iecd.a51597.client.game.ClientBoardRenderer;
import iecd.a51597.client.game.GameController;
import iecd.a51597.common.game.dotsandboxes.DotsAndBoxesMove;
import iecd.a51597.common.game.dotsandboxes.DotsAndBoxesMoveCodec;
import iecd.a51597.common.protocol.Message;
import iecd.a51597.common.protocol.MessageBody;
import iecd.a51597.common.protocol.exceptions.MalformedMessageException;
import iecd.a51597.common.protocol.types.ActionType;

import java.util.UUID;

public class GameScreen extends Screen {

    private final GameController controller;

    public GameScreen(StateMachine sm, Client client, GameController controller) {
        super(sm, client);
        this.controller = controller;
        this.prompt = "Game> ";
    }

    @Override
    public void onEnter() {
        logger.info("Entering Game Screen for game: {}", controller.getState().getGameId());
    }

    @Override
    public void display() {
        // Don't prompt for moves if the game is over
        if (controller.getState().isGameOver()) {
            return;
        }

        ClientBoardRenderer.printBoard(controller);

        if (controller.isMyTurn()) {
            System.out.print("\nYour turn! Enter coordinates (x1 y1 x2 y2): ");
        } else {
            System.out.println("\nWaiting for " + controller.getOpponentUsername() + " to play...");
        }
    }

    @Override
    public void handleInput(String input) {
        if (controller.getState().isGameOver()) {
            // If the game is over, pressing enter returns them to the main menu
            sm.changeState(new MainMenuScreen(sm, client));
            return;
        }

        if (!controller.isMyTurn()) {
            System.out.println("[!] It is not your turn yet. Please wait for the opponent.");
            return;
        }

        try {
            String[] parts = input.trim().split("\\s+");
            if (parts.length != 4) {
                System.out.println("[!] Invalid format. Please enter exactly 4 numbers separated by spaces (e.g., 0 0 1 0).");
                return;
            }

            int x1 = Integer.parseInt(parts[0]);
            int y1 = Integer.parseInt(parts[1]);
            int x2 = Integer.parseInt(parts[2]);
            int y2 = Integer.parseInt(parts[3]);

            DotsAndBoxesMove move = new DotsAndBoxesMove(x1, y1, x2, y2);
            controller.attemptLocalMove(move);

            // Check if that move ended the game locally
            checkGameOver();

        } catch (NumberFormatException e) {
            System.out.println("[!] Coordinates must be valid integers.");
        }
    }

    @Override
    public void handlePush(Message message) {
        logger.debug("GameScreen received push notification: {}", message.actionType());

        if (message.actionType() == ActionType.GAME_MOVE) {
            MessageBody.GameMove body = (MessageBody.GameMove) message.body();
            try {
                DotsAndBoxesMove move = (DotsAndBoxesMove) new DotsAndBoxesMoveCodec().deserialize(body.rawMove());
                controller.applyOpponentMove(move);
            } catch (MalformedMessageException e) {
                throw new RuntimeException(e);
            }
        }

        System.out.println("\n[Update received from server]");

        checkGameOver();

        if (!controller.getState().isGameOver()) {
            display();
        }
    }

    @Override
    public void onExit() {
        logger.info("Exiting Game Screen");
    }

    /**
     * Helper method to evaluate end-state and draw the final scoreboard.
     */
    private void checkGameOver() {
        if (controller.getState().isGameOver()) {
            ClientBoardRenderer.printBoard(controller);
            UUID winnerId = controller.getState().getWinnerId();

            System.out.println("\n=================================");
            if (winnerId == null) {
                System.out.println("        IT'S A TIE!");
            } else if (winnerId.equals(controller.getMyUserId())) {
                System.out.println("        YOU WON!");
            } else {
                System.out.println("        YOU LOST!");
            }
            System.out.println("=================================\n");

            System.out.print("Press ENTER to return to the Main Menu...");
        }
    }
}