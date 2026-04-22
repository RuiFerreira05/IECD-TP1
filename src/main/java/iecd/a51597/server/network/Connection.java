package iecd.a51597.server.network;

import iecd.a51597.server.Server;
import iecd.a51597.server.config.ServerConfiguration;
import iecd.a51597.server.handlers.MessageDispatcher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

/**
 * Stateful TCP connection wrapper responsible for framed I/O and dispatch.
 */
public class Connection implements Runnable {

    private final Socket clientSocket;
    private final Server server;
    private final Logger logger = LogManager.getLogger(Connection.class);

    private DataInputStream  inputStream;
    private DataOutputStream outputStream;

    private final MessageDispatcher messageDispatcher;

    /**
     * Creates a connection wrapper and initializes data streams.
     *
     * @param client accepted socket
     * @param server owning server instance
     * @param messageDispatcher frame handler
     */
    public Connection(Socket client, Server server, MessageDispatcher messageDispatcher) {
        this.clientSocket   = client;
        this.server         = server;
        this.messageDispatcher = messageDispatcher;
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

    /**
     * Blocking read loop for framed inbound messages.
     */
    @Override
    public void run() {
        try {
            while (!clientSocket.isClosed()) {
                readIncomingMessage();
            }
        } finally {
            closeConnection();
        }
    }

    private void readIncomingMessage() {
        try {
            int length = inputStream.readInt();

            if (length <= 0 || length > ServerConfiguration.MAX_FRAME_SIZE) {
                logger.warn("Invalid frame length {} from {}, closing", length, clientSocket.getInetAddress());
                closeConnection();
                return;
            }

            byte[] frameBytes = new byte[length];
            inputStream.readFully(frameBytes);
            messageDispatcher.handle(frameBytes, this);

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
        }
    }

    /**
     * Sends one framed payload to the client.
     *
     * @param payload serialized protocol payload bytes
     */
    public void sendMessage(byte[] payload) {
        if (payload == null) {return;}
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

    /**
     * @return underlying client socket
     */
    public Socket getClientSocket() {
        return clientSocket;
    }

    /**
     * Closes this connection and performs related cleanup.
     */
    public void closeConnection() {
        if (clientSocket.isClosed()) return;
        server.removeConnection(this);
        server.getSessionManager().invalidateByConnection(this);
        try {
            clientSocket.close();
        } catch (IOException e) {
            logger.error("Error closing connection: {}", e.getMessage());
        }
    }
}