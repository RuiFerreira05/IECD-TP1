package iecd.a51597.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Consumer;

public class CLIHandler {

    public boolean running = false;
    public Scanner scanner = new Scanner(System.in);

    private BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

    private Logger logger = LogManager.getLogger(CLIHandler.class);

    private final Map<String, Command> commands = new HashMap<>();

    public CLIHandler() {
        commands.put("help", new Command(this::help, "Show this help message"));
        commands.put("start", new Command(this::start, "Start the server"));
        commands.put("stop", new Command(this::stop, "Stop the server"));
        commands.put("exit", new Command(this::exit, "Shutdown the server"));
    }

    void loop() {
        running = true;
        while (running) {
            try {
                System.out.print(">> ");
                String line = reader.readLine();
                if (line == null) break;
                handleCommand(line);
            } catch (IOException e) {
                if (!running) break;
                logger.error("CLI read error: {}", e.getMessage());
            }
        }
    }

    private void handleCommand(String input) {
        String[] parts = input.trim().split("\\s+");
        String name = parts[0].toLowerCase();
        String[] args = Arrays.copyOfRange(parts, 1, parts.length);

        Command command = commands.get(name);
        if(command == null) {
            System.out.println("Unknown command: " + name);
            System.out.println("Type 'help' for available commands.");
            logger.warn("Unknown command entered: {}", name);
        } else {
            try {
                command.execute(args);
            } catch (Exception e) {
                System.out.println("Error executing command '" + name + "': " + e.getMessage());
                logger.error("Error executing command '{}': {}", name, e.getMessage());
            }
        }
    }

    private void help(String[] args) {
        commands.forEach((name, cmd) ->
                System.out.printf("%-10s -> %s\n", name, cmd.description())
        );
    }

    private void start(String[] args) {
        Server.startListener();
        System.out.println("Server started listening for connections on port: " + Server.port);
        logger.info("Server started listening for connections on port: {}", Server.port);
    }

    private void stop(String[] args) {
        Server.stopListener();
        System.out.println("Server stopped listening for connections");
        logger.info("Server stopped listening for connections");
    }

    public void exit(String[] args) {
        running = false;
        Server.shutdown();
        logger.info("Server shutting down");
        System.out.println("Shutting down server...");
    }
}

record Command(Consumer<String[]> action, String description) {
    void execute(String[] args) { action.accept(args); }
}
