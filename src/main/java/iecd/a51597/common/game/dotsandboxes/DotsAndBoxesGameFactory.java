package iecd.a51597.common.game.dotsandboxes;

import iecd.a51597.common.game.Game;
import iecd.a51597.common.game.GameFactory;
import iecd.a51597.common.game.MoveCodec;

import java.util.UUID;

/**
 * Pluggable factory to instantiate the Dots and Boxes engine.
 */
public class DotsAndBoxesGameFactory implements GameFactory {

    private final MoveCodec codec = new DotsAndBoxesMoveCodec();

    @Override
    public Game createGame(UUID gameId, UUID player1Id, UUID player2Id) {
        return new DotsAndBoxesGame(gameId, player1Id, player2Id);
    }

    @Override
    public MoveCodec getMoveCodec() {
        return codec;
    }
}