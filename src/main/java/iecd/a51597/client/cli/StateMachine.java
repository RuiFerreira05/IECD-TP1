package iecd.a51597.client.cli;

import iecd.a51597.client.cli.screens.Screen;

import java.util.HashMap;
import java.util.Map;

public class StateMachine {

    private final Map<String, Screen> screens;
    public Screen currentScreen;
    private final ClientCliHandler cliHandler;

    public StateMachine(ClientCliHandler cliHandler) {
        this.cliHandler = cliHandler;
        this.screens = new HashMap<>();
    }

    public void registerScreen(String identifier, Screen screen) {
        if (currentScreen == null) {
            currentScreen = screen;
        }
        screens.put(identifier, screen);
    }

    public void transitionTo(String identifier) {
        if (screens.containsKey(identifier)) {
            if (currentScreen != null) {currentScreen.onExit();}
            currentScreen = screens.get(identifier);
            currentScreen.onEnter();
        } else {
            throw new IllegalArgumentException("Screen not found: " + identifier);
        }
    }
}
