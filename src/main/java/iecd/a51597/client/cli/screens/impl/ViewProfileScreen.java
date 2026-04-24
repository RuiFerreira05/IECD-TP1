package iecd.a51597.client.cli.screens.impl;

import iecd.a51597.client.Client;
import iecd.a51597.client.cli.StateMachine;
import iecd.a51597.client.cli.screens.OptionScreen;
import iecd.a51597.common.protocol.Message;
import iecd.a51597.common.store.UserDTO;

public class ViewProfileScreen extends OptionScreen {

    private UserDTO user;

    public ViewProfileScreen(StateMachine sm, Client client) {
        super(sm, client);
        addOption("back", sm::back);
        addOption("Back to main menu", () -> sm.transitionTo("main"));
        addOption("Edit Profile", this::editProfile, () -> client.getSessionManager().getUser() == user);
    }

    private void editProfile() {
        sm.transitionTo("edit-profile");
    }

    @Override
    public void display() {
        System.out.println("=== Your Profile ===");
        if (user == null) {
            return;
        }
        System.out.println("User ID: " + user.userId());
        System.out.println("Username: " + user.username());
        System.out.println("nationality: " + (user.nationality() == null ? "" : user.nationality()));
        System.out.println("Date of Birth: " + (user.dob() == null ? "" : user.dob() + " (" + user.getAge() + " years old)"));
        if (user.stats() != null) {
            System.out.println("stats:");
            System.out.println("  Games played: " + user.stats().gamesPlayed());
            System.out.println("  Games won: " + user.stats().gamesWon());
            System.out.println("  Games lost: " + user.stats().gamesLost());
            System.out.println("  Win-rate: " + user.stats().winRate());
            System.out.println("  Total Play Time: " + (user.stats().totalPlayTimeSecs() / 60) + " minutes");
        }
        System.out.println();
        displayOptions();
    }

    @Override
    public void handlePush(Message message) {

    }

    @Override
    public void handleArgs(Object[] args) {
        logger.info("passed args to ViewProfileScreen: {}", args);
        if (args[0] instanceof UserDTO) {
            user = (UserDTO) args[0];
        }
    }

    @Override
    public void onEnter() {
        user = client.getSessionManager().getUser(); // reset user
        logger.info("Entered ViewProfileScreen");
    }

    @Override
    public void onExit() {
        logger.info("Exited ViewProfileScreen");
    }
}
