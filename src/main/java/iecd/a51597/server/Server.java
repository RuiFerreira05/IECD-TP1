package iecd.a51597.server;

import iecd.a51597.server.game.GameFactory;
import iecd.a51597.server.protocol.builders.MessageBuilder;
import iecd.a51597.server.protocol.builders.XMLMessageBuilder;
import iecd.a51597.server.protocol.parsers.CommParser;
import iecd.a51597.server.protocol.parsers.XMLParser;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.ArrayList;
import java.util.List;

public class Server {

    private static volatile Server instance;

    private int startupPort = 5555;
    private ListenerThread listener;
    private final SessionManager sessionManager = new SessionManager();
    private final CommParser commParser;
    private final MessageBuilder messageBuilder;
    private final MessageHandler messageHandler;
    private final GameManager gameManager = new GameManager();

    private final CLIHandler cliHandler;
    private final Logger logger = LogManager.getLogger(Server.class);

    // Probably should be a CopyOnWriteArrayList, but I have no experience with it
    private final List<Connection> connections = new ArrayList<>();

    private Server() {
        logger.info("Initializing Server...");
        this.cliHandler = new CLIHandler(this);
        this.messageBuilder = new XMLMessageBuilder();
        this.commParser = new XMLParser();
        this.messageHandler = new MessageHandler(commParser, messageBuilder, sessionManager);

        // registerGameFactory(GAME GOES HERE BRO);
    }

    public void registerGameFactory(GameFactory factory) {
        gameManager.registerFactory(factory);
    }

    public MessageHandler getMessageHandler() {
        return messageHandler;
    }

    public ListenerThread getListener() {
        return listener;
    }

    public MessageBuilder getMessageBuilder() {
        return messageBuilder;
    }

    public CommParser getCommParser() {
        return commParser;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
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
            return List.copyOf(connections);
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
                this.startupPort = Integer.parseInt(args[0]);
                logger.info("Server port assigned to: {}", this.startupPort);
            }
        } catch (NumberFormatException e) {
            logger.error("Invalid port number: {}", e.getMessage());
        }
    }

    void loop() {
        Thread cliThread = new Thread(cliHandler::loop);
        this.startListener();
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
        logger.info("Starting Listener thread with default port: {}", this.startupPort);
        startListener(this.startupPort);
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
        synchronized (connections) { connections.forEach(Connection::closeConnection); }
        logger.info("Server shutdown complete");
    }

    public int getStartupPort() {
        return this.startupPort;
    }
}