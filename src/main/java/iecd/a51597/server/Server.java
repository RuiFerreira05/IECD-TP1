package iecd.a51597.server;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.ArrayList;
import java.util.List;

public class Server {

    static int port = 5555;
    static ListenerThread listener;

    private static final CLIHandler cliHandler = new CLIHandler();

    private static final Logger logger = LogManager.getLogger(Server.class);

    private static final List<Connection> connections = new ArrayList<>();

    public static void main(String[] args) {
        Server.init(args);
        Server.loop();
    }

    public static List<Connection> getConnections() {
        synchronized (connections) {
            return connections;
        }
    }

    public static void addConnection(Connection connection) {
        synchronized (connections) {
            connections.add(connection);
        }
    }

    static private void handleCLIParams(String[] args) {
        try {
            if (args.length > 0) {
                Server.port = Integer.parseInt(args[0]);
                logger.info("Server port assigned to: {}", Server.port);
            }
        } catch (NumberFormatException e) {
            logger.error("Invalid port number: {}", e.getMessage());
        }
    }

    static void init(String[] args) {
        handleCLIParams(args);
        Server.logger.info("Initializing Server...");
    }

    static void loop() {
        Thread cliThread = new Thread(cliHandler::loop);
        cliThread.start();
    }

    public static void startListener(int port) {
        if (!Server.isListening()) {
            listener = new ListenerThread(port);
            listener.start();
            Server.logger.info("Server listening on port: {}", Server.port);
        }
    }

    public static void startListener() {
        startListener(Server.port);
    }

    public static void stopListener() {
        if (Server.isListening()) {
            Server.listener.stopListener();
            logger.info("Server stopping listener");
        }
    }

    public static boolean isListening() {
        if (Server.listener != null) {
            return Server.listener.running;
        }

        return false;
    }

    public static void shutdown() {
        stopListener();
        //TODO: save state
        //TODO: close connections
        System.exit(0);
    }
}
