import iecd.a51597.client.cli.ClientCliHandler;
import iecd.a51597.client.cli.StateMachine;
import iecd.a51597.client.cli.screens.Screen;
import iecd.a51597.common.protocol.Message;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

class StateMachineTest {

    @Test
    void transitionTo_registeredScreen_setsCurrentAndCallsOnEnter() {
        StateMachine sm = new StateMachine(mock(ClientCliHandler.class));
        TestScreen first = new TestScreen(sm);
        sm.registerScreen("first", first);

        sm.transitionTo("first");

        assertSame(first, sm.getCurrentScreen());
        assertEquals(1, first.enterCount);
        assertEquals(0, first.exitCount);
    }

    @Test
    void transitionTo_pushesPreviousScreen_andBackReturnsIt() {
        StateMachine sm = new StateMachine(mock(ClientCliHandler.class));
        TestScreen first = new TestScreen(sm);
        TestScreen second = new TestScreen(sm);
        sm.registerScreen("first", first);
        sm.registerScreen("second", second);

        sm.transitionTo("first");
        sm.transitionTo("second");
        Screen previous = sm.back();

        assertSame(first, previous);
        assertSame(first, sm.getCurrentScreen());
        assertEquals(2, first.enterCount);
        assertEquals(1, first.exitCount);
        assertEquals(1, second.enterCount);
        assertEquals(1, second.exitCount);
    }

    @Test
    void back_withoutHistory_returnsNull() {
        StateMachine sm = new StateMachine(mock(ClientCliHandler.class));

        assertNull(sm.back());
    }

    @Test
    void transitionTo_unregisteredScreen_keepsCurrentScreen() {
        StateMachine sm = new StateMachine(mock(ClientCliHandler.class));
        TestScreen first = new TestScreen(sm);
        sm.registerScreen("first", first);
        sm.transitionTo("first");

        sm.transitionTo("missing");

        assertSame(first, sm.getCurrentScreen());
        assertEquals(1, first.enterCount);
        assertEquals(0, first.exitCount);
    }

    @Test
    void transitionToWithArgs_unregisteredScreen_keepsCurrent_andDoesNotCallHandleArgs() {
        StateMachine sm = new StateMachine(mock(ClientCliHandler.class));
        TestScreen first = new TestScreen(sm);
        sm.registerScreen("first", first);
        sm.transitionTo("first");

        sm.transitionTo("missing", new Object[]{"payload"});

        assertSame(first, sm.getCurrentScreen());
        assertEquals(0, first.handleArgsCount);
    }

    private static final class TestScreen extends Screen {

        int enterCount;
        int exitCount;
        int handleArgsCount;

        private TestScreen(StateMachine sm) {
            super(sm, null);
        }

        @Override
        public void display() {
            // no-op for unit tests
        }

        @Override
        public void handleInput(String input) {
            // no-op for unit tests
        }

        @Override
        public void handlePush(Message message) {
            // no-op for unit tests
        }

        @Override
        public void handleArgs(Object[] args) {
            handleArgsCount++;
        }

        @Override
        public void onEnter() {
            enterCount++;
        }

        @Override
        public void onExit() {
            exitCount++;
        }
    }
}

