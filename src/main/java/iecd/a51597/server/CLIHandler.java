package iecd.a51597.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Consumer;

public class CLIHandler {

    public boolean running = false;
    public Scanner scanner = new Scanner(System.in);

    private Logger logger = LogManager.getLogger(CLIHandler.class);
    private Logger consoleLogger = LogManager.getLogger("iecd.a51597.server.consoleOutput");

    private final Map<String, Consumer<String[]>> commands = new HashMap<>();

    public CLIHandler() {
        commands.put("help", this::help);
        commands.put("start", this::start);
        commands.put("stop", this::stop);
    }

    void loop() {
        running = true;
        while(running) {
            consoleLogger.info(">> ");
            handleCommand(scanner.nextLine());
        }
    }

    private void handleCommand(String input) {
        String[] parts = input.trim().split("\\s+");
        String name = parts[0].toLowerCase();
        String[] args = Arrays.copyOfRange(parts, 1, parts.length);

        Consumer<String[]> command = commands.get(name);
        if(command == null) {
            consoleLogger.warn("Unknown command: {}\n", name);
        } else {
            try {
                command.accept(args);
            } catch (Exception e) {
                consoleLogger.error("Error executing command '{}': {}\n", name, e.getMessage());
            }
        }
    }

    private void help(String[] args) {
        consoleLogger.info("test\n");
    }

    private void start(String[] args) {
        Server.startListener();
        consoleLogger.info("Server started listening for connections on port: {}\n", Server.port);
    }

    private void stop(String[] args) {
        Server.stopListener();
        consoleLogger.info("Server stopped listening for connections\n");
    }

}
