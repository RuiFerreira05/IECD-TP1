package iecd.a51597.client.game;

import iecd.a51597.client.network.ServerConnection;
import iecd.a51597.client.session.ClientSessionManager;
import iecd.a51597.common.game.dotsandboxes.DotsAndBoxesGame;
import iecd.a51597.common.game.dotsandboxes.DotsAndBoxesMove;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GameControllerTest {

    private GameController controller;
    private ServerConnection connection;
    private DotsAndBoxesGame game;
    private final UUID myId = UUID.randomUUID();
    private final UUID oppId = UUID.randomUUID();
    private final UUID gameId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        connection = mock(ServerConnection.class);
        ClientSessionManager sessionManager = mock(ClientSessionManager.class);
        when(connection.getSessionManager()).thenReturn(sessionManager);
        when(sessionManager.getSessionUUID()).thenReturn(UUID.randomUUID());

        game = new DotsAndBoxesGame(gameId, myId, oppId);
        controller = new GameController(game, connection, myId, "me", "opp");
    }

    @Test
    void isMyTurn_reflectsGameState() {
        assertTrue(controller.isMyTurn());
        
        // Apply a move that doesn't capture a box (turn changes)
        game.applyMove(myId, new DotsAndBoxesMove(0, 0, 1, 0));
        assertFalse(controller.isMyTurn());
    }

    @Test
    void attemptLocalMove_valid_sendsRequest() {
        DotsAndBoxesMove move = new DotsAndBoxesMove(0, 0, 1, 0);
        controller.attemptLocalMove(move);

        verify(connection).sendRequest(any());
        // Verify turn changed locally
        assertFalse(controller.isMyTurn());
    }

    @Test
    void applyOpponentMove_updatesLocalState() {
        // Change turn to opponent first
        game.applyMove(myId, new DotsAndBoxesMove(0, 0, 1, 0));
        assertFalse(controller.isMyTurn());

        DotsAndBoxesMove oppMove = new DotsAndBoxesMove(0, 0, 0, 1);
        controller.applyOpponentMove(oppMove);

        assertTrue(game.getDrawnLines().contains(oppMove));
        assertTrue(controller.isMyTurn()); // opp didn't capture, so it's my turn again
    }
}
