package iecd.a51597.server.protocol.builders;

import iecd.a51597.server.User;
import iecd.a51597.server.protocol.types.ActionType;
import iecd.a51597.server.protocol.types.ErrorCodeType;

import javax.xml.parsers.ParserConfigurationException;
import java.util.List;
import java.util.UUID;

public interface MessageBuilder {

    //  GENERIC
    byte[] errorNoId(ErrorCodeType errorCode, String description) throws ParserConfigurationException;
    byte[] error(UUID messageId, ActionType actionType, ErrorCodeType errorCode, String description) throws ParserConfigurationException;
    byte[] ok(UUID messageId);

    // AUTH
    byte[] loginSuccess(UUID messageId, UUID sessionToken, User user);

    // SEARCH
    byte[] searchUsersSuccess(UUID messageId, List<User> results);

    // GAME - TBD
    byte[] gameInviteResponse(UUID messageId, UUID gameId);

    byte[] gameInvitePush(UUID gameId, User fromUser);

    byte[] gameInviteAcceptedPush(UUID gameId, User user);

    byte[] gameInviteDeclinedPush(UUID gameId);

    byte[] gameMovePush(UUID gameId, String movePayload);

    byte[] gameOverPush(UUID gameId, User winner);
}