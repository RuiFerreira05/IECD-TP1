package iecd.a51597.server.handlers;

import iecd.a51597.common.protocol.Message;
import iecd.a51597.common.protocol.MessageBody;
import iecd.a51597.common.protocol.builders.server.ServerMessageBuilder;
import iecd.a51597.common.protocol.types.ActionType;
import iecd.a51597.common.protocol.types.ErrorCodeType;
import iecd.a51597.common.protocol.types.MessageType;
import iecd.a51597.server.network.Connection;
import iecd.a51597.server.persistence.PersistenceManager;
import iecd.a51597.server.session.Session;
import iecd.a51597.server.session.SessionManager;
import iecd.a51597.server.store.UserStore;
import iecd.a51597.server.store.entities.User;
import iecd.a51597.server.store.exceptions.UsernameAlreadyTakenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AuthHandlerTest {

    private AuthHandler handler;
    private ServerMessageBuilder builder;
    private SessionManager sessionManager;
    private UserStore userStore;
    private PersistenceManager persistenceManager;
    private Connection connection;

    private static final UUID MSG_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        builder = mock(ServerMessageBuilder.class);
        sessionManager = mock(SessionManager.class);
        userStore = mock(UserStore.class);
        persistenceManager = mock(PersistenceManager.class);
        connection = mock(Connection.class);
        handler = new AuthHandler(builder, sessionManager, userStore, persistenceManager);
    }

    @Test
    void register_success_sendsOk() throws UsernameAlreadyTakenException {
        Message msg = new Message(MSG_ID, MessageType.REQUEST, "1.0", ActionType.REGISTER, null,
                new MessageBody.Register("alice", "pass"));
        
        handler.register(msg, connection);

        verify(userStore).register("alice", "pass");
        verify(builder).ok(MSG_ID, ActionType.REGISTER);
        verify(connection).sendMessage(any());
    }

    @Test
    void register_taken_sendsError() throws UsernameAlreadyTakenException {
        Message msg = new Message(MSG_ID, MessageType.REQUEST, "1.0", ActionType.REGISTER, null,
                new MessageBody.Register("alice", "pass"));
        doThrow(new UsernameAlreadyTakenException("alice")).when(userStore).register(any(), any());

        handler.register(msg, connection);

        verify(builder).error(eq(MSG_ID), eq(ActionType.REGISTER), eq(ErrorCodeType.USERNAME_TAKEN), any());
    }

    @Test
    void login_success_createsSessionAndSendsSuccess() {
        Message msg = new Message(MSG_ID, MessageType.REQUEST, "1.0", ActionType.LOGIN, null,
                new MessageBody.LoginRequest("alice", "pass"));
        User user = new User(UUID.randomUUID(), "alice", "hash", null);
        Session session = new Session(user, connection);
        
        when(userStore.findByCredentials("alice", "pass")).thenReturn(Optional.of(user));
        when(sessionManager.createSession(user, connection)).thenReturn(session);

        handler.login(msg, connection);

        verify(sessionManager).createSession(user, connection);
        verify(builder).loginSuccess(MSG_ID, session.getToken(), user);
    }

    @Test
    void login_failure_sendsError() {
        Message msg = new Message(MSG_ID, MessageType.REQUEST, "1.0", ActionType.LOGIN, null,
                new MessageBody.LoginRequest("alice", "wrong"));
        when(userStore.findByCredentials(any(), any())).thenReturn(Optional.empty());

        handler.login(msg, connection);

        verify(builder).error(eq(MSG_ID), eq(ActionType.LOGIN), eq(ErrorCodeType.AUTH_FAILED), any());
    }

    @Test
    void logout_validSession_invalidatesAndSendsOk() {
        UUID token = UUID.randomUUID();
        Message msg = new Message(MSG_ID, MessageType.REQUEST, "1.0", ActionType.LOGOUT, token, new MessageBody.Logout());
        Session session = mock(Session.class);
        when(sessionManager.validate(token)).thenReturn(Optional.of(session));

        handler.logout(msg, connection);

        verify(sessionManager).invalidate(token);
        verify(builder).ok(MSG_ID, ActionType.LOGOUT);
    }
}
