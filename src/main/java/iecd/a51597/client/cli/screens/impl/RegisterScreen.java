package iecd.a51597.client.cli.screens.impl;

import iecd.a51597.client.Client;
import iecd.a51597.client.cli.StateMachine;
import iecd.a51597.client.cli.screens.Screen;
import iecd.a51597.client.session.ClientSessionManager;
import iecd.a51597.common.protocol.Message;

public class RegisterScreen extends Screen {

    private enum RegisterState {
        ENTER_USERNAME,
        ENTER_PASSWORD
    }

    private RegisterState currentState = RegisterState.ENTER_USERNAME;
    private String tempUsername;
    private String tempPassword;

    public RegisterScreen(StateMachine sm, Client client) {
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
                currentState = RegisterState.ENTER_PASSWORD;
            }
            case ENTER_PASSWORD -> {
                if (input.equalsIgnoreCase("exit")) {
                    sm.changeState(new MainMenuScreen(sm, client));
                    return;
                }
                // Store password in a temporary variable
                tempPassword = input;
                attemptRegister();
            }
        }
    }

    private void attemptRegister() {
        switch (client.getSessionManager().register(tempUsername, tempPassword)) {
            case ClientSessionManager.RegisterResult.Success ignored -> {
                if (client.getSessionManager().login(tempUsername, tempPassword) instanceof ClientSessionManager.LoginResult.Success) {
                    System.out.println("Registration successful! Welcome, " + client.getSessionManager().getUser().username() + "!");
                    sm.changeState(new MainMenuScreen(sm, client));
                    resetState();
                } else {
                    System.out.println("Registration succeeded but login failed. Please try logging in from the main menu.");
                    sm.changeState(new MainMenuScreen(sm, client));
                    resetState();
                }
            }
            case ClientSessionManager.RegisterResult.UsernameTaken ignored -> {
                System.out.println("That username is already taken. Please try a different one.");
                resetState();
            }
            case ClientSessionManager.RegisterResult.Error ignored -> {
                System.out.println("An error occurred during registration. Please try again.");
                resetState();
            }
        }
    }

    private void resetState() {
        tempUsername = null;
        tempPassword = null;
        currentState = RegisterState.ENTER_USERNAME;
    }

    @Override
    public void handlePush(Message message) {
        // TODO
    }

    @Override
    public void onEnter() {
        logger.info("Entered RegisterScreen");
    }

    @Override
    public void onExit() {
        logger.info("Exited RegisterScreen");
    }
}
