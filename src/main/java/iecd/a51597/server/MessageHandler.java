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
            case REGISTER: register(message);
            case LOGIN: login(message);
            case LOGOUT: logout(message);
            case UPDATE_PROFILE: updateProfile(message);
            case SEARCH_USERS: searchUsers(message);
            case GAME_INVITE: gameInvite(message);
            case GAME_INVITE_RESPONSE: gameInviteResponse(message);
            case GAME_MOVE: gameMove(message);
            case GAME_OVER: gameOver(message);
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
        }
    }

    private void gameOver(Message message) {
        //TODO
    }

    private void gameMove(Message message) {
        //TODO
    }

    private void gameInviteResponse(Message message) {
        //TODO
    }

    private void gameInvite(Message message) {
        //TODO
    }

    private void searchUsers(Message message) {
        //TODO
    }

    private void updateProfile(Message message) {
        //TODO
    }

    private void logout(Message message) {
        //TODO
    }

    private void login(Message message) {
        //TODO
    }

    private void register(Message message) {
        //TODO
    }
}
