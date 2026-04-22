package iecd.a51597.client;

import iecd.a51597.client.cli.ClientCliHandler;
import iecd.a51597.client.config.ClientConfiguration;
import iecd.a51597.client.network.ServerConnection;
import iecd.a51597.client.session.ClientSessionManager;
import iecd.a51597.common.protocol.builders.client.XMLClientMessageBuilder;
import iecd.a51597.common.protocol.parsers.XMLParser;

/**
 * Client-side entry point.
 */
public class Client {

    private final ServerConnection serverConnection;
    private final ClientCliHandler cliHandler;
    private final ClientSessionManager sessionManager;
    private static volatile Client instance;


    private Client() {
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
        cliHandler.running = false;
        serverConnection.shutdown();
    }

    public ServerConnection getServerConnection() {
        return serverConnection;
    }

    public ClientSessionManager getSessionManager() {
        return sessionManager;
    }

    public static void main(String[] args) {
        Client client = Client.getInstance();
        Thread connectionThread = new Thread(client.serverConnection, "client-server-connection");
        connectionThread.setDaemon(true);
        connectionThread.start();
        client.cliHandler.loop();
    }
}
