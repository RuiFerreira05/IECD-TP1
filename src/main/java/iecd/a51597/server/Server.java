package iecd.a51597.server;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.ArrayList;
import java.util.List;

public class Server {

    private static volatile Server instance;

    private int port = 5555;
    private ListenerThread listener;

    private final CLIHandler cliHandler;
    private final Logger logger = LogManager.getLogger(Server.class);
    private final List<Connection> connections = new ArrayList<>();

    private Server() {
        logger.info("Initializing Server...");
        this.cliHandler = new CLIHandler(this);
    }

    public static Server getInstance() {
        if (instance == null) {
            synchronized (Server.class) {
                if (instance == null) {
                    instance = new Server();
                }
            }
        }
        return instance;
    }

    public static void main(String[] args) {
        Server server = Server.getInstance();
        server.handleCLIParams(args);
        server.loop();
    }

    public List<Connection> getConnections() {
        synchronized (connections) {
            return connections;
        }
    }

    public void addConnection(Connection connection) {
        synchronized (connections) {
            connections.add(connection);
        }
    }

    public void removeConnection(Connection connection) {
        synchronized (connections) {
            connections.remove(connection);
        }
    }

    private void handleCLIParams(String[] args) {
        try {
            if (args.length > 0) {
                this.port = Integer.parseInt(args[0]);
                logger.info("Server port assigned to: {}", this.port);
            }
        } catch (NumberFormatException e) {
            logger.error("Invalid port number: {}", e.getMessage());
        }
    }

    void loop() {
        Thread cliThread = new Thread(cliHandler::loop);
        cliThread.start();
    }

    public void startListener(int port) {
        if (!this.isListening()) {
            listener = new ListenerThread(port, this);
            logger.info("Server creating new Listener Thread instance");
            listener.start();
            logger.info("Server listening on port: {}", port);
        }
    }

    public void startListener() {
        startListener(this.port);
    }

    public void stopListener() {
        if (this.isListening()) {
            this.listener.stopListener();
            logger.info("Server stopping listener");
        }
    }

    public boolean isListening() {
        if (this.listener != null) {
            return this.listener.running;
        }

        return false;
    }

    public void shutdown() {
        stopListener();
        //TODO: save state
        //TODO: close connections
    }

    public int getPort() {
        return this.port;
    }

    public ListenerThread getListener() {
        return listener;
    }
}