package iecd.a51597.client.cli.screens.impl;

import iecd.a51597.client.Client;
import iecd.a51597.client.cli.StateMachine;
import iecd.a51597.client.cli.screens.OptionScreen;
import iecd.a51597.client.session.ClientSessionManager;
import iecd.a51597.client.session.exceptions.UnexpectedResponse;
import iecd.a51597.common.protocol.Message;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class MainMenuScreen extends OptionScreen {

    public MainMenuScreen(StateMachine sm, Client client) {
        super(sm, client);
        addOption("Login", this::login, () -> !client.getSessionManager().isLoggedIn());
        addOption("Register", this::register, () -> !client.getSessionManager().isLoggedIn());
        addOption("Logout", this::logout, () -> client.getSessionManager().isLoggedIn());
        addOption("Search for another player", this::searchForPlayer);
        addOption("View profile", this::viewProfile, () -> client.getSessionManager().isLoggedIn());
        addOption("Exit", this::exit);
    }

    @Override
    public void handlePush(Message message) {
        // TODO
    }

    private void exit() {
        System.out.println("Exit selected");
        client.exit();
    }

    private void viewProfile() {
        sm.transitionTo("profile");
    }

    private void searchForPlayer() {
        sm.transitionTo("search");
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
//        sm.transitionTo("login");
        try {
            client.getSessionManager().login("test", "1234");
        } catch (ExecutionException | UnexpectedResponse | TimeoutException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void register() {
        sm.transitionTo("register");
    }

    @Override
    public void onEnter() {
        sm.getLogger().info("Entered MainMenuScreen");
    }

    @Override
    public void onExit() {
        sm.getLogger().info("Exited MainMenuScreen");
    }
}
