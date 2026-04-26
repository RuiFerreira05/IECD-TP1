package iecd.a51597.client.cli.screens.impl;

import iecd.a51597.client.Client;
import iecd.a51597.client.cli.StateMachine;
import iecd.a51597.client.cli.screens.Screen;
import iecd.a51597.client.session.ClientSessionManager;
import iecd.a51597.common.protocol.Message;

/**
 * Screen for user authentication.
 */
public class LoginScreen extends Screen {

    private enum LoginState {
        ENTER_USERNAME,
        ENTER_PASSWORD
    }

    private LoginState currentState = LoginState.ENTER_USERNAME;
    private String tempUsername;
    private String tempPassword;

    public LoginScreen(StateMachine sm, Client client) {
        super(sm, client);
    }

    @Override
    public void display() {
        switch (currentState) {
            case ENTER_USERNAME -> System.out.print("Enter username (type 'exit' to go back to main menu): ");
            case ENTER_PASSWORD -> System.out.print("Enter password (type 'exit' to go back to main menu): ");
        }
    }

    @Override
    public void handleInput(String input) {
        switch (currentState) {
            case ENTER_USERNAME -> {
                if (input.equalsIgnoreCase("exit")) {
                    sm.changeState(new MainMenuScreen(sm, client));
                    return;
                }
                // Store username in a temporary variable
                tempUsername = input;
                currentState = LoginState.ENTER_PASSWORD;
            }
            case ENTER_PASSWORD -> {
                if (input.equalsIgnoreCase("exit")) {
                    sm.changeState(new MainMenuScreen(sm, client));
                    return;
                }
                // Store password in a temporary variable
                tempPassword = input;
                attemptLogin();
            }
        }
    }

    private void attemptLogin() {
        switch (client.getSessionManager().login(tempUsername, tempPassword)) {
            case ClientSessionManager.LoginResult.Success ignored -> {
                System.out.println("Login successful! Welcome back, " + client.getSessionManager().getUser().username() + "!");
                sm.changeState(new MainMenuScreen(sm, client));
            }
            case ClientSessionManager.LoginResult.InvalidCredentials ignored -> {
                System.out.println("Invalid username or password. Please try again.");
                resetState();
            }
            case ClientSessionManager.LoginResult.Error ignored -> {
                System.out.println("An error occurred while trying to log in. Please try again.");
                resetState();
            }
        }
    }

    private void resetState() {
        tempUsername = null;
        tempPassword = null;
        currentState = LoginState.ENTER_USERNAME;
    }

    @Override
    public void handlePush(Message message) {
        // TODO
    }


    @Override
    public void onEnter() {
        logger.info("Entered LoginScreen");
    }

    @Override
    public void onExit() {
        logger.info("Exited LoginScreen");
    }
}
