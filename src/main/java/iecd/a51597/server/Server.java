package iecd.a51597.server;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.ArrayList;
import java.util.List;

public class Server {

    static int port = 5555;
    static ListenerThread listener;

    static CLIHandler cliHandler = new CLIHandler();

    static Logger logger = LogManager.getLogger(Server.class);

    static List<Connection> connections = new ArrayList<Connection>();

    public static void main(String[] args) {
        Server.init(args);
        Server.loop();
    }

    static private void handleCLIParams(String[] args) {
        try {
            if (args.length > 0) {
                Server.port = Integer.parseInt(args[0]);
                logger.info("Server port assigned to: {}", Server.port);
            }
        } catch (NumberFormatException e) {
            logger.error("Invalid port number: {}", e.getMessage());
        }
    }

    static void init(String[] args) {
        handleCLIParams(args);
        Server.logger.info("Initializing Server...");

        Server.listener = new ListenerThread();
    }

    static void loop() {
        Server.cliHandler.loop();
    }

    public static void startListener() {
        Server.listener.start();
        Server.logger.info("Server listening on port: {}", Server.port);
    }

    public static void stopListener() {
        Server.listener.stopListener();
        logger.info("Server stopping listener");
    }
}
