package iecd.a51597.server.handlers;

import iecd.a51597.server.config.ServerConfiguration;
import iecd.a51597.server.network.Connection;
import iecd.a51597.common.protocol.Message;
import iecd.a51597.common.protocol.builders.MessageBuilder;
import iecd.a51597.common.protocol.exceptions.CommException;
import iecd.a51597.common.protocol.exceptions.MalformedMessageException;
import iecd.a51597.common.protocol.exceptions.MessageParseException;
import iecd.a51597.common.protocol.parsers.CommParser;
import iecd.a51597.common.protocol.types.ErrorCodeType;
import iecd.a51597.common.protocol.types.MessageType;

import java.io.ByteArrayInputStream;

public class MessageHandler {

    private final CommParser commParser;
    private final MessageBuilder messageBuilder;
    private final AuthHandler authHandler;
    private final ProfileHandler profileHandler;
    private final SearchHandler searchHandler;
    private final GameHandler gameHandler;

    public MessageHandler(CommParser commParser, MessageBuilder messageBuilder,
                          AuthHandler authHandler, ProfileHandler profileHandler,
                          SearchHandler searchHandler, GameHandler gameHandler) {
        this.commParser = commParser;
        this.messageBuilder = messageBuilder;
        this.authHandler = authHandler;
        this.profileHandler = profileHandler;
        this.searchHandler = searchHandler;
        this.gameHandler = gameHandler;
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
        } catch (MessageParseException e) {
            connection.sendMessage(messageBuilder.errorNoId(
                    ErrorCodeType.MALFORMED_REQUEST,
                    "The message sent could not be parsed"
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

        if (!message.version().equals(ServerConfiguration.PROTOCOL_VERSION)) {
            connection.sendMessage(messageBuilder.error(
                    message.messageId(),
                    message.actionType(),
                    ErrorCodeType.OUTDATED_PROTOCOL,
                    "Unsupported protocol version"
            ));
            return;
        }

        switch (message.actionType()) {
            case REGISTER -> authHandler.register(message, connection);
            case LOGIN -> authHandler.login(message, connection);
            case LOGOUT -> authHandler.logout(message, connection);
            case UPDATE_PROFILE -> profileHandler.updateProfile(message, connection);
            case SEARCH_USERS -> searchHandler.searchUsers(message, connection);
            case GAME_INVITE -> gameHandler.gameInvite(message, connection);
            case GAME_INVITE_RESPONSE -> gameHandler.gameInviteResponse(message, connection);
            case GAME_MOVE -> gameHandler.gameMove(message, connection);
            case GAME_OVER -> gameHandler.gameOver(message, connection);
            case UNKNOWN -> connection.sendMessage(messageBuilder.error(
                message.messageId(),
                message.actionType(),
                ErrorCodeType.UNEXPECTED_MESSAGE_ACTION,
                "Action not expected by server"
            ));
            default -> connection.sendMessage(messageBuilder.error(
                message.messageId(),
                message.actionType(),
                ErrorCodeType.UNKNOWN_ACTION,
                "Unknown action type"
            ));
        }
    }
}