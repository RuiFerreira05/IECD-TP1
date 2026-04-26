package iecd.a51597.server.handlers;

import iecd.a51597.server.network.Connection;
import iecd.a51597.server.persistence.PersistenceManager;
import iecd.a51597.server.session.Session;
import iecd.a51597.server.session.SessionManager;
import iecd.a51597.common.protocol.Message;
import iecd.a51597.common.protocol.MessageBody;
import iecd.a51597.common.protocol.builders.server.ServerMessageBuilder;
import iecd.a51597.common.protocol.types.ErrorCodeType;
import iecd.a51597.server.store.entities.User;
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
    public ProfileHandler(ServerMessageBuilder messageBuilder, SessionManager sessionManager, UserStore userStore, PersistenceManager persistenceManager) {
        super(messageBuilder, sessionManager, persistenceManager);
        this.userStore = userStore;
    }

    /**
     * Applies profile changes from an update request.
     */
    public void updateProfile(Message message, Connection connection) {
        logger.info("Received profile update request from connection");
        Optional<Session> sessionOpt = requireSession(message, connection);
        if (sessionOpt.isEmpty()) {
            logger.warn("Profile update request missing valid session token");
            return;
        };

        User user = sessionOpt.get().getUser();
        MessageBody.UpdateProfile body = (MessageBody.UpdateProfile) message.body();

        try {
            if (body.username() != null && !body.username().isBlank()) userStore.updateUsername(user, body.username());
            if (body.password() != null && !body.password().isBlank()) userStore.updatePassword(user, body.password());
            if (body.nationality() != null && !body.nationality().isBlank()) userStore.updateNationality(user, body.nationality());
            if (body.photo() != null) {
                String reference = persistenceManager.savePhoto(body.photo(), user.getPhoto());
                userStore.updatePhoto(user, reference);
            }
            if (body.dob() != null) userStore.updateDob(user, body.dob());
        } catch (UsernameAlreadyTakenException e) {
            logger.error("Failed to update profile for user {}", user.getUserId(), e);
            sendError(message, connection, ErrorCodeType.USERNAME_TAKEN, "Username is already taken");
            return;
        }

        connection.sendMessage(messageBuilder.updateProfileSuccess(message.messageId(), user));
    }
}
