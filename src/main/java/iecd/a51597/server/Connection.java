package iecd.a51597.server;

import iecd.a51597.server.protocol.builders.MessageBuilder;
import iecd.a51597.server.protocol.parsers.CommParser;
import iecd.a51597.server.protocol.Message;
import iecd.a51597.server.protocol.exceptions.CommException;
import iecd.a51597.server.protocol.exceptions.MalformedMessageException;
import iecd.a51597.server.protocol.exceptions.MessageParseException;
import iecd.a51597.server.protocol.types.ErrorCodeType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.UUID;

public class Connection implements Runnable {

    private final Socket clientSocket;
    private final Server server;
    private final Logger logger = LogManager.getLogger(Connection.class);
    private InputStream  inputStream;
    private OutputStream outputStream;
    private final SessionManager sessionManager;
    private final CommParser commParser;
    private final MessageBuilder messageBuilder;

    public Connection(Socket client, Server server, SessionManager sessionManager, CommParser commParser, MessageBuilder messageBuilder) {
        this.clientSocket = client;
        this.server = server;
        this.sessionManager = sessionManager;
        this.commParser = commParser;
        this.messageBuilder = messageBuilder;
        initStreams();
    }

    private void initStreams() {
        try {
            this.inputStream = this.clientSocket.getInputStream();
            this.outputStream = this.clientSocket.getOutputStream();
        } catch (IOException e) {
            logger.error("Error initializing streams for connection: {}", e.getMessage());
            closeConnection();
        }
    }

    @Override
    public void run() {
        try {
            while (!clientSocket.isClosed()) {
                handleIncomingMessage();
            }
        } finally {
            closeConnection();
        }
    }

    private void handleIncomingMessage() {
        try {
            Message message = commParser.parseMessage(inputStream);
        } catch (MalformedMessageException e) {
            logger.warn("Malformed message from {}: {}", clientSocket.getInetAddress(), e.getMessage());
            sendError(ErrorCodeType.MALFORMED_REQUEST, "Message does not conform to schema");
        } catch (MessageParseException e) {
            logger.error("Parse failure from {}: {}", clientSocket.getInetAddress(), e.getMessage());
            sendError(ErrorCodeType.INTERNAL_ERROR, "Failed to parse message");
        } catch (CommException e) {
            logger.error("Unexpected comm error: {}", e.getMessage());
            sendError(ErrorCodeType.INTERNAL_ERROR, "Communication error");
        }
    }

    private void sendError(ErrorCodeType errorCode, String description) {
        byte[] response = messageBuilder.errorNoId(errorCode, description);
        sendMessage(response);
    }

    private void sendError(UUID messageId, ErrorCodeType errorCode, String description) {
        byte[] response = messageBuilder.error(messageId, errorCode, description);
        sendMessage(response);
    }

    private void sendMessage(byte[] message) {
        try {
            outputStream.write(message);
            outputStream.flush();
        } catch (IOException e) {
            logger.error("Error sending error response: {}", e.getMessage());
            closeConnection();
        }
    }

    public Socket getClientSocket() {
        return clientSocket;
    }

    public void closeConnection() {
        if (clientSocket.isClosed()) return;
        server.removeConnection(this);
        try {
            clientSocket.close();
        } catch (IOException e) {
            logger.error("Error closing connection: {}", e.getMessage());
        }
    }
}
