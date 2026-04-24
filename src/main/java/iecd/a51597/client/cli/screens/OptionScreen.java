package iecd.a51597.client.cli.screens;

import iecd.a51597.client.Client;
import iecd.a51597.client.cli.StateMachine;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

public abstract class OptionScreen extends Screen {

    protected List<ScreenOption> options;

    protected OptionScreen(StateMachine sm, Client client) {
        super(sm, client);
        this.options = new ArrayList<>();
    }

    protected void clearOptions() {
        this.options.clear();
    }

    @Override
    public void display() {
        System.out.println("=== " + this.getClass().getSimpleName() + " ===");
        displayOptions();
    };

    public void displayOptions() {
        List<ScreenOption> visibleOptions = getVisibleOptions();
        int counter = 1;
        for (ScreenOption option : visibleOptions) {
            System.out.println(counter + ". " + option);
            counter++;
        }
    }

    @Override
    public void handleInput(String input) {
        try {
            int option = Integer.parseInt(input);
            List<ScreenOption> visibleOptions = getVisibleOptions();
            if (option < 1 || option > visibleOptions.size()) {
                System.out.println("Invalid option. Please enter a number between 1 and " + visibleOptions.size() + ".");
            } else {
                visibleOptions.get(option - 1).execute();
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter a number.");
        }
    }

    protected void addOption(String description, Runnable action, BooleanSupplier condition) {
        options.add(new ScreenOption(description, action, condition));
    }

    protected void addOption(String description, Runnable action) {
        addOption(description, action, () -> true);
    }

    private List<ScreenOption> getVisibleOptions() {
        return options.stream()
                .filter(ScreenOption::isVisible)
                .toList();
    }

    protected record ScreenOption(String description, Runnable action, BooleanSupplier condition) {
        private boolean isVisible() {
            return condition.getAsBoolean();
        }

        @Override
        public String toString() {
            return description;
        }

        public void execute() {
            action.run();
        }
    }
}
