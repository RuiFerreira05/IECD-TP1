package iecd.a51597.client.cli.screens.impl;

import iecd.a51597.client.Client;
import iecd.a51597.client.cli.StateMachine;
import iecd.a51597.client.cli.screens.OptionScreen;
import iecd.a51597.client.config.ClientConfiguration;
import iecd.a51597.client.session.ClientSessionManager;
import iecd.a51597.common.protocol.Message;

/**
 * The main menu screen displayed after authentication.
 */
public class MainMenuScreen extends OptionScreen {

    public MainMenuScreen(StateMachine sm, Client client) {
        super(sm, client);
        addOption("Play Dots and Boxes!", () -> sm.changeState(new GameMenuScreen(sm, client)), () -> client.getSessionManager().isLoggedIn());
        addOption("Login", this::login, () -> !client.getSessionManager().isLoggedIn());
        addOption("Register", this::register, () -> !client.getSessionManager().isLoggedIn());
        addOption("View profile", this::viewProfile, () -> client.getSessionManager().isLoggedIn());
        addOption("Search for another player", this::searchForPlayer);
        addOption("Logout", this::logout, () -> client.getSessionManager().isLoggedIn());
        addOption("Exit", this::exit);
    }

    @Override
    public void handlePush(Message message) {
        // TODO
    }

    private void exit() {
        System.out.println("Goodbye!");
        client.exit();
    }

    private void viewProfile() {
        sm.changeState(new ViewProfileScreen(sm, client));
    }

    private void searchForPlayer() {
        sm.changeState(new SearchForPlayerScreen(sm, client));
    }

    private void logout() {
        ClientSessionManager session = client.getServerConnection().getSessionManager();
        if (!session.isLoggedIn()) {
            System.out.println("You are not logged in.");
            return;
        }

        if (session.logout() instanceof ClientSessionManager.LogoutResult.Success) {
            System.out.println("Logged out successfully.");
            prompt = ClientConfiguration.DEFAULT_PROMPT;
        } else {
            System.out.println("An error occurred while logging out. Please try again.");
        }
    }

    private void login() {
        sm.changeState(new LoginScreen(sm, client));
    }

    private void register() {
        sm.changeState(new RegisterScreen(sm, client));
    }

    @Override
    public void onEnter() {
        logger.info("Entered MainMenuScreen");
        if (client.getSessionManager().isLoggedIn()) {
            prompt = client.getSessionManager().getUser().username() + "> ";
        }
    }

    @Override
    public void onExit() {
        logger.info("Exited MainMenuScreen");
    }
}
