package iecd.a51597.client.cli.screens;

import iecd.a51597.client.Client;
import iecd.a51597.client.cli.StateMachine;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class OptionScreen extends Screen {

    protected List<ScreenOption> options;

    protected OptionScreen(StateMachine sm, Client client) {
        super(sm, client);
        this.options = new ArrayList<>();
    }

    @Override
    public void display() {
        System.out.println("=== " + this.getClass().getSimpleName() + " ===");
        for (int i = 0; i < options.size(); i++) {
            System.out.println((i + 1) + ". " + options.get(i));
        }
    };

    @Override
    public void handleInput(String input) {
        try {
            int option = Integer.parseInt(input);
            if (option < 1 || option > options.size()) {
                System.out.println("Invalid option. Please enter a number between 1 and " + options.size() + ".");
            } else {
                options.get(option - 1).execute();
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter a number.");
        }
    };

    protected void addOption(String description, Consumer<Void> action) {
        options.add(new ScreenOption(description, action));
    }

    protected record ScreenOption(String description, Consumer<Void> action) {
        @Override
        public String toString() {
            return description;
        }

        public void execute() {
            action.accept(null);
        }
    }
}
