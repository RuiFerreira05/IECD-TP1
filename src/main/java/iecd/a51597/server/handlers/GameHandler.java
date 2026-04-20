package iecd.a51597.server.handlers;

import iecd.a51597.server.game.Move;
import iecd.a51597.server.network.Connection;
import iecd.a51597.server.game.GameManager;
import iecd.a51597.common.protocol.exceptions.MalformedMessageException;
import iecd.a51597.server.session.Session;
import iecd.a51597.server.session.SessionManager;
import iecd.a51597.server.game.Game;
import iecd.a51597.server.game.MoveResult;
import iecd.a51597.common.protocol.Message;
import iecd.a51597.common.protocol.MessageBody;
import iecd.a51597.common.protocol.builders.MessageBuilder;
import iecd.a51597.common.protocol.types.ErrorCodeType;
import iecd.a51597.server.store.User;
import iecd.a51597.server.store.UserStore;

import java.util.Optional;

/**
 * Handles multiplayer game invitation and move lifecycle actions.
 */
public class GameHandler extends BaseHandler {

    private final UserStore userStore;
    private final GameManager gameManager;

    /**
     * Creates a game handler.
     */
    public GameHandler(MessageBuilder messageBuilder, SessionManager sessionManager, UserStore userStore, GameManager gameManager) {
        super(messageBuilder, sessionManager);
        this.userStore = userStore;
        this.gameManager = gameManager;
    }

    /**
     * Handles game invitation requests.
     */
    public void gameInvite(Message message, Connection connection) {
        if (!requireConfiguredGame(message, connection)) return;
        Optional<Session> sessionOpt = requireSession(message, connection);
        if (sessionOpt.isEmpty()) return;
        Session session = sessionOpt.get();

        User sender = session.getUser();

        if (gameManager.isInGame(sender.getUserId())) {
            sendError(message, connection, ErrorCodeType.ALREADY_IN_GAME, "You are already in a game");
            return;
        }

        MessageBody.GameInviteRequest body = (MessageBody.GameInviteRequest) message.body();

        if (body.targetUserId().equals(sender.getUserId())) {
            sendError(message, connection, ErrorCodeType.UNEXPECTED_MESSAGE_ACTION, "Cannot invite yourself to a game");
            return;
        }

        Optional<User> targetOpt = userStore.findById(body.targetUserId());
        if (targetOpt.isEmpty()) {
            sendError(message, connection, ErrorCodeType.USER_NOT_FOUND, "Target user does not exist");
            return;
        }
        User target = targetOpt.get();

        Optional<Session> targetSessionOpt = sessionManager.getSessionByUserId(target.getUserId());
        if (targetSessionOpt.isEmpty()) {
            sendError(message, connection, ErrorCodeType.USER_NOT_ONLINE, "Target user is not online");
            return;
        }
        Session targetSession = targetSessionOpt.get();

        if (gameManager.isInGame(target.getUserId())) {
            sendError(message, connection, ErrorCodeType.ALREADY_IN_GAME, "Target user is already in a game");
            return;
        }

        Game game = gameManager.createPendingGame(sender, target);

        connection.sendMessage(messageBuilder.gameInviteResponse(message.messageId(), game.getGameId()));
        targetSession.getConnection().sendMessage(
                messageBuilder.gameInvitePush(game.getGameId(), sender)
        );
    }

    /**
     * Handles invitation acceptance/decline responses.
     */
    public void gameInviteResponse(Message message, Connection connection) {
        Optional<Session> sessionOpt = requireSession(message, connection);
        if (sessionOpt.isEmpty()) return;
        Session session = sessionOpt.get();

        if (gameManager.isInGame(session.getUserId())) {
            sendError(message, connection, ErrorCodeType.ALREADY_IN_GAME, "You are already in a game");
            return;
        }

        User responder = session.getUser();

        MessageBody.GameInviteResponseRequest body = (MessageBody.GameInviteResponseRequest) message.body();

        Optional<Game> gameOpt = gameManager.getPendingGame(body.gameId());
        if (gameOpt.isEmpty()) {
            sendError(message, connection, ErrorCodeType.GAME_NOT_FOUND, "Game not found");
            return;
        }
        Game game = gameOpt.get();

        User inviter = game.getPlayer1();

        // Edge case just to prevent game invite high-jacking, not even sure if it would trigger but might as well
        if (!responder.getUserId().equals(game.getPlayer2().getUserId())) {
            sendError(message, connection, ErrorCodeType.UNEXPECTED_MESSAGE_ACTION, "You are not the invited player");
            return;
        }

        Optional<Session> inviterSessionOpt = sessionManager.getSessionByUserId(inviter.getUserId());

        if (inviterSessionOpt.isEmpty()) {
            gameManager.declineGame(game.getGameId());
            sendError(message, connection, ErrorCodeType.USER_NOT_ONLINE, "The inviting player is no longer online");
            return;
        }

        if (!body.accept()) {
            gameManager.declineGame(game.getGameId());
            connection.sendMessage(messageBuilder.ok(message.messageId(), message.actionType()));
            inviterSessionOpt.ifPresent(s ->
                s.getConnection().sendMessage(messageBuilder.gameInviteDeclinedPush(game.getGameId()))
            );
            return;
        }

        gameManager.acceptGame(game.getGameId());
        connection.sendMessage(messageBuilder.ok(message.messageId(), message.actionType()));
        inviterSessionOpt.ifPresent(s ->
                s.getConnection().sendMessage(messageBuilder.gameInviteAcceptedPush(game.getGameId(), responder))
        );
    }

