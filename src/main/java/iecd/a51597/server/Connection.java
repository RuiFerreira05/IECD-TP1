package iecd.a51597.server;

import java.io.IOException;
import java.net.Socket;

public class Connection extends Thread {

    private final Socket clientSocket;

    public Connection(Socket client) {
        this.clientSocket = client;
    }

    @Override
    public void run() {
        try {
            // TODO: protocol logic
        } finally {
            try { clientSocket.close(); } catch (IOException ignored) {}
        }
    }

    public Socket getClientSocket() {
        return clientSocket;
    }
}
