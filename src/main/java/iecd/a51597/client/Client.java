package iecd.a51597.client;

import iecd.a51597.client.cli.ClientCliHandler;
import iecd.a51597.client.config.ClientConfiguration;
import iecd.a51597.client.game.GameController;
import iecd.a51597.client.network.ServerConnection;
import iecd.a51597.client.session.ClientSessionManager;
import iecd.a51597.common.protocol.MessageBody;
import iecd.a51597.common.protocol.builders.client.XMLClientMessageBuilder;
import iecd.a51597.common.protocol.parsers.XMLParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Client-side entry point.
 */
public class Client {

    private final ServerConnection serverConnection;
    private final ClientCliHandler cliHandler;
    private final ClientSessionManager sessionManager;
    private static volatile Client instance;
    private final List<MessageBody.GameInvitePush> pendingInvites = new CopyOnWriteArrayList<>();

    private static final Logger logger = LogManager.getLogger(Client.class);

    private Client() {
        logger.info("Initializing Client Configuration");
        ClientConfiguration.load();
        serverConnection = new ServerConnection(
                this,
                ClientConfiguration.SERVER_IP,
                ClientConfiguration.SERVER_PORT,
                new XMLParser(),
                new XMLClientMessageBuilder()
        );
        sessionManager = new ClientSessionManager(serverConnection);
        serverConnection.setSessionManager(sessionManager);

        cliHandler = new ClientCliHandler(this);
        logger.info("Client bootstrapping complete");
    }

    public static Client getInstance() {
        if (instance == null) {
            synchronized (Client.class) {
                if (instance == null) {
                    instance = new Client();
                }
            }
        }
        return instance;
    }

    public void exit() {
        logger.info("Shutting down client");
        cliHandler.running = false;
        serverConnection.shutdown();
    }

    public ServerConnection getServerConnection() {
        return serverConnection;
    }

    public ClientSessionManager getSessionManager() {
        return sessionManager;
    }

    public ClientCliHandler getCliHandler() {
        return cliHandler;
    }

    public List<MessageBody.GameInvitePush> getPendingInvites() {
        return pendingInvites;
    }

    public static void main(String[] args) {
        Client client = Client.getInstance();
        Thread connectionThread = new Thread(client.serverConnection, "client-server-connection");
        connectionThread.setDaemon(true);
        connectionThread.start();
        client.cliHandler.loop();
    }
}