    /**
     * Handles game move requests for active games.
     */
    public void gameMove(Message message, Connection connection) {
        if (!requireConfiguredGame(message, connection)) return;
        Optional<Session> sessionOpt = requireSession(message, connection);
        if (sessionOpt.isEmpty()) return;
        Session  session = sessionOpt.get();

        User player = session.getUser();
        MessageBody.GameMove body = (MessageBody.GameMove) message.body();

        Optional<Game> gameOpt = gameManager.getGame(body.gameId());
        if (gameOpt.isEmpty()) {
            sendError(message, connection, ErrorCodeType.GAME_NOT_FOUND, "Game not found");
            return;
        }

        Game game = gameOpt.get();

        // This guard prevents game move injection from third parties, a bit overkill for a uni project, but
        // I'm kinda overkill
        if (!player.getUserId().equals(game.getPlayer1().getUserId())
                && !player.getUserId().equals(game.getPlayer2().getUserId())) {
            sendError(message, connection, ErrorCodeType.UNEXPECTED_MESSAGE_ACTION, "You are not a player in this game");
            return;
        }

        Move move;
        try {
            move = gameManager.getCodec().deserialize(body.rawMove());
        } catch (MalformedMessageException e) {
            sendError(message, connection, ErrorCodeType.MALFORMED_REQUEST, "Invalid move payload");
            return;
        }

        switch (game.applyMove(player, move)) {
            case MoveResult.Accepted() -> {
                connection.sendMessage(messageBuilder.ok(message.messageId(), message.actionType()));
                User opponent = game.getPlayer1().getUserId().equals(player.getUserId())
                        ? game.getPlayer2() : game.getPlayer1();
                sessionManager.getSessionByUserId(opponent.getUserId()).ifPresent(s ->
                        s.getConnection().sendMessage(
                                messageBuilder.gameMovePush(game.getGameId(), body.rawMove())
                        )
                );
            }
            case MoveResult.Rejected(String reason) ->
                    sendError(message, connection, ErrorCodeType.INVALID_MOVE, reason);
            case MoveResult.GameOver(User winner) -> {
                connection.sendMessage(messageBuilder.ok(message.messageId(), message.actionType()));
                User opponent = game.getPlayer1().getUserId().equals(player.getUserId())
                        ? game.getPlayer2() : game.getPlayer1();
                sessionManager.getSessionByUserId(opponent.getUserId()).ifPresent(s ->
                        s.getConnection().sendMessage(
                                messageBuilder.gameMovePush(game.getGameId(), body.rawMove())
                        )
                );
                pushGameOver(game, winner);
                gameManager.endGame(game.getGameId());
            }
        }
    }

    /**
     * Rejects client-originated game-over requests.
     */
    public void gameOver(Message message, Connection connection) {
        // GAME_OVER is server-initiated (PUSH only), client should never send this
        sendError(message, connection, ErrorCodeType.UNEXPECTED_MESSAGE_ACTION, "GAME_OVER is server-initiated only");
    }

    private void pushGameOver(Game game, User winner) {
        byte[] payload = messageBuilder.gameOverPush(game.getGameId(), winner);
        sessionManager.getSessionByUserId(game.getPlayer1().getUserId())
                .ifPresent(s -> s.getConnection().sendMessage(payload));
        sessionManager.getSessionByUserId(game.getPlayer2().getUserId())
                .ifPresent(s -> s.getConnection().sendMessage(payload));
    }

    private boolean requireConfiguredGame(Message message, Connection connection) {
        if (gameManager.hasFactory()) {
            return true;
        } else {
            sendError(message, connection, ErrorCodeType.INTERNAL_ERROR, "No game is configured on this server");
            return false;
        }
    }
}
