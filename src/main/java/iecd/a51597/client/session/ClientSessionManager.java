package iecd.a51597.client.session;

import iecd.a51597.client.config.ClientConfiguration;
import iecd.a51597.client.network.ServerConnection;
import iecd.a51597.client.session.exceptions.UnexpectedResponse;
import iecd.a51597.common.protocol.Message;
import iecd.a51597.common.protocol.MessageBody;
import iecd.a51597.common.protocol.MessageFactory;
import iecd.a51597.common.protocol.types.ActionType;
import iecd.a51597.common.protocol.types.MessageType;
import iecd.a51597.common.store.PlayerStats;
import iecd.a51597.common.store.UserDTO;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ClientSessionManager {

    private final ServerConnection serverConnection;
    private UUID sessionUUID;
    private UserDTO user;

    public ClientSessionManager(ServerConnection serverConnection) {
        this.serverConnection = serverConnection;
        this.sessionUUID = null;
        this.user = null;
    }

    public UUID login(String username, String password) throws ExecutionException, InterruptedException, TimeoutException, UnexpectedResponse {
        Message request = MessageFactory.buildLoginRequest(
                ClientConfiguration.PROTOCOL_VERSION,
                null,
                username,
                password
        );
        Message response = serverConnection.sendRequest(request).get(10, TimeUnit.SECONDS);

        if (response.body() instanceof MessageBody.LoginResponse(
                String status, UUID session, UserDTO userDTO, MessageBody.ErrorDetail error
        )) {
            if (status.equals("OK")) {
                this.user = userDTO;
                this.sessionUUID = session;
                return session;
            } else {
                throw new RuntimeException("Login failed: " + error);
            }
        } else {
            throw new UnexpectedResponse("Expected LoginResponse, got " + response.body().getClass().getSimpleName());
        }
    }

    public void logout() throws ExecutionException, InterruptedException, TimeoutException, UnexpectedResponse {
        if (sessionUUID == null) {
            return;
        }

        Message request = MessageFactory.buildLogoutRequest(
                ClientConfiguration.PROTOCOL_VERSION,
                null,
                sessionUUID
                );
        Message response = serverConnection.sendRequest(request).get(10, TimeUnit.SECONDS);

        if (response.body() instanceof MessageBody.LogoutResponse(String status, MessageBody.ErrorDetail error)) {
            if (status.equals("OK")) {
                this.sessionUUID = null;
                this.user = null;
                return;
            }
            throw new RuntimeException("Logout failed: " + error);
        } else {
            throw new UnexpectedResponse("Expected LogoutResponse, got " + response.body().getClass().getSimpleName());
        }
    }

    public UUID register(String username, String password) throws ExecutionException, InterruptedException, TimeoutException, UnexpectedResponse {
        Message request = MessageFactory.buildRegisterRequest(
                ClientConfiguration.PROTOCOL_VERSION,
                null,
                username,
                password
        );
        Message response = serverConnection.sendRequest(request).get(10, TimeUnit.SECONDS);

        if (response.body() instanceof MessageBody.RegisterResponse(String status, MessageBody.ErrorDetail error)) {
            if (status.equals("OK")) {
                return login(username, password);
            } else {
                throw new RuntimeException("Registration failed: " + error);
            }
        } else {
            throw new UnexpectedResponse("Expected RegisterResponse, got " + response.body().getClass().getSimpleName());
        }
    }

    public boolean isLoggedIn() {
        return sessionUUID != null;
    }

    public UUID getSessionUUID() {
        return sessionUUID;
    }
}
