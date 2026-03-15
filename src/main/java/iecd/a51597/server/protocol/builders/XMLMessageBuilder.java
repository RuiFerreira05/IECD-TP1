package iecd.a51597.server.protocol.builders;

import iecd.a51597.server.User;
import iecd.a51597.server.protocol.types.ErrorCodeType;

import javax.xml.parsers.DocumentBuilderFactory;
import java.util.List;
import java.util.UUID;

public class XMLMessageBuilder implements MessageBuilder {

    private final DocumentBuilderFactory dbf;

    public XMLMessageBuilder() {
        dbf = DocumentBuilderFactory.newInstance();
        dbf.setValidating(false);
        dbf.setIgnoringComments(true);
        dbf.setNamespaceAware(true);
    }

    @Override
    public byte[] errorNoId(ErrorCodeType errorCode, String description) {
        return null; //TODO
    }

    @Override
    public byte[] error(UUID messageId, ErrorCodeType errorCode, String description) {
        return null; //TODO
    }

    @Override
    public byte[] ok(UUID messageId) {
        return null; //TODO
    }

    @Override
    public byte[] loginSuccess(UUID messageId, UUID sessionToken, User user) {
        return null; //TODO
    }

    @Override
    public byte[] searchUsersSuccess(UUID messageId, List<User> results) {
        return null; //TODO
    }

    @Override
    public byte[] gameInviteResponse(UUID messageId, UUID gameId) {
        return null; //TODO
    }

    @Override
    public byte[] gameInvitePush(UUID gameId, User fromUser) {
        return null; //TODO
    }

    @Override
    public byte[] gameInviteAcceptedPush(UUID gameId, User user) {
        return null; //TODO
    }

    @Override
    public byte[] gameInviteDeclinedPush(UUID gameId) {
        return null; //TODO
    }

    @Override
    public byte[] gameMovePush(UUID gameId, String movePayload) {
        return null; //TODO
    }

    @Override
    public byte[] gameOverPush(UUID gameId, User winner) {
        return null; //TODO
    }
}
