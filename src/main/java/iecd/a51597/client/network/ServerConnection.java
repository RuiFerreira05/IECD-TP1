package iecd.a51597.client.network;

import iecd.a51597.client.Client;
import iecd.a51597.client.cli.screens.handlers.ClientSearchHandler;
import iecd.a51597.client.config.ClientConfiguration;
import iecd.a51597.client.session.ClientSessionManager;
import iecd.a51597.common.protocol.Message;
import iecd.a51597.common.protocol.builders.client.ClientMessageBuilder;
import iecd.a51597.common.protocol.exceptions.CommException;
import iecd.a51597.common.protocol.parsers.CommParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class ServerConnection implements Runnable {

    private volatile boolean connected = false;
    private Map<UUID, CompletableFuture<Message>> pendingRequests;

    private Socket socket;
    private String serverHost;
    private int serverPort;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private CommParser parser;
    private ClientMessageBuilder messageBuilder;
    private int reconnectAttempts;
    private ClientSearchHandler searchHandler;

    private ClientSessionManager sessionManager = null;

    public final Logger logger = LogManager.getLogger(ServerConnection.class);

    public ServerConnection(Client client, String serverHost, int serverPort, CommParser parser, ClientMessageBuilder messageBuilder) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.parser = parser;
        this.messageBuilder = messageBuilder;

        this.searchHandler = new ClientSearchHandler(this);
        this.pendingRequests = new ConcurrentHashMap<>();
        this.reconnectAttempts = ClientConfiguration.RECONNECT_ATTEMPTS;
    }

    public CompletableFuture<Message> sendRequest(Message request) {
        CompletableFuture<Message> future = new CompletableFuture<>();
        pendingRequests.put(request.messageId(), future);

        byte[] payload = messageBuilder.getMessageInBytes(request);
        if (payload == null) {
            pendingRequests.remove(request.messageId());
            future.completeExceptionally(new IllegalStateException("Failed to serialize request"));
            return future;
        }

        if (!writeFrame(payload)) {
            pendingRequests.remove(request.messageId());
            future.completeExceptionally(new IllegalStateException("Failed to send request to server"));
        }

        return future;
    }

    private synchronized boolean writeFrame(byte[] payload) {
        if (!connected) {
            logger.warn("Attempted to send message while not connected to server.");
            return false;
        }
        try {
            outputStream.writeInt(payload.length);
            outputStream.write(payload);
            outputStream.flush();
            return true;
        } catch (IOException e) {
            logger.error("Error sending message to server", e);
            closeConnection();
            return false;
        }
    }

    private void closeConnection() {
        if (socket == null || socket.isClosed()) return;
        try {
            socket.close();
        } catch (IOException e) {
            logger.error("Error closing connection", e);
        }
    }

    @Override
    public void run() {
        connected = true;
        while (connected) {
            try (Socket socket = new Socket(serverHost, serverPort)) {

                DataInputStream inputStream = new DataInputStream(socket.getInputStream());
                DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());

                this.socket = socket;
                this.inputStream = inputStream;
                this.outputStream = outputStream;
                this.connected = true;

                logger.info("Connected to server at {}:{}", serverHost, serverPort);

                while (connected) {
                    int length = inputStream.readInt();
                    byte[] payload = new byte[length];
                    inputStream.readFully(payload);
                    Message message = parser.parseMessage(new ByteArrayInputStream(payload));
                    if (message != null) {
                        UUID messageId = message.messageId();
                        if (pendingRequests.containsKey(messageId)) {
                            pendingRequests.remove(messageId).complete(message);
                        } else {
                            logger.warn("Received unsolicited message with id {} and action {}", messageId, message.actionType());
                        }
                    } else {
                        logger.warn("Received invalid message from server");
                    }
                }
            } catch (IOException e) {
                logger.error("IO error in server connection", e);
                if (reconnectAttempts == 0) {
                    shutdown();
                    return;
                }
                logger.warn("Attempting to reconnect... ({} attempts remaining)", reconnectAttempts);
                reconnectAttempts--;
            } catch (CommException e) {
                logger.error("Protocol error in server connection", e);
            }
        }
    }

    public void shutdown() {
        logger.info("Shutting down server connection");
        persistSession();
        connected = false;
    }

    private void persistSession() {

    }

    public ClientSessionManager getSessionManager() {
        return sessionManager;
    }

    public void setSessionManager(ClientSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public ClientSearchHandler getSearchHandler() {
        return searchHandler;
    }
}
