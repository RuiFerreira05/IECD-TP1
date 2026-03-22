package iecd.a51597.server.handlers;

import iecd.a51597.server.Connection;
import iecd.a51597.server.GameManager;
import iecd.a51597.server.Session;
import iecd.a51597.server.SessionManager;
import iecd.a51597.server.game.Game;
import iecd.a51597.server.game.MoveResult;
import iecd.a51597.server.protocol.Message;
import iecd.a51597.server.protocol.MessageBody;
import iecd.a51597.server.protocol.builders.MessageBuilder;
import iecd.a51597.server.protocol.types.ErrorCodeType;
import iecd.a51597.server.store.User;
import iecd.a51597.server.store.UserStore;

import java.util.Optional;

public class GameHandler extends BaseHandler {

    private final UserStore userStore;
    private final GameManager gameManager;

    public GameHandler(MessageBuilder messageBuilder, SessionManager sessionManager, UserStore userStore, GameManager gameManager) {
        super(messageBuilder, sessionManager);
        this.userStore = userStore;
        this.gameManager = gameManager;
    }

    public void gameInvite(Message message, Connection connection) {
        Optional<Session> sessionOpt = requireSession(message, connection);
        if (sessionOpt.isEmpty()) return;
        Session session = sessionOpt.get();

        User sender = session.getUser();

        if (gameManager.isInGame(sender.getUserId())) {
            sendError(message, connection, ErrorCodeType.ALREADY_IN_GAME, "You are already in a game");
            return;
        }

        MessageBody.GameInvite body = (MessageBody.GameInvite) message.body();

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

        Game game = gameManager.createGame(sender, target);

        connection.sendMessage(messageBuilder.gameInviteResponse(message.messageId(), game.getGameId()));
        targetSession.getConnection().sendMessage(
                messageBuilder.gameInvitePush(game.getGameId(), sender)
        );
    }

    public void gameInviteResponse(Message message, Connection connection) {
        Optional<Session> sessionOpt = requireSession(message, connection);
        if (sessionOpt.isEmpty()) return;
        Session session = sessionOpt.get();

        User responder = session.getUser();

        MessageBody.GameInviteResponse body = (MessageBody.GameInviteResponse) message.body();

        Optional<Game> gameOpt = gameManager.getGame(body.gameId());
        if (gameOpt.isEmpty()) {
            sendError(message, connection, ErrorCodeType.USER_NOT_FOUND, "Game not found");
            return;
        }
        Game game = gameOpt.get();

        User inviter = game.getPlayer1();

        Optional<Session> inviterSessionOpt = sessionManager.getSessionByUserId(inviter.getUserId());

        if (!body.accept()) {
            connection.sendMessage(messageBuilder.ok(message.messageId(), message.actionType()));
            gameManager.endGame(game.getGameId());
            inviterSessionOpt.ifPresent(s ->
                s.getConnection().sendMessage(messageBuilder.gameInviteDeclinedPush(game.getGameId()))
            );
            return;
        }

        connection.sendMessage(messageBuilder.ok(message.messageId(), message.actionType()));
        inviterSessionOpt.ifPresent(s ->
                s.getConnection().sendMessage(messageBuilder.gameInviteAcceptedPush(game.getGameId(), responder))
        );
    }

    public void gameMove(Message message, Connection connection) {
        Optional<Session> sessionOpt = requireSession(message, connection);
        if (sessionOpt.isEmpty()) return;

        User player = sessionOpt.get().getUser();
        MessageBody.GameMove body = (MessageBody.GameMove) message.body();

        Optional<Game> gameOpt = gameManager.getGame(body.gameId());
        if (gameOpt.isEmpty()) {
            sendError(message, connection, ErrorCodeType.USER_NOT_FOUND, "Game not found");
            return;
        }

        Game game = gameOpt.get();
        String movePayload = body.move().getTextContent();

        switch (game.applyMove(player, movePayload)) {
            case MoveResult.Accepted() -> {
                connection.sendMessage(messageBuilder.ok(message.messageId(), message.actionType()));
                User opponent = game.getPlayer1().getUserId().equals(player.getUserId()) ? game.getPlayer2() : game.getPlayer1();
                sessionManager.getSessionByUserId(opponent.getUserId()).ifPresent(s ->
                        s.getConnection().sendMessage(messageBuilder.gameMovePush(game.getGameId(), movePayload))
                );
            }
            case MoveResult.Rejected(String reason) ->
                    sendError(message, connection, ErrorCodeType.INVALID_MOVE, reason);
            case MoveResult.GameOver(User winner) -> {
                connection.sendMessage(messageBuilder.ok(message.messageId(), message.actionType()));
                pushGameOver(game, winner);
                gameManager.endGame(game.getGameId());
            }
        }
    }

    public void gameOver(Message message, Connection connection) {
        // GAME_OVER is server-initiated (PUSH only) — client should never send this
        sendError(message, connection, ErrorCodeType.UNEXPECTED_MESSAGE_ACTION, "GAME_OVER is server-initiated only");
    }

    private void pushGameOver(Game game, User winner) {
        byte[] payload = messageBuilder.gameOverPush(game.getGameId(), winner);
        sessionManager.getSessionByUserId(game.getPlayer1().getUserId())
                .ifPresent(s -> s.getConnection().sendMessage(payload));
        sessionManager.getSessionByUserId(game.getPlayer2().getUserId())
                .ifPresent(s -> s.getConnection().sendMessage(payload));
    }
}
