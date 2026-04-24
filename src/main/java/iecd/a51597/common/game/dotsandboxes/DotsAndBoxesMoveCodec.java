package iecd.a51597.common.game.dotsandboxes;

import iecd.a51597.common.game.Move;
import iecd.a51597.common.game.MoveCodec;
import iecd.a51597.common.protocol.exceptions.MalformedMessageException;

/**
 * Serializes/deserializes moves to/from the "x1,y1,x2,y2" string wire format.
 */
public class DotsAndBoxesMoveCodec implements MoveCodec {

    @Override
    public String serialize(Move move) {
        if (!(move instanceof DotsAndBoxesMove(int x1, int y1, int x2, int y2))) {
            throw new IllegalArgumentException("Unsupported move type provided to Codec");
        }

        return x1 + "," + y1 + "," + x2 + "," + y2;
    }

    @Override
    public Move deserialize(String rawMove) throws MalformedMessageException {
        try {
            String[] parts = rawMove.split(",");
            if (parts.length != 4) {
                throw new MalformedMessageException("Move payload requires exactly 4 coordinates separated by commas.");
            }

            int x1 = Integer.parseInt(parts[0].trim());
            int y1 = Integer.parseInt(parts[1].trim());
            int x2 = Integer.parseInt(parts[2].trim());
            int y2 = Integer.parseInt(parts[3].trim());

            // Return the pure common move directly!
            return new DotsAndBoxesMove(x1, y1, x2, y2);

        } catch (NumberFormatException e) {
            throw new MalformedMessageException("Move coordinates must be valid integers.", e);
        }
    }
}