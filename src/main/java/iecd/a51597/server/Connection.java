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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.UUID;

public class Connection implements Runnable {

    private final Socket clientSocket;
    private final Server server;
    private final Logger logger = LogManager.getLogger(Connection.class);

    private DataInputStream  inputStream;
    private DataOutputStream outputStream;

    private final SessionManager sessionManager;
    private final CommParser commParser;
    private final MessageBuilder messageBuilder;

    public Connection(Socket client, Server server, SessionManager sessionManager,
                      CommParser commParser, MessageBuilder messageBuilder) {
        this.clientSocket   = client;
        this.server         = server;
        this.sessionManager = sessionManager;
        this.commParser     = commParser;
        this.messageBuilder = messageBuilder;
        initStreams();
    }

    private void initStreams() {
        try {
            this.inputStream  = new DataInputStream(clientSocket.getInputStream());
            this.outputStream = new DataOutputStream(clientSocket.getOutputStream());
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
            int length = inputStream.readInt();

            if (length <= 0) {
                logger.warn("Non-positive frame length {} from {}, closing connection",
                        length, clientSocket.getInetAddress());
                closeConnection();
                return;
            }

            byte[] frameBytes = new byte[length];
            inputStream.readFully(frameBytes);

            Message message = commParser.parseMessage(new ByteArrayInputStream(frameBytes));
            // TODO: dispatch message to handler

        } catch (EOFException | SocketException e) {
            // Client closed the connection cleanly (EOF) or the socket was
            // reset/closed from our side — either way, no error to report.
            logger.info("Connection closed by {}", clientSocket.getInetAddress());
            closeConnection();
        } catch (IOException e) {
            if (!clientSocket.isClosed()) {
                logger.error("IO error reading frame from {}: {}",
                        clientSocket.getInetAddress(), e.getMessage());
            }
            closeConnection();
        } catch (MalformedMessageException e) {
            logger.warn("Malformed message from {}: {}", clientSocket.getInetAddress(), e.getMessage());
            sendError(ErrorCodeType.MALFORMED_REQUEST, "Message does not conform to schema");
        } catch (MessageParseException e) {
            logger.error("Parse failure from {}: {}", clientSocket.getInetAddress(), e.getMessage());
            sendError(ErrorCodeType.INTERNAL_ERROR, "Failed to parse message");
        } catch (CommException e) {
            logger.error("Unexpected comm error from {}: {}",
                    clientSocket.getInetAddress(), e.getMessage());
            sendError(ErrorCodeType.INTERNAL_ERROR, "Communication error");
        }
    }

    private void sendMessage(byte[] payload) {
        try {
            outputStream.writeInt(payload.length);
            outputStream.write(payload);
            outputStream.flush();
        } catch (IOException e) {
            logger.error("Error sending message to {}: {}",
                    clientSocket.getInetAddress(), e.getMessage());
            closeConnection();
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