package iecd.a51597.server.handlers;

import iecd.a51597.common.game.Game;
import iecd.a51597.common.protocol.Message;
import iecd.a51597.common.protocol.MessageBody;
import iecd.a51597.common.protocol.builders.server.ServerMessageBuilder;
import iecd.a51597.common.protocol.types.ActionType;
import iecd.a51597.common.protocol.types.ErrorCodeType;
import iecd.a51597.common.protocol.types.MessageType;
import iecd.a51597.server.game.GameManager;
import iecd.a51597.server.network.Connection;
import iecd.a51597.server.persistence.PersistenceManager;
import iecd.a51597.server.session.Session;
import iecd.a51597.server.session.SessionManager;
import iecd.a51597.server.store.UserStore;
import iecd.a51597.server.store.entities.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class GameHandlerTest {

    private GameHandler handler;
    private ServerMessageBuilder builder;
    private SessionManager sessionManager;
    private UserStore userStore;
    private GameManager gameManager;
    private PersistenceManager persistenceManager;
    private Connection connection;

    private static final UUID MSG_ID = UUID.randomUUID();
    private static final UUID SES_TOKEN = UUID.randomUUID();
    private static final UUID TARGET_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        builder = mock(ServerMessageBuilder.class);
        sessionManager = mock(SessionManager.class);
        userStore = mock(UserStore.class);
        gameManager = mock(GameManager.class);
        persistenceManager = mock(PersistenceManager.class);
        connection = mock(Connection.class);
        handler = new GameHandler(builder, sessionManager, userStore, gameManager, persistenceManager);
        
        when(gameManager.hasFactory()).thenReturn(true);
    }

    @Test
    void gameInvite_success() {
        User sender = new User(UUID.randomUUID(), "alice", "h", null);
        User target = new User(TARGET_ID, "bob", "h", null);
        Session senderSession = new Session(sender, connection);
        Session targetSession = mock(Session.class);
        Connection targetConn = mock(Connection.class);
        Game game = mock(Game.class);
        UUID gameId = UUID.randomUUID();

        when(sessionManager.validate(SES_TOKEN)).thenReturn(Optional.of(senderSession));
        when(userStore.findById(TARGET_ID)).thenReturn(Optional.of(target));
        when(sessionManager.getSessionByUserId(TARGET_ID)).thenReturn(Optional.of(targetSession));
        when(targetSession.getConnection()).thenReturn(targetConn);
        when(gameManager.createPendingGame(sender.getUserId(), TARGET_ID)).thenReturn(game);
        when(game.getGameId()).thenReturn(gameId);

        Message msg = new Message(MSG_ID, MessageType.REQUEST, "1.0", ActionType.GAME_INVITE, SES_TOKEN,
                new MessageBody.GameInviteRequest(TARGET_ID));

        handler.gameInvite(msg, connection);

        verify(builder).gameInviteResponse(MSG_ID, gameId);
        verify(builder).gameInvitePush(gameId, sender);
        verify(connection).sendMessage(any());
        verify(targetConn).sendMessage(any());
    }

    @Test
    void gameInvite_targetOffline_sendsError() {
        User sender = new User(UUID.randomUUID(), "alice", "h", null);
        Session senderSession = new Session(sender, connection);

        when(sessionManager.validate(SES_TOKEN)).thenReturn(Optional.of(senderSession));
        when(userStore.findById(TARGET_ID)).thenReturn(Optional.of(new User(TARGET_ID, "bob", "h", null)));
        when(sessionManager.getSessionByUserId(TARGET_ID)).thenReturn(Optional.empty());

        Message msg = new Message(MSG_ID, MessageType.REQUEST, "1.0", ActionType.GAME_INVITE, SES_TOKEN,
                new MessageBody.GameInviteRequest(TARGET_ID));

        handler.gameInvite(msg, connection);

        verify(builder).error(eq(MSG_ID), eq(ActionType.GAME_INVITE), eq(ErrorCodeType.USER_NOT_ONLINE), any());
    }

    @Test
    void gameInviteResponse_accept_startsGame() {
        User responder = new User(TARGET_ID, "bob", "h", null);
        Session responderSession = new Session(responder, connection);
        UUID gameId = UUID.randomUUID();
        UUID inviterId = UUID.randomUUID();
        Game game = mock(Game.class);
        Session inviterSession = mock(Session.class);
        Connection inviterConn = mock(Connection.class);

        when(sessionManager.validate(SES_TOKEN)).thenReturn(Optional.of(responderSession));
        when(gameManager.getPendingGame(gameId)).thenReturn(Optional.of(game));
        when(game.getGameId()).thenReturn(gameId);
        when(game.getPlayer1Id()).thenReturn(inviterId);
        when(game.getPlayer2Id()).thenReturn(TARGET_ID);
        when(sessionManager.getSessionByUserId(inviterId)).thenReturn(Optional.of(inviterSession));
        when(inviterSession.getConnection()).thenReturn(inviterConn);

        Message msg = new Message(MSG_ID, MessageType.REQUEST, "1.0", ActionType.GAME_INVITE_RESPONSE, SES_TOKEN,
                new MessageBody.GameInviteResponseRequest(gameId, true));

        handler.gameInviteResponse(msg, connection);

        verify(gameManager).acceptGame(gameId);
        verify(builder).gameInviteAcceptedPush(eq(gameId), eq(responder));
        verify(connection).sendMessage(any());
        verify(inviterConn).sendMessage(any());
    }
}
