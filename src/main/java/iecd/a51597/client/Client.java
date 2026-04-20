package iecd.a51597.client;

import iecd.a51597.client.cli.ClientCliHandler;

/**
 * Client-side entry point.
 */
public class Client {

    private final ClientCliHandler cliHandler;
    private volatile Client instance;

    private Client() {
        cliHandler = new ClientCliHandler(this);
    }

    public Client getInstance() {
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
        System.exit(0);
    }

    static void main() {
        Client client = new Client().getInstance();
        client.cliHandler.loop();
    }
}
