package iecd.a51597.client.cli;

import iecd.a51597.client.Client;
import iecd.a51597.client.cli.screens.impl.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Handles the main CLI loop, delegating display and input handling to the current screen in the state machine.
 */
public class ClientCliHandler {

    private final Client client;
    public volatile Boolean running;
    private final StateMachine stateMachine;

    private static final Logger logger = LogManager.getLogger(ClientCliHandler.class);

    public ClientCliHandler(Client client) {
        this.client = client;
        this.stateMachine = new StateMachine(this);
        this.stateMachine.registerScreen("main", new MainMenuScreen(stateMachine, client));
        this.stateMachine.registerScreen("login", new LoginScreen(stateMachine, client));
        this.stateMachine.registerScreen("register", new RegisterScreen(stateMachine, client));
        this.stateMachine.registerScreen("view-profile", new ViewProfileScreen(stateMachine, client));
        this.stateMachine.registerScreen("edit-profile", new EditProfileScreen(stateMachine, client));
    }

    public void loop() {
        running = true;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            stateMachine.transitionTo("main");
            while (running) {
                System.out.println();
                stateMachine.getCurrentScreen().display();
                System.out.print("\n"+stateMachine.getCurrentScreen().prompt);
                String input = reader.readLine();
                if (input == null) {
                    break;
                }
                input = input.trim();
                if (input.isEmpty()) {
                    continue;
                }
                stateMachine.getCurrentScreen().handleInput(input);
            }
        } catch (IOException e) {
            if (running) {
                logger.error("CLI read error", e);
            }
        }
    }
}