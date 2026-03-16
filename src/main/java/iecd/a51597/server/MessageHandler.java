package iecd.a51597.server;

import iecd.a51597.server.protocol.Message;
import iecd.a51597.server.protocol.builders.MessageBuilder;
import iecd.a51597.server.protocol.exceptions.CommException;
import iecd.a51597.server.protocol.exceptions.MalformedMessageException;
import iecd.a51597.server.protocol.parsers.CommParser;

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
            // TODO
        } catch (CommException e) {
            // TODO
        }
    }

    private void dispatch(Message message, Connection connection) {
        // TODO
    }
}
