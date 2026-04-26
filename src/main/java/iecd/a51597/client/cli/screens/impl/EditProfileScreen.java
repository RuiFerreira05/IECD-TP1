package iecd.a51597.client.cli.screens.impl;

import iecd.a51597.client.Client;
import iecd.a51597.client.cli.StateMachine;
import iecd.a51597.client.cli.screens.Screen;
import iecd.a51597.client.session.ClientSessionManager;
import iecd.a51597.common.protocol.Message;

import java.time.LocalDate;

/**
 * Screen for editing user profile information.
 */
public class EditProfileScreen extends Screen {

    public enum State {
        USERNAME,
        PASSWORD,
        PHOTO,
        NATIONALITY,
        DOB
    }

    private State currentState = State.USERNAME;
    private String tempUsername;
    private String tempPassword;
    private String tempPhoto;
    private String tempNationality;
    private LocalDate tempDob;

    public EditProfileScreen(StateMachine sm, Client client) {
        super(sm, client);
    }

    @Override
    public void display() {
            switch (currentState) {
                case USERNAME -> System.out.print("Enter new username (or \"skip\" to keep current): ");
                case PASSWORD -> System.out.print("Enter new password (or \"skip\" to keep current): ");
                case PHOTO -> System.out.print("Enter new photo URL (or \"skip\" to keep current): ");
                case NATIONALITY -> System.out.print("Enter new nationality (or \"skip\" to keep current): ");
                case DOB -> System.out.print("Enter new date of birth (YYYY-MM-DD) (or \"skip\" to keep current): ");
            }
    }

    @Override
    public void handleInput(String input) {
        switch (currentState) {
            case USERNAME -> {
                if (!input.equals("skip")) {
                    tempUsername = input;
                }
                currentState = State.PASSWORD;
            }
            case PASSWORD -> {
                if (!input.equals("skip")) {
                    tempPassword = input;
                }
                currentState = State.PHOTO;
            }
            case PHOTO -> {
                if (!input.equals("skip")) {
                    tempPhoto = input;
                }
                currentState = State.NATIONALITY;
            }
            case NATIONALITY -> {
                if (!input.equals("skip")) {
                    tempNationality = input;
                }
                currentState = State.DOB;
            }
            case DOB -> {
                if (!input.equals("skip")) {
                    try {
                        tempDob = LocalDate.parse(input);
                        if (tempDob.isAfter(LocalDate.now())) {
                            System.out.println("Date of birth cannot be in the future.");
                            tempDob = null;
                            return; // Stay on the DOB state to allow re-entry
                        }
                    } catch (Exception e) {
                        System.out.println("Invalid date format. Please use YYYY-MM-DD.");
                        return; // Stay on the DOB state to allow re-entry
                    }
                }
                editProfile();
            }
        }
    }

    private void editProfile() {
        switch (client.getSessionManager().editProfile(tempUsername, tempPassword, tempPhoto, tempNationality, tempDob)) {
            case ClientSessionManager.EditProfileResult.Success ignored -> {
                System.out.println("Profile updated successfully!");
                sm.changeState(new ViewProfileScreen(sm, client));
            }
            case ClientSessionManager.EditProfileResult.UsernameTaken ignored -> {
                System.out.println("Username is already taken. Please choose a different username.");
            }
            case ClientSessionManager.EditProfileResult.PhotoNotFoundError ignored -> {
                System.out.println("Photo file not found. Please check the provided path and try again.");
            }
            case ClientSessionManager.EditProfileResult.Error message -> {
                System.out.println("Failed to update profile: " + message);
                sm.changeState(new ViewProfileScreen(sm, client));
            }
        }
        resetState();
    }

    @Override
    public void handlePush(Message message) {

    }

    private void resetState() {
        currentState = State.USERNAME;
        tempUsername = null;
        tempPassword = null;
        tempPhoto = null;
        tempNationality = null;
        tempDob = null;
    }

    @Override
    public void onEnter() {
        resetState();
    }

    @Override
    public void onExit() {

    }
}
