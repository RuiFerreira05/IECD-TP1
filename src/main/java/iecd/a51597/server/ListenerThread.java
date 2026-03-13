package iecd.a51597.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.ServerSocket;

public class ListenerThread extends Thread {

    public boolean running = false;
    private final Logger logger;

    public ListenerThread() {
        this.logger = LogManager.getLogger(ListenerThread.class);
    }

    @Override
    public void run() {
        running = true;
        try (ServerSocket serverSocket = new ServerSocket(Server.port)) {
            while (running) {
                Connection conn = new Connection(serverSocket.accept());
                Server.connections.add(conn);
                conn.start();

//                Server.logger.info("New connection established from IP: {}", ); TODO: Log IP address of the new connection
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
