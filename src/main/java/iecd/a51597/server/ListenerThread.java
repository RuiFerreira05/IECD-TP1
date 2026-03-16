package iecd.a51597.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;

public class ListenerThread extends Thread {

    public volatile boolean running = true;
    private final int port;
    private final Server server;
    private final Logger logger = LogManager.getLogger(ListenerThread.class);
    private ServerSocket serverSocket;

    public ListenerThread(int port, Server server) {
        this.port = port;
        this.server = server;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)){
            this.serverSocket = serverSocket;
            while (running) {
                Connection conn = new Connection(serverSocket.accept(), server, server.getSessionManager(), server.getMessageHandler());
                server.addConnection(conn);
                new Thread(conn).start();

                logger.info("New connection established from IP: {}", conn.getClientSocket().getInetAddress().getHostAddress());
                logger.info("Total Connections: {}", server.getConnections().size());
            }
        } catch (IOException e) {
            if (running) {
                logger.error("Error in ListenerThread: {}", e.getMessage());
            }
        } finally {
            running = false;
        }
    }

    public int getPort() {
        return port;
    }

    public void stopListener() {
        running = false;
        try { if (serverSocket != null) serverSocket.close(); }
        catch (IOException e) { logger.error("Error closing socket: {}", e.getMessage()); }
        logger.info("Stopping ListenerThread");
    }
}
