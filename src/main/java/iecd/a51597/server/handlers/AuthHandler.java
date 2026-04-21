package iecd.a51597.server.handlers;

import iecd.a51597.server.network.Connection;
import iecd.a51597.server.session.Session;
import iecd.a51597.server.session.SessionManager;
import iecd.a51597.common.protocol.Message;
import iecd.a51597.common.protocol.MessageBody;
import iecd.a51597.common.protocol.builders.server.ServerMessageBuilder;
import iecd.a51597.common.protocol.types.ErrorCodeType;
import iecd.a51597.server.store.UserStore;
import iecd.a51597.server.store.exceptions.UsernameAlreadyTakenException;

/**
 * Handles registration/login/logout protocol actions.
 */
public class AuthHandler extends BaseHandler {

    private final UserStore userStore;

    /**
     * Creates an auth handler.
     */
    public AuthHandler(ServerMessageBuilder messageBuilder, SessionManager sessionManager, UserStore userStore) {
        super(messageBuilder, sessionManager);
        this.userStore = userStore;
    }

    /**
     * Handles user registration requests.
     */
    public void register(Message message, Connection connection) {
        MessageBody.Register body = (MessageBody.Register) message.body();

        try {
            userStore.register(body.username(), body.password());
            connection.sendMessage(messageBuilder.ok(message.messageId(), message.actionType()));
        } catch (UsernameAlreadyTakenException e) {
            sendError(message, connection, ErrorCodeType.USERNAME_TAKEN, "Username is already taken");
        }
    }

    /**
     * Handles user login requests.
     */
    public void login(Message message, Connection connection) {
        MessageBody.LoginRequest body = (MessageBody.LoginRequest) message.body();

        userStore.findByCredentials(body.username(), body.password()).ifPresentOrElse(
                user -> {
                    Session session = sessionManager.createSession(user, connection);
                    connection.sendMessage(messageBuilder.loginSuccess(
                            message.messageId(),
                            session.getToken(),
                            user
                    ));
                },
                () -> sendError(message, connection, ErrorCodeType.AUTH_FAILED, "Invalid username or password")
        );
    }

    /**
     * Handles logout requests by invalidating the current session.
     */
    public void logout(Message message, Connection connection) {
        if (requireSession(message, connection).isEmpty()) return;

        sessionManager.invalidate(message.sessionToken());
        connection.sendMessage(messageBuilder.ok(message.messageId(), message.actionType()));
    }
}
