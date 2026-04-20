package iecd.a51597.client.cli.screens;

import iecd.a51597.client.Client;
import iecd.a51597.client.cli.StateMachine;

import java.util.ArrayList;
import java.util.List;

public abstract class Screen {

    protected List<String> options;
    protected StateMachine sm;
    protected Client client;
    public String carat;

    protected Screen(StateMachine sm, Client client){
        this.sm = sm;
        this.client = client;
        this.options = new ArrayList<>();
        this.carat = ">> ";
    }

    public void display() {
        System.out.println("=== " + this.getClass().getSimpleName() + " ===");
        for (int i = 0; i < options.size(); i++) {
            System.out.println((i + 1) + ". " + options.get(i));
        }
    };

    public void handleInput(String input) {
        try {
            int option = Integer.parseInt(input);
            if (option < 1 || option > options.size()) {
                System.out.println("Invalid option. Please enter a number between 1 and " + options.size() + ".");
            } else {
                handleOption(option);
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter a number.");
        }
    };

    abstract void handleOption(int option);

    abstract public void onEnter();

    abstract public void onExit();
}
