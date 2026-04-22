package iecd.a51597.client.cli.screens.impl;

import iecd.a51597.client.Client;
import iecd.a51597.client.cli.StateMachine;
import iecd.a51597.client.cli.screens.OptionScreen;
import iecd.a51597.client.config.ClientConfiguration;
import iecd.a51597.client.session.ClientSessionManager;
import iecd.a51597.client.session.exceptions.UnexpectedResponse;
import iecd.a51597.common.protocol.Message;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class MainMenuScreen extends OptionScreen {

    private enum AuthInputMode {
        NONE,
        LOGIN_USERNAME,
        LOGIN_PASSWORD,
        REGISTER_USERNAME,
        REGISTER_PASSWORD
    }

    private AuthInputMode authInputMode = AuthInputMode.NONE;
    private String pendingUsername;

    public MainMenuScreen(StateMachine sm, Client client) {
        super(sm, client);
        addOption("Login", v -> login());
        addOption("Register", v -> register());
        addOption("Logout", v -> logout());
        addOption("Search for another player", v -> searchForPlayer());
        addOption("View profile", v -> viewProfile());
        addOption("Exit", v -> exit());
    }

    @Override
    public void handlePush(Message message) {
        // TODO
    }

    @Override
    public void display() {
        if (authInputMode == AuthInputMode.NONE) {
            super.display();
            return;
        }

        switch (authInputMode) {
            case LOGIN_USERNAME -> System.out.println("=== Login ===\nEnter username:");
            case LOGIN_PASSWORD -> System.out.println("=== Login ===\nEnter password:");
            case REGISTER_USERNAME -> System.out.println("=== Register ===\nChoose username:");
            case REGISTER_PASSWORD -> System.out.println("=== Register ===\nChoose password:");
            default -> {
            }
        }
    }

    @Override
    public void handleInput(String input) {
        if (authInputMode == AuthInputMode.NONE) {
            super.handleInput(input);
            return;
        }

        switch (authInputMode) {
            case LOGIN_USERNAME -> {
                pendingUsername = input;
                authInputMode = AuthInputMode.LOGIN_PASSWORD;
                prompt = "password> ";
            }
            case LOGIN_PASSWORD -> {
                String username = pendingUsername;
                clearAuthFlow();
                performLogin(username, input);
            }
            case REGISTER_USERNAME -> {
                pendingUsername = input;
                authInputMode = AuthInputMode.REGISTER_PASSWORD;
                prompt = "password> ";
            }
            case REGISTER_PASSWORD -> {
                String username = pendingUsername;
                clearAuthFlow();
                performRegister(username, input);
            }
            default -> clearAuthFlow();
        }
    }

    private void exit() {
        System.out.println("Exit selected");
        client.exit();
    }

    private void viewProfile() {
        System.out.println("View profile selected");
    }

    private void searchForPlayer() {
        System.out.println("Search for another player selected");
    }

    private void logout() {
        ClientSessionManager session = client.getServerConnection().getSessionManager();
        if (!session.isLoggedIn()) {
            System.out.println("You are not logged in.");
            return;
        }

        try {
            session.logout();
            System.out.println("Logged out successfully.");
        } catch (UnexpectedResponse | ExecutionException | TimeoutException e) {
            System.out.println("Logout failed: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Logout interrupted.");
        }
    }

    private void login() {
        ClientSessionManager session = client.getServerConnection().getSessionManager();
        if (session.isLoggedIn()) {
            System.out.println("Already logged in.");
            return;
        }

        authInputMode = AuthInputMode.LOGIN_USERNAME;
        prompt = "username> ";
    }

    private void register() {
        ClientSessionManager session = client.getServerConnection().getSessionManager();
        if (session.isLoggedIn()) {
            System.out.println("Logout before registering a new account.");
            return;
        }

        authInputMode = AuthInputMode.REGISTER_USERNAME;
        prompt = "username> ";
    }

    private void performLogin(String username, String password) {
        try {
            client.getServerConnection().getSessionManager().login(username, password);
            System.out.println("Login successful.");
        } catch (UnexpectedResponse | ExecutionException | TimeoutException e) {
            System.out.println("Login failed: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Login interrupted.");
        }
    }

    private void performRegister(String username, String password) {
        try {
            client.getServerConnection().getSessionManager().register(username, password);
            System.out.println("Registration successful. Logged in.");
        } catch (UnexpectedResponse | ExecutionException | TimeoutException e) {
            System.out.println("Registration failed: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Registration interrupted.");
        }
    }

    private void clearAuthFlow() {
        authInputMode = AuthInputMode.NONE;
        pendingUsername = null;
        prompt = ClientConfiguration.DEFAULT_PROMPT;
    }

//    @Override
//    public void display() {
//    }

    @Override
    public void onEnter() {
        clearAuthFlow();
        sm.getLogger().info("Entered MainMenuScreen");
    }

    @Override
    public void onExit() {
        sm.getLogger().info("Exited MainMenuScreen");
    }
}
