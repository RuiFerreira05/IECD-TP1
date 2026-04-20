package iecd.a51597.server.handlers;

import iecd.a51597.server.network.Connection;
import iecd.a51597.server.session.Session;
import iecd.a51597.server.session.SessionManager;
import iecd.a51597.common.protocol.Message;
import iecd.a51597.common.protocol.builders.MessageBuilder;
import iecd.a51597.common.protocol.types.ErrorCodeType;

import java.util.Optional;

public abstract class BaseHandler {

    protected final MessageBuilder messageBuilder;
    protected final SessionManager sessionManager;

    protected BaseHandler(MessageBuilder messageBuilder, SessionManager sessionManager) {
        this.messageBuilder = messageBuilder;
        this.sessionManager = sessionManager;
    }

    protected void sendError(Message message, Connection connection, ErrorCodeType errorCode, String description) {
        connection.sendMessage(messageBuilder.error(
                message.messageId(),
                message.actionType(),
                errorCode,
                description
        ));
    }

    protected Optional<Session> requireSession(Message message, Connection connection) {
        if (message.sessionToken() == null) {
            sendError(message, connection, ErrorCodeType.NOT_AUTHENTICATED, "No session token provided");
            return Optional.empty();
        }
        Optional<Session> session = sessionManager.validate(message.sessionToken());
        if (session.isEmpty()) {
            sendError(message, connection, ErrorCodeType.SESSION_EXPIRED, "Session token is invalid or expired");
        }
        return session;
    }
}
