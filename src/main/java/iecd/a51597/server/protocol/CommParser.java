package iecd.a51597.server.protocol;

import iecd.a51597.server.protocol.errors.CommError;

import java.io.InputStream;

public interface CommParser {
    Message parseMessage(InputStream input) throws CommError;
}
