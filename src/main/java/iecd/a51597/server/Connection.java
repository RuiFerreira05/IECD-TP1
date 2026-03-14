package iecd.a51597.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class Connection implements Runnable {

    private final Socket clientSocket;
    private final Server server;
    private final Logger logger = LogManager.getLogger();
    private InputStream  inputStream;
    private OutputStream outputStream;

    public Connection(Socket client, Server server) {
        this.clientSocket = client;
        this.server = server;
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
            // TODO: protocol logic
        } finally {
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
