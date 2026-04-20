package iecd.a51597.client;

import iecd.a51597.client.cli.ClientCliHandler;
import iecd.a51597.client.config.ClientConfiguration;

/**
 * Client-side entry point.
 */
public class Client {

    private final ClientCliHandler cliHandler;
    private static volatile Client instance;
    public String serverUrl;

    private Client() {
        ClientConfiguration.load();
        serverUrl = ClientConfiguration.SERVER_URL;
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
        // TODO: rest of the shutdown procedure
    }

    public static void main(String[] args) {
        Client client = Client.getInstance();
        client.cliHandler.loop();
    }
}
