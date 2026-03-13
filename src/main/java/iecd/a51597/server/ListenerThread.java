package iecd.a51597.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;

public class ListenerThread extends Thread {

    public volatile boolean running = false;
    private final int port;
    private final Logger logger = LogManager.getLogger(ListenerThread.class);
    private ServerSocket serverSocket;

    public ListenerThread(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        running = true;
        try (ServerSocket serverSocket = new ServerSocket(port)){
            this.serverSocket = serverSocket;
            while (running) {
                Connection conn = new Connection(serverSocket.accept());
                Server.addConnection(conn);
                conn.start();

                logger.info("New connection established from IP: {}", conn.getClientSocket().getInetAddress().getHostAddress());
                logger.info("Total Connections: {}", Server.getConnections().size());
            }
        } catch (IOException e) {
            if (running) {
                logger.error("Error in ListenerThread: {}", e.getMessage());
            }
        } finally {
            running = false;
        }
    }

    public void stopListener() {
        running = false;
        try { if (serverSocket != null) serverSocket.close(); }
        catch (IOException e) { logger.error("Error closing socket: {}", e.getMessage()); }
        logger.info("Stopping ListenerThread");
    }
}
