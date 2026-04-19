package iecd.a51597.server;

import iecd.a51597.server.cli.CLIHandler;
import iecd.a51597.server.config.ServerConfiguration;
import iecd.a51597.server.game.GameFactory;
import iecd.a51597.server.game.GameManager;
import iecd.a51597.server.handlers.*;
import iecd.a51597.server.network.Connection;
import iecd.a51597.server.network.ListenerThread;
import iecd.a51597.server.persistence.PersistenceManager;
import iecd.a51597.server.protocol.builders.MessageBuilder;
import iecd.a51597.server.protocol.builders.XMLMessageBuilder;
import iecd.a51597.server.protocol.parsers.CommParser;
import iecd.a51597.server.protocol.parsers.XMLParser;
import iecd.a51597.server.session.SessionManager;
import iecd.a51597.server.store.Leaderboard;
import iecd.a51597.server.store.UserStore;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.ArrayList;
import java.util.List;

public class Server {

    private static volatile Server instance;

    private int port;
    private ListenerThread listener;
    private final SessionManager sessionManager = new SessionManager();
    private final CommParser commParser;
    private final MessageBuilder messageBuilder;
    private final MessageHandler messageHandler;
    private final GameManager gameManager = new GameManager();
    private final UserStore userStore;
    private final Leaderboard leaderboard;
    private final PersistenceManager persistenceManager;

    private final CLIHandler cliHandler;
    private final Logger logger = LogManager.getLogger(Server.class);

    private final List<Connection> connections = new ArrayList<>();

    private Server() {
        logger.info("Initializing Server...");
        ServerConfiguration.load();

        this.port = ServerConfiguration.DEFAULT_PORT;
        this.cliHandler = new CLIHandler(this);
        this.messageBuilder = new XMLMessageBuilder();
        this.commParser = new XMLParser();
        this.userStore = new UserStore();
        this.leaderboard = new Leaderboard(userStore);
        this.persistenceManager = new PersistenceManager(userStore);

        persistenceManager.load();

        AuthHandler authHandler = new AuthHandler(messageBuilder, sessionManager, userStore);
        ProfileHandler profileHandler = new ProfileHandler(messageBuilder, sessionManager, userStore);
        SearchHandler searchHandler = new SearchHandler(messageBuilder, userStore);
        GameHandler gameHandler = new GameHandler(messageBuilder, sessionManager, userStore, gameManager);

        this.messageHandler = new MessageHandler(commParser, messageBuilder, authHandler, profileHandler, searchHandler, gameHandler);

        // registerGameFactory(GAME GOES HERE);
    }

    public void registerGameFactory(GameFactory factory) {
        gameManager.registerFactory(factory);
    }

    public MessageHandler getMessageHandler() { return messageHandler; }
    public ListenerThread getListener() { return listener; }
    public MessageBuilder getMessageBuilder() { return messageBuilder; }
    public CommParser getCommParser() { return commParser; }
    public SessionManager getSessionManager() { return sessionManager; }
    public UserStore getUserStore() { return userStore; }
    public GameManager getGameManager() { return gameManager; }

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

    public List<Connection> getConnections() {
        synchronized (connections) { return List.copyOf(connections); }
    }

    public void addConnection(Connection connection) {
        synchronized (connections) { connections.add(connection); }
    }

    public void removeConnection(Connection connection) {
        synchronized (connections) { connections.remove(connection); }
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
        this.startListener();
        cliThread.start();
    }

    public void startListener(int port) {
        if (!this.isListening()) {
            listener = new ListenerThread(port, this);
            listener.start();
            logger.info("Server listening on port: {}", port);
        }
    }

    public void startListener() {
        logger.info("Starting Listener thread with default port: {}", this.port);
        startListener(this.port);
    }

    public void stopListener() {
        if (this.isListening()) {
            this.listener.stopListener();
            logger.info("Server stopping listener");
        }
    }

    public boolean isListening() {
        return this.listener != null && this.listener.isRunning();
    }

    public void shutdown() {
        stopListener();
        persistenceManager.save();

        // The reason we take a snapshot of the connections list here is to avoid a ConcurrentModificationException when
        // Connection calls server.removeConnection(). It's a little ugly but it works
        List<Connection> snapshot;
        synchronized (connections) { snapshot = List.copyOf(connections); }
        snapshot.forEach(Connection::closeConnection);
        logger.info("Server shutdown complete");
    }

    public int getStartupPort() { return this.port; }

    public Leaderboard getLeaderboard() {
        return leaderboard;
    }

    public static void main(String[] args) {
        Server server = Server.getInstance();
        server.handleCLIParams(args);
        server.loop();
    }
}