package iecd.a51597.client.cli.screens.impl;

import iecd.a51597.client.Client;
import iecd.a51597.client.cli.StateMachine;
import iecd.a51597.client.cli.screens.OptionScreen;

public class MainMenuScreen extends OptionScreen {
    public MainMenuScreen(StateMachine sm, Client client) {
        super(sm, client);
        addOption("Login", v -> login());
        addOption("Logout", v -> logout());
        addOption("Search for another player", v -> searchForPlayer());
        addOption("View profile", v -> viewProfile());
        addOption("Exit", v -> exit());
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
        System.out.println("Logout selected");
    }

    private void login() {
        System.out.println("Login selected");
    }

//    @Override
//    public void display() {
//    }

    @Override
    public void onEnter() {
        System.out.println("DEBUG: Entering MainMenuScreen");
        sm.getLogger().info("Entered MainMenuScreen");
    }

    @Override
    public void onExit() {
        System.out.println("DEBUG: Exiting MainMenuScreen");
        sm.getLogger().info("Exited MainMenuScreen");
    }
}
