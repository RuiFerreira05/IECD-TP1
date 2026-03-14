package iecd.a51597.server;

import java.io.IOException;
import java.net.Socket;

public class Connection implements Runnable {

    private final Socket clientSocket;
    private final Server server;

    public Connection(Socket client, Server server) {
        this.clientSocket = client;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            // TODO: protocol logic
        } finally {
            try { clientSocket.close(); } catch (IOException ignored) {
                server.removeConnection(this);
            }
        }
    }

    public Socket getClientSocket() {
        return clientSocket;
    }
}
