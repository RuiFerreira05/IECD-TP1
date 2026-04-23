package iecd.a51597.client.session;

import iecd.a51597.client.config.ClientConfiguration;
import iecd.a51597.client.network.ServerConnection;
import iecd.a51597.common.protocol.Message;
import iecd.a51597.common.protocol.MessageBody;
import iecd.a51597.common.protocol.MessageFactory;
import iecd.a51597.common.protocol.types.ErrorCodeType;
import iecd.a51597.common.store.UserDTO;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ClientSessionManager {

    private final ServerConnection serverConnection;
    private UUID sessionUUID;
    private UserDTO user;

    public ClientSessionManager(ServerConnection serverConnection) {
        this.serverConnection = serverConnection;
        this.sessionUUID = null;
        this.user = null;
    }

    public sealed interface LoginResult {
        record Success(UUID sessionToken) implements LoginResult {}
        record InvalidCredentials() implements LoginResult {}
        record Error(String message) implements LoginResult {}
    }

    public LoginResult login(String username, String password) {
        Message request = MessageFactory.buildLoginRequest(
                ClientConfiguration.PROTOCOL_VERSION,
                null,
                username,
                password
        );

        Message response = null;
        try {
            response = serverConnection.sendRequest(request).get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            return new LoginResult.Error(e.getMessage());
        }

        if (response.body() instanceof MessageBody.LoginResponse(
                String status, UUID session, UserDTO userDTO, MessageBody.ErrorDetail error
        )) {
            if (status.equals("OK")) {
                this.user = userDTO;
                this.sessionUUID = session;
                return new LoginResult.Success(session);
            } else {
                if (error.code() == ErrorCodeType.AUTH_FAILED) {
                    return new LoginResult.InvalidCredentials();
                }
            }
        } else {
            return new LoginResult.Error("Unexpected response type: " + response.body().getClass());
        }

        return new LoginResult.Error("Something went wrong");
    }

    public UserDTO getUser() {
        return user;
    }

    public sealed interface LogoutResult {
        record Success() implements LogoutResult {}
        record NotLoggedIn() implements LogoutResult {}
        record Error(String message) implements LogoutResult {}
    }

    public LogoutResult logout() {
        if (sessionUUID == null) {
            return new LogoutResult.NotLoggedIn();
        }

        Message request = MessageFactory.buildLogoutRequest(
                ClientConfiguration.PROTOCOL_VERSION,
                null,
                sessionUUID
                );

        Message response = null;
        try {
            response = serverConnection.sendRequest(request).get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            return new LogoutResult.Error(e.getMessage());
        }

        if (response.body() instanceof MessageBody.LogoutResponse(String status, MessageBody.ErrorDetail error)) {
            if (status.equals("OK")) {
                this.sessionUUID = null;
                this.user = null;
                return new LogoutResult.Success();
            }
            return new LogoutResult.Error("Logout failed " + error.message());
        } else {
            return new LogoutResult.Error("Unexpected response type: " + response.body().getClass());
        }
    }

    public sealed interface RegisterResult {
        record Success() implements RegisterResult {}
        record UsernameTaken() implements RegisterResult {}
        record Error(String message) implements RegisterResult {}
    }

    public RegisterResult register(String username, String password) {
        Message request = MessageFactory.buildRegisterRequest(
                ClientConfiguration.PROTOCOL_VERSION,
                null,
                username,
                password
        );

        Message response = null;
        try {
            response = serverConnection.sendRequest(request).get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            return new RegisterResult.Error(e.getMessage());
        }

        if (response.body() instanceof MessageBody.RegisterResponse(String status, MessageBody.ErrorDetail error)) {
            if (status.equals("OK")) {
                return new RegisterResult.Success();
            } else {
                if (error.code() == ErrorCodeType.USERNAME_TAKEN) {
                    return new RegisterResult.UsernameTaken();
                } else {
                    return new RegisterResult.Error("Registration failed: " + error.message());
                }
            }
        } else {
            return new RegisterResult.Error("Unexpected response type: " + response.body().getClass());
        }
    }

    public boolean isLoggedIn() {
        return sessionUUID != null;
    }

    public UUID getSessionUUID() {
        return sessionUUID;
    }
}
