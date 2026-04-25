import iecd.a51597.client.cli.ClientCliHandler;
import iecd.a51597.client.cli.StateMachine;
import iecd.a51597.client.cli.screens.OptionScreen;
import iecd.a51597.common.protocol.Message;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class OptionScreenTest {

    @Test
    void displayOptions_numbersOnlyVisibleOptions() {
        TestOptionScreen screen = new TestOptionScreen();
        screen.add("First", () -> { });
        screen.add("Hidden", () -> { }, () -> false);
        screen.add("Third", () -> { });

        String output = captureStdOut(screen::displayOptions);

        assertTrue(output.contains("1. First"));
        assertTrue(output.contains("2. Third"));
        assertFalse(output.contains("Hidden"));
    }

    @Test
    void handleInput_mapsIndexToVisibleOptionsOnly() {
        TestOptionScreen screen = new TestOptionScreen();
        List<String> executed = new ArrayList<>();
        screen.add("First", () -> executed.add("first"));
        screen.add("Hidden", () -> executed.add("hidden"), () -> false);
        screen.add("Third", () -> executed.add("third"));

        screen.handleInput("2");

        assertEquals(List.of("third"), executed);
    }

    @Test
    void visibility_isReevaluatedEveryTime() {
        TestOptionScreen screen = new TestOptionScreen();
        AtomicBoolean loggedIn = new AtomicBoolean(false);
        screen.add("Login", () -> { }, () -> !loggedIn.get());
        screen.add("Logout", () -> { }, loggedIn::get);

        String loggedOutMenu = captureStdOut(screen::displayOptions);
        loggedIn.set(true);
        String loggedInMenu = captureStdOut(screen::displayOptions);

        assertTrue(loggedOutMenu.contains("Login"));
        assertFalse(loggedOutMenu.contains("Logout"));
        assertFalse(loggedInMenu.contains("Login"));
        assertTrue(loggedInMenu.contains("Logout"));
    }

    private static String captureStdOut(Runnable action) {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
            action.run();
            return buffer.toString(StandardCharsets.UTF_8);
        } finally {
            System.setOut(originalOut);
        }
    }

    private static final class TestOptionScreen extends OptionScreen {

        private TestOptionScreen() {
            super(new StateMachine(mock(ClientCliHandler.class)), null);
        }

        private void add(String description, Runnable action) {
            addOption(description, action);
        }

        private void add(String description, Runnable action, BooleanSupplier condition) {
            addOption(description, action, condition);
        }

        @Override
        public void handlePush(Message message) {
            // no-op for unit tests
        }

        @Override
        public void onEnter() {
            // no-op for unit tests
        }

        @Override
        public void onExit() {
            // no-op for unit tests
        }
    }
}

