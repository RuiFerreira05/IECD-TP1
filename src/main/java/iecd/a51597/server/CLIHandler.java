package iecd.a51597.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class CLIHandler {

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());

    private final Server  server;
    private final Instant startedAt = Instant.now();
    private boolean running = false;

    private final Logger logger = LogManager.getLogger(CLIHandler.class);

    private final Map<String, Command> commands = new HashMap<>();

    public CLIHandler(Server server) {
        this.server = server;
        commands.put("help", new Command(this::help, "Show this help message"));
        commands.put("status", new Command(this::status, "Print server status header"));
        commands.put("start", new Command(this::start, "Start the server"));
        commands.put("stop", new Command(this::stop, "Stop the server"));
        commands.put("exit", new Command(this::exit, "Shutdown the server"));
    }

    void loop() {
        running = true;
        printStatusHeader();
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

    public void printStatusHeader() {
        final int W = ServerConfiguration.STATUS_BOX_WIDTH;

        boolean listening = server.isListening();
        int connections = server.getConnections().size();
        int sessions = server.getSessionManager().activeSessionCount();
        Instant now = Instant.now();

        String statusLine = listening
                ? "● LISTENING  (port " + server.getListener().getPort() + ")"
                : "○ IDLE";

        Duration uptime = Duration.between(startedAt, now);
        String uptimeLine = String.format("%dd %02dh %02dm %02ds",
                uptime.toDaysPart(),
                uptime.toHoursPart(),
                uptime.toMinutesPart(),
                uptime.toSecondsPart());

        String timeLine = DATE_FMT.format(now) + "  " + TIME_FMT.format(now);

        String border = "═".repeat(W);
        String top = "╔" + border + "╗";
        String mid = "╠" + border + "╣";
        String bot = "╚" + border + "╝";
        String title = centre("IECD-TP1 - SERVER STATUS", W);

        System.out.println(top);
        System.out.println("║" + title + "║");
        System.out.println(mid);
        System.out.println(row("Status", statusLine, W));
        System.out.println(row("Connections", String.valueOf(connections), W));
        System.out.println(row("Sessions", String.valueOf(sessions), W));
        System.out.println(row("Uptime", uptimeLine, W));
        System.out.println(row("Time", timeLine, W));
        System.out.println(bot);
    }

    private static String centre(String text, int width) {
        int padding = width - text.length();
        int left    = padding / 2;
        int right   = padding - left;
        return " ".repeat(left) + text + " ".repeat(right);
    }

    private static String row(String label, String value, int innerWidth) {
        String content = String.format("  %-12s: %s", label, value);
        if (content.length() < innerWidth) {
            content = content + " ".repeat(innerWidth - content.length());
        } else if (content.length() > innerWidth) {
            content = content.substring(0, innerWidth);
        }
        return "║" + content + "║";
    }

    private void handleCommand(String input) {
        String[] parts = input.trim().split("\\s+");
        String   name  = parts[0].toLowerCase();
        String[] args  = Arrays.copyOfRange(parts, 1, parts.length);

        Command command = commands.get(name);
        if (command == null) {
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

    private void status(String[] args) {
        printStatusHeader();
    }

    private void start(String[] args) {
        int port = server.getStartupPort();

        if (args.length != 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port number: " + args[0]);
                logger.error("Invalid port number in start command: {}", args[0]);
                return;
            }
        }

        if (!server.isListening()) {
            server.startListener(port);
            System.out.println("Server started on port: " + port);
            logger.info("Server started on port: {}", port);
        } else {
            System.out.println("Server is already listening for connections");
        }
    }

    private void stop(String[] args) {
        if (server.isListening()) {
            server.stopListener();
            System.out.println("Server stopped listening for connections");
            logger.info("Server stopped listening for connections");
        } else  {
            System.out.println("Server is already not listening for connections");
        }
    }

    private void exit(String[] args) {
        running = false;
        logger.info("Server shutting down");
        System.out.println("Shutting down Server...");
        server.shutdown();
    }

    private record Command(Consumer<String[]> action, String description) {
        void execute(String[] args) { action.accept(args); }
    }
}