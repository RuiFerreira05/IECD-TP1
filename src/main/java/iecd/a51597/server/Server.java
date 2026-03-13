package iecd.a51597.server;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class Server {

    int port = 5555;
    ListenerThread listener;

    CLIHandler cliHandler = new CLIHandler(this);

    Logger logger = LogManager.getLogger(Server.class);

    static void main(String[] args) {
        Server server = new Server();
        server.handleCLIParams(args);
        server.init();
        server.loop();
    }

    void handleCLIParams(String[] args) {
        try {
            if (args.length > 0) {
                this.port = Integer.parseInt(args[0]);
                logger.info("Server port assigned to: {}", this.port);
            }
        } catch (NumberFormatException e) {
            logger.error("Invalid port number: {}", e.getMessage());
        }
    }

    void init() {
        this.logger.info("Initializing Server...");

        this.listener = new ListenerThread(this);
    }

    void loop() {
//        this.cliHandler.loop();
    }

}
