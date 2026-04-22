package iecd.a51597.client.cli;

import iecd.a51597.client.cli.screens.Screen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class StateMachine {

    private final Map<String, Screen> screens;
    private Screen currentScreen;
    private final Stack<Screen> history;
    private final ClientCliHandler cliHandler;

    private final Logger logger = LogManager.getLogger(StateMachine.class);

    public StateMachine(ClientCliHandler cliHandler) {
        this.cliHandler = cliHandler;
        this.screens = new HashMap<>();
        this.history = new Stack<>();
    }

    public void registerScreen(String identifier, Screen screen) {
        screens.put(identifier, screen);
    }

    public void transitionTo(String identifier) {
        Screen nextScreen = screens.get(identifier);
        if (nextScreen != null) {
            if (currentScreen != null) {
                currentScreen.onExit();
                history.push(currentScreen);
            }
            currentScreen = nextScreen;
            currentScreen.onEnter();
        } else {
            logger.warn("Attempted to transition to unregistered screen: {}", identifier);
            System.out.println("Error: Screen not found: " + identifier);
        }
    }

    public Screen back() {
        if (history.isEmpty()) {
            return null;
        }

        if (currentScreen != null) {
            currentScreen.onExit();
        }
        currentScreen = history.pop();
        currentScreen.onEnter();
        return currentScreen;
    }

    public Screen getCurrentScreen() {
        return currentScreen;
    }

    public Logger getLogger() {
        return logger;
    }
}
