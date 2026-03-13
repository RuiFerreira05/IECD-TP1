package iecd.a51597.server;

import java.net.Socket;

public class Connection extends Thread {

    private Socket clientSocket;

    public Connection(Socket client) {
        this.clientSocket = client;
    }

    @Override
    public void run() {

    }
}
