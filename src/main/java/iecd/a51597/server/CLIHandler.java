package iecd.a51597.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class CLIHandler {

    private boolean running = false;

    private final Logger logger = LogManager.getLogger(CLIHandler.class);

    private final Map<String, Command> commands = new HashMap<>();

    public CLIHandler() {
        commands.put("help", new Command(this::help, "Show this help message"));
        commands.put("start", new Command(this::start, "Start the server"));
        commands.put("stop", new Command(this::stop, "Stop the server"));
        commands.put("exit", new Command(this::exit, "Shutdown the server"));
    }

    void loop() {
        running = true;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            while (running) {
                System.out.print(">> ");
                String line = reader.readLine();
                if (line == null) break;
                handleCommand(line);
            }
        } catch (IOException e) {
            if (running) {
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
        int port = Server.port;

        if (args.length != 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port number: " + args[0]);
                logger.error("Invalid port number in start command: {}", args[0]);
                return;
            }
        }

        if (!Server.isListening()) {
            Server.startListener(port);
            System.out.println("Server started on port: " + port);
            logger.info("Server started on port: {}", port);
        } else {
            System.out.println("Server is already listening for connections");
        }
    }

    private void stop(String[] args) {
        if (Server.isListening()) {
            Server.stopListener();
            System.out.println("Server stopped listening for connections");
            logger.info("Server stopped listening for connections");
        } else  {
            System.out.println("Server is already not listening for connections");
        }
    }

    public void exit(String[] args) {
        running = false;
        logger.info("Server shutting down");
        System.out.println("Shutting down server...");
        Server.shutdown();
    }
}

record Command(Consumer<String[]> action, String description) {
    void execute(String[] args) { action.accept(args); }
}
