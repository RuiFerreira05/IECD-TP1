package iecd.a51597.client.cli.screens;

import iecd.a51597.client.Client;
import iecd.a51597.client.cli.StateMachine;

public class MainMenuScreen extends Screen {
    public MainMenuScreen(StateMachine sm, Client client) {
        super(sm, client);
        this.options.add("Login");
        this.options.add("Logout");
        this.options.add("Search for another player");
        this.options.add("View profile");
        this.options.add("Exit");
    }

//    @Override
//    public void display() {
//    }

    @Override
    void handleOption(int option) {
            switch (option) {
                case 1:
                    System.out.println("Login selected");
                    break;
                case 2:
                    System.out.println("Logout selected");
                    break;
                case 3:
                    System.out.println("Search for another player selected");
                    break;
                case 4:
                    System.out.println("View profile selected");
                    break;
                case 5:
                    System.out.println("Exiting...");
                    client.exit();
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
    }

    @Override
    public void onEnter() {

    }

    @Override
    public void onExit() {

    }
}
