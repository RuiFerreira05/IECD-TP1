package iecd.a51597.server.protocol;

import java.util.UUID;

public final class ProtocolConstants {

    // This class shouldn't be instantiated
    private ProtocolConstants() {}

    // Server received a message it couldn't retrieve the id from
    public static final UUID ERROR_NO_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");
}