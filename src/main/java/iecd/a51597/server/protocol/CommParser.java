package iecd.a51597.server.protocol;

import iecd.a51597.server.protocol.exceptions.CommException;

import java.io.InputStream;

public interface CommParser {
    Message parseMessage(InputStream input) throws CommException;
}
