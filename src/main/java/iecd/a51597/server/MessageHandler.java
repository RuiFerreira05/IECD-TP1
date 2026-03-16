package iecd.a51597.server;

import iecd.a51597.server.protocol.Message;
import iecd.a51597.server.protocol.builders.MessageBuilder;
import iecd.a51597.server.protocol.exceptions.CommException;
import iecd.a51597.server.protocol.exceptions.MalformedMessageException;
import iecd.a51597.server.protocol.parsers.CommParser;
import iecd.a51597.server.protocol.types.ErrorCodeType;
import iecd.a51597.server.protocol.types.MessageType;

import java.io.ByteArrayInputStream;

public class MessageHandler {

    private final CommParser commParser;
    private final MessageBuilder messageBuilder;
    private final SessionManager sessionManager;

    public MessageHandler(CommParser commParser, MessageBuilder messageBuilder, SessionManager sessionManager) {
        this.commParser = commParser;
        this.messageBuilder = messageBuilder;
        this.sessionManager = sessionManager;
    }

    public void handle(byte[] frameBytes, Connection connection) {
        try {
            Message message = commParser.parseMessage(new ByteArrayInputStream(frameBytes));
            dispatch(message, connection);
        } catch (MalformedMessageException e) {
            connection.sendMessage(messageBuilder.errorNoId(
                    ErrorCodeType.MALFORMED_REQUEST,
                    "The message does not conform to protocol"
            ));
        } catch (CommException e) {
            connection.sendMessage(messageBuilder.errorNoId(
                    ErrorCodeType.INTERNAL_ERROR,
                    "An internal error occurred while processing the message"
            ));
        }
    }

    private void dispatch(Message message, Connection connection) {
        if (message.messageType() != MessageType.REQUEST) {
            connection.sendMessage(messageBuilder.error(
                    message.messageId(),
                    message.actionType(),
                    ErrorCodeType.UNEXPECTED_MESSAGE_TYPE,
                    "Server only accepts REQUEST messages"
            ));
            return;
        }

        switch (message.actionType()) {
            case REGISTER:
                // TODO
                break;
            case LOGIN:
                // TODO
                break;
            case LOGOUT:
                // TODO
                break;
            case UPDATE_PROFILE:
                // TODO
                break;
            case SEARCH_USERS:
                // TODO
                break;
            case GAME_INVITE:
                // TODO
                break;
            case GAME_INVITE_RESPONSE:
                // TODO
                break;
            case GAME_MOVE:
                // TODO
                break;
            case GAME_OVER:
                // TODO
                break;
            case UNKNOWN:
                connection.sendMessage(messageBuilder.error(
                        message.messageId(),
                        message.actionType(),
                        ErrorCodeType.UNEXPECTED_MESSAGE_ACTION,
                        "Action not expected by server"
                ));
                break;
            default:
                connection.sendMessage(messageBuilder.error(
                        message.messageId(),
                        message.actionType(),
                        ErrorCodeType.UNKNOWN_ACTION,
                        "Unknown action type"
                ));
                break;
        }
    }
}
