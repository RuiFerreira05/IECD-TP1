package iecd.a51597.server.handlers;

import iecd.a51597.server.network.Connection;
import iecd.a51597.server.session.Session;
import iecd.a51597.server.session.SessionManager;
import iecd.a51597.server.protocol.Message;
import iecd.a51597.server.protocol.MessageBody;
import iecd.a51597.server.protocol.builders.MessageBuilder;
import iecd.a51597.server.protocol.types.ErrorCodeType;
import iecd.a51597.server.store.UserStore;
import iecd.a51597.server.store.exceptions.UsernameAlreadyTakenException;

public class AuthHandler extends BaseHandler {

    private final UserStore userStore;

    public AuthHandler(MessageBuilder messageBuilder, SessionManager sessionManager, UserStore userStore) {
        super(messageBuilder, sessionManager);
        this.userStore = userStore;
    }

    public void register(Message message, Connection connection) {
        MessageBody.Register body = (MessageBody.Register) message.body();

        try {
            userStore.register(body.username(), body.password());
            connection.sendMessage(messageBuilder.ok(message.messageId(), message.actionType()));
        } catch (UsernameAlreadyTakenException e) {
            sendError(message, connection, ErrorCodeType.USERNAME_TAKEN, "Username is already taken");
        }
    }

    public void login(Message message, Connection connection) {
        MessageBody.Login body = (MessageBody.Login) message.body();

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

    public void logout(Message message, Connection connection) {
        if (message.sessionToken() == null) return;

        sessionManager.invalidate(message.sessionToken());
        connection.sendMessage(messageBuilder.ok(message.messageId(), message.actionType()));
    }
}
