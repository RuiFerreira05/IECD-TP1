package iecd.a51597.client.session;

import iecd.a51597.client.network.ServerConnection;
import iecd.a51597.common.protocol.Message;
import iecd.a51597.common.protocol.MessageBody;
import iecd.a51597.common.protocol.types.ActionType;
import iecd.a51597.common.protocol.types.MessageType;
import iecd.a51597.common.store.UserDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClientSessionManagerTest {

    private ClientSessionManager manager;
    private ServerConnection connection;

    @BeforeEach
    void setUp() {
        connection = mock(ServerConnection.class);
        manager = new ClientSessionManager(connection);
    }

    @Test
    void login_success_updatesState() {
        UUID sessionToken = UUID.randomUUID();
        UserDTO user = new UserDTO(UUID.randomUUID(), "alice", null, null, null, null);
        Message response = new Message(UUID.randomUUID(), MessageType.RESPONSE, "1.0", ActionType.LOGIN, null,
                new MessageBody.LoginResponse("OK", sessionToken, user, null));
        
        when(connection.sendRequest(any())).thenReturn(CompletableFuture.completedFuture(response));

        ClientSessionManager.LoginResult result = manager.login("alice", "pass");

        assertInstanceOf(ClientSessionManager.LoginResult.Success.class, result);
        assertEquals(sessionToken, ((ClientSessionManager.LoginResult.Success) result).sessionToken());
        assertTrue(manager.isLoggedIn());
        assertEquals("alice", manager.getUser().username());
    }

    @Test
    void logout_success_clearsState() {
        // Set logged in state manually if possible, or just login
        // But manager fields are private. Let's use the actual login first or reflect.
        // I'll just login.
        UUID sessionToken = UUID.randomUUID();
        UserDTO user = new UserDTO(UUID.randomUUID(), "alice", null, null, null, null);
        Message loginResp = new Message(UUID.randomUUID(), MessageType.RESPONSE, "1.0", ActionType.LOGIN, null,
                new MessageBody.LoginResponse("OK", sessionToken, user, null));
        when(connection.sendRequest(any())).thenReturn(CompletableFuture.completedFuture(loginResp));
        manager.login("alice", "pass");

        Message logoutResp = new Message(UUID.randomUUID(), MessageType.RESPONSE, "1.0", ActionType.LOGOUT, null,
                new MessageBody.LogoutResponse("OK", null));
        when(connection.sendRequest(any())).thenReturn(CompletableFuture.completedFuture(logoutResp));

        ClientSessionManager.LogoutResult result = manager.logout();

        assertInstanceOf(ClientSessionManager.LogoutResult.Success.class, result);
        assertFalse(manager.isLoggedIn());
        assertNull(manager.getUser());
    }
}
