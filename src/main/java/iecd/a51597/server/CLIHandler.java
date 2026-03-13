package iecd.a51597.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

public class CLIHandler {

    public boolean running = false;
    public Scanner scanner = new Scanner(System.in);

    private Logger logger = LogManager.getLogger(CLIHandler.class);

    private final Map<String, Consumer<String[]>> commands = new HashMap<>();

    public CLIHandler() {
        commands.put("help", args -> {
            help(args);
        });
    }

    void loop() {
        running = true;
        while(running) {
            logger.info(">> ");
            handleCommand(scanner.nextLine());
        }
    }

    private void handleCommand(String input) {
        String[] parts = input.trim().split("\\s+");
        String name = parts[0].toLowerCase();
        String[] args = Arrays.copyOfRange(parts, 1, parts.length);

        Consumer<String[]> command = commands.get(name);
        if(command == null) {
            logger.warn("Unknown command: {}\n", name);
        } else {
            try {
                command.accept(args);
            } catch (Exception e) {
                logger.error("Error executing command '{}': {}\n", name, e.getMessage());
            }
        }
    }

    private void help(String[] args) {
        logger.info("test\n");
    }
}
