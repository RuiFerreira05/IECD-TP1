package iecd.a51597.common.protocol.builders;

import iecd.a51597.server.store.User;
import iecd.a51597.common.protocol.types.ActionType;
import iecd.a51597.common.protocol.types.ErrorCodeType;

import java.util.List;
import java.util.UUID;

public interface MessageBuilder {

    //  GENERIC
    byte[] errorNoId(ErrorCodeType errorCode, String description);

    byte[] error(UUID messageId, ActionType actionType, ErrorCodeType errorCode, String description);

    byte[] ok(UUID messageId, ActionType actionType);

    // AUTH
    byte[] loginSuccess(UUID messageId, UUID sessionToken, User user);

    // SEARCH
    byte[] searchUsersSuccess(UUID messageId, List<User> results);

    // GAME - TBD
    byte[] gameInviteResponse(UUID messageId, UUID gameId);

    byte[] gameInvitePush(UUID gameId, User fromUser);

    byte[] gameInviteAcceptedPush(UUID gameId, User user);

    byte[] gameInviteDeclinedPush(UUID gameId);

    byte[] gameMovePush(UUID gameId, String rawMove);

    byte[] gameOverPush(UUID gameId, User winner);
}