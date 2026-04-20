package iecd.a51597.server.game;

import iecd.a51597.common.protocol.exceptions.MalformedMessageException;

public interface MoveCodec {
    String serialize(Move move);
    Move deserialize(String rawMove) throws MalformedMessageException;
}