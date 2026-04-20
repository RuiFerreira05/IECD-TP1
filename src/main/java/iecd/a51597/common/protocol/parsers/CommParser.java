package iecd.a51597.common.protocol.parsers;

import iecd.a51597.common.protocol.Message;
import iecd.a51597.common.protocol.exceptions.CommException;

import java.io.InputStream;

public interface CommParser {
    Message parseMessage(InputStream input) throws CommException;
}
