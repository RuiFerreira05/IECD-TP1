package iecd.a51597.server.game;

import iecd.a51597.server.store.User;

import java.util.UUID;

public interface Game {
    UUID getGameId();
    User getPlayer1();
    User getPlayer2();
    MoveResult applyMove(User player, String movePayload);
}