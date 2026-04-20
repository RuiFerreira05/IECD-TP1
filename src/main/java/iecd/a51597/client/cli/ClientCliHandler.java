package iecd.a51597.client.cli;

import iecd.a51597.client.Client;
import iecd.a51597.client.cli.screens.impl.MainMenuScreen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Interactive administrative command-line interface for runtime server control.
 */
public class ClientCliHandler {

    private final Client client;
    public volatile Boolean running;
    private final StateMachine stateMachine;

    private final Logger logger = LogManager.getLogger(ClientCliHandler.class);

    public ClientCliHandler(Client client) {
        this.client = client;
        this.stateMachine = new StateMachine(this);
        this.stateMachine.registerScreen("main", new MainMenuScreen(stateMachine, client));
    }

    public void loop() {
        running = true;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            stateMachine.transitionTo("main");
            while (running) {
                System.out.println();
                stateMachine.currentScreen.display();
                System.out.print("\n"+stateMachine.currentScreen.prompt);
                String input = reader.readLine();
                if (input == null) {
                    break;
                }
                input = input.trim();
                if (input.isEmpty()) {
                    continue;
                }
                stateMachine.currentScreen.handleInput(input);
            }
        } catch (IOException e) {
            if (running) {
                logger.error("CLI read error: {}", e.getMessage());
            }
        }
    }
}