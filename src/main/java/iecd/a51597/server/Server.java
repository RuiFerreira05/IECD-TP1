package iecd.a51597.server;

public class Server {

    ListenerThread listener;
    int port = 5555;
    CLIHandler cliHandler;

    static void main(String[] args) {
        Server server = new Server();
        server.handleCLIParams(args);
        server.init();
    }

    void handleCLIParams(String[] args) {
        try {
            if (args.length > 0) {
                this.port = Integer.parseInt(args[0]);
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid port number. Using default port: " + this.port);
        }
    }

    void init() {
        this.listener = new ListenerThread(this);
        this.CLIHandler = new CLIHandler(this);
    }

    void loop() {
        this.cliHandler.start();
    }

}
