package iecd.a51597.server;

import iecd.a51597.server.handlers.MessageHandler;
import iecd.a51597.server.protocol.ProtocolConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

public class Connection implements Runnable {

    private final Socket clientSocket;
    private final Server server;
    private final Logger logger = LogManager.getLogger(Connection.class);

    private DataInputStream  inputStream;
    private DataOutputStream outputStream;

    private final MessageHandler messageHandler;

    public Connection(Socket client, Server server, MessageHandler messageHandler) {
        this.clientSocket   = client;
        this.server         = server;
        this.messageHandler = messageHandler;
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
            messageHandler.handle(frameBytes, this);

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

    public void sendMessage(byte[] payload) {
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