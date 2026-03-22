package iecd.a51597.server.handlers;

import iecd.a51597.server.Connection;
import iecd.a51597.server.Session;
import iecd.a51597.server.SessionManager;
import iecd.a51597.server.protocol.Message;
import iecd.a51597.server.protocol.MessageBody;
import iecd.a51597.server.protocol.builders.MessageBuilder;
import iecd.a51597.server.protocol.types.ErrorCodeType;
import iecd.a51597.server.store.User;
import iecd.a51597.server.store.UserStore;
import iecd.a51597.server.store.exceptions.UsernameAlreadyTakenException;

import java.util.Optional;

public class ProfileHandler extends BaseHandler {

    private final UserStore userStore;

    public ProfileHandler(MessageBuilder messageBuilder, SessionManager sessionManager, UserStore userStore) {
        super(messageBuilder, sessionManager);
        this.userStore = userStore;
    }

    public void updateProfile(Message message, Connection connection) {
        Optional<Session> sessionOpt = requireSession(message, connection);
        if (sessionOpt.isEmpty()) return;

        User user = sessionOpt.get().getUser();
        MessageBody.UpdateProfile body = (MessageBody.UpdateProfile) message.body();

        try {
            if (body.username() != null) userStore.updateUsername(user, body.username());
            if (body.password() != null) userStore.updatePassword(user, body.password());
            if (body.photo()    != null) userStore.updatePhoto(user, body.photo());
        } catch (UsernameAlreadyTakenException e) {
            sendError(message, connection, ErrorCodeType.USERNAME_TAKEN, "Username is already taken");
            return;
        }

        connection.sendMessage(messageBuilder.ok(message.messageId(), message.actionType()));
    }
}
