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
        while (running) {
            try (ServerSocket serverSocket = new ServerSocket(Server.port)) {
                Connection conn = new Connection(serverSocket.accept());

                Server.logger.info("New connection established.");
            } catch (Exception e) {
                logger.error("Error in ListenerThread: {}", e.getMessage());
            }
        }
    }
}
