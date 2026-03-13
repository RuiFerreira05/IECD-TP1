package iecd.a51597.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.ServerSocket;

public class ListenerThread extends Thread {

    public boolean running = false;
    private final int port;
    private final Logger logger = LogManager.getLogger(ListenerThread.class);

    public ListenerThread(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        running = true;
        try (ServerSocket serverSocket = new ServerSocket(this.port)) {
            while (running) {
                Connection conn = new Connection(serverSocket.accept());
                Server.connections.add(conn);
                conn.start();

                Server.logger.info("New connection established from IP: {}", conn.getClientSocket().getInetAddress().getHostAddress());
                Server.logger.info("Total Connections: {}", Server.connections.size());
            }
        } catch (Exception e) {
            logger.error("Error in ListenerThread: {}", e.getMessage());
        }
    }

    public void stopListener() {
        running = false;
        logger.info("Stopping ListenerThread");
    }
}
