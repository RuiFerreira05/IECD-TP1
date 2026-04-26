package iecd.a51597.common.game.dotsandboxes;

import iecd.a51597.common.protocol.exceptions.MalformedMessageException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DotsAndBoxesMoveCodecTest {

    private final DotsAndBoxesMoveCodec codec = new DotsAndBoxesMoveCodec();

    @Test
    void serialize_correctFormat() {
        DotsAndBoxesMove move = new DotsAndBoxesMove(0, 0, 1, 0);
        String result = codec.serialize(move);
        assertEquals("0,0,1,0", result);
    }

    @Test
    void deserialize_validString() throws MalformedMessageException {
        DotsAndBoxesMove move = (DotsAndBoxesMove) codec.deserialize("1,2,1,3");
        assertEquals(1, move.x1());
        assertEquals(2, move.y1());
        assertEquals(1, move.x2());
        assertEquals(3, move.y2());
    }

    @Test
    void deserialize_invalidFormat_throws() {
        assertThrows(MalformedMessageException.class, () -> codec.deserialize("1,2,3"));
        assertThrows(MalformedMessageException.class, () -> codec.deserialize("1,2,a,4"));
    }

    @Test
    void normalization_isHandledByMoveItself() {
        // Codec should serialize what the move gives it.
        // Move(1,0, 0,0) becomes Move(0,0, 1,0)
        DotsAndBoxesMove move = new DotsAndBoxesMove(1, 0, 0, 0);
        assertEquals(0, move.x1());
        assertEquals(0, move.y1());
        assertEquals(1, move.x2());
        assertEquals(0, move.y2());
        
        assertEquals("0,0,1,0", codec.serialize(move));
    }
}
