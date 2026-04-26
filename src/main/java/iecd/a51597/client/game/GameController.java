package iecd.a51597.client.game;

import iecd.a51597.client.config.ClientConfiguration;
import iecd.a51597.client.network.ServerConnection;
import iecd.a51597.common.game.MoveResult;
import iecd.a51597.common.game.dotsandboxes.DotsAndBoxesGame;
import iecd.a51597.common.game.dotsandboxes.DotsAndBoxesMove;
import iecd.a51597.common.protocol.Message;
import iecd.a51597.common.protocol.MessageFactory;

import java.util.UUID;

/**
 * Controller responsible for managing active game state and server communication.
 */
public class GameController {
    private final DotsAndBoxesGame localGameState;
    private final ServerConnection connection;

    private final UUID myUserId;
    private final String myUsername;
    private final String opponentUsername;

    public GameController(DotsAndBoxesGame initialState, ServerConnection connection, UUID myUserId,
                          String myUsername, String opponentUsername) {
        this.localGameState = initialState;
        this.connection = connection;
        this.myUserId = myUserId;
        this.myUsername = myUsername;
        this.opponentUsername = opponentUsername;
    }

    public DotsAndBoxesGame getState() { return localGameState; }
    public String getMyUsername() { return myUsername; }
    public String getOpponentUsername() { return opponentUsername; }
    public UUID getMyUserId() { return myUserId; }

    public boolean isMyTurn() {
        return localGameState.getCurrentPlayerId().equals(myUserId);
    }

    /**
     * Called by the GameScreen when the user inputs a move.
     */
    public void attemptLocalMove(DotsAndBoxesMove move) {
        // 1. Evaluate locally
        MoveResult result = localGameState.applyMove(myUserId, move);

        if (result instanceof MoveResult.Rejected(String reason)) {
            System.out.println("\n[!] Invalid move: " + reason + "\n");
            return;
        }

        // 2. If valid locally, send to the server
        String rawMove = move.x1() + "," + move.y1() + "," + move.x2() + "," + move.y2();

        Message request = MessageFactory.createMoveRequest(
                ClientConfiguration.PROTOCOL_VERSION,
                connection.getSessionManager().getSessionUUID(),
                localGameState.getGameId(),
                rawMove
        );
        connection.sendRequest(request);
    }

    /**
     * Called by the GameScreen when the user wants to surrender.
     */
    public void attemptSurrender() {
        if (localGameState.isGameOver()) return;

        Message request = MessageFactory.buildSurrenderRequest(
                ClientConfiguration.PROTOCOL_VERSION,
                connection.getSessionManager().getSessionUUID(),
                localGameState.getGameId()
        );
        connection.sendRequest(request);
    }

    /**
     * Called by your network message parser when a GAME_MOVE_PUSH arrives from the server.
     */
    public void applyOpponentMove(DotsAndBoxesMove move) {
        UUID opponentId = localGameState.getPlayer1Id().equals(myUserId)
                ? localGameState.getPlayer2Id()
                : localGameState.getPlayer1Id();

        localGameState.applyMove(opponentId, move);
    }
}