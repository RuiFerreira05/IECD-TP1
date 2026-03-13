package iecd.a51597.server;

public class ListenerThread extends Thread {

    public Server server;

    public ListenerThread(Server server) {
        this.server = server;
    }

    @Override
    public void run() {
        System.out.println("Listening for server connection...");
    }

}
