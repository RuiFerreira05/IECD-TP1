package iecd.a51597.server.handlers;

import iecd.a51597.server.network.Connection;
import iecd.a51597.server.session.Session;
import iecd.a51597.server.session.SessionManager;
import iecd.a51597.common.protocol.Message;
import iecd.a51597.common.protocol.MessageBody;
import iecd.a51597.common.protocol.builders.MessageBuilder;
import iecd.a51597.common.protocol.types.ErrorCodeType;
import iecd.a51597.server.store.User;
import iecd.a51597.server.store.UserStore;
import iecd.a51597.server.store.exceptions.UsernameAlreadyTakenException;

import java.util.Optional;

/**
 * Handles user profile updates for authenticated sessions.
 */
public class ProfileHandler extends BaseHandler {

    private final UserStore userStore;

    /**
     * Creates a profile handler.
     */
    public ProfileHandler(MessageBuilder messageBuilder, SessionManager sessionManager, UserStore userStore) {
        super(messageBuilder, sessionManager);
        this.userStore = userStore;
    }

    /**
     * Applies profile changes from an update request.
     */
    public void updateProfile(Message message, Connection connection) {
        Optional<Session> sessionOpt = requireSession(message, connection);
        if (sessionOpt.isEmpty()) return;

        User user = sessionOpt.get().getUser();
        MessageBody.UpdateProfile body = (MessageBody.UpdateProfile) message.body();

        try {
            if (body.username() != null && !body.username().isBlank()) userStore.updateUsername(user, body.username());
            if (body.password() != null && !body.password().isBlank()) userStore.updatePassword(user, body.password());
            if (body.photo() != null && !body.photo().isBlank()) userStore.updatePhoto(user, body.photo());
        } catch (UsernameAlreadyTakenException e) {
            sendError(message, connection, ErrorCodeType.USERNAME_TAKEN, "Username is already taken");
            return;
        }

        connection.sendMessage(messageBuilder.ok(message.messageId(), message.actionType()));
    }
}
