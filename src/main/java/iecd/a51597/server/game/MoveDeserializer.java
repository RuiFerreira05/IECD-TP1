package iecd.a51597.server.game;

import iecd.a51597.server.protocol.exceptions.MalformedMessageException;

public interface MoveDeserializer {
    Move deserialize(String rawMove) throws MalformedMessageException;
}