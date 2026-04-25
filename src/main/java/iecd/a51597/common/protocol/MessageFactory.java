package iecd.a51597.common.protocol;

import iecd.a51597.common.protocol.types.ActionType;
import iecd.a51597.common.protocol.types.MessageType;
import iecd.a51597.common.store.UserDTO;

import java.time.LocalDate;
import java.util.UUID;

public class MessageFactory {

    private MessageFactory() {
    }

    public static Message buildLoginRequest(String protocolVersion, UUID uuid, String username, String password) {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }

        return new Message(
                uuid,
                MessageType.REQUEST,
                protocolVersion,
                ActionType.LOGIN,
                null,
                new MessageBody.LoginRequest(username, password)
        );
    }

    public static Message buildRegisterRequest(String protocolVersion, UUID uuid, String username, String password) {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }

        return new Message(
                uuid,
                MessageType.REQUEST,
                protocolVersion,
                ActionType.REGISTER,
                null,
                new MessageBody.Register(username, password)
        );
    }

    public static Message buildLogoutRequest(String protocolVersion, UUID uuid, UUID sessionToken) {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }

        return new Message(
                uuid,
                MessageType.REQUEST,
                protocolVersion,
                ActionType.LOGOUT,
                sessionToken,
                new MessageBody.Logout()
        );
    }

    public static Message buildUpdateProfileRequest(String protocolVersion, UUID sessionToken, String username, String password, byte[] photo, String nationality, LocalDate dob) {
        return new Message(
                UUID.randomUUID(),
                MessageType.REQUEST,
                protocolVersion,
                ActionType.UPDATE_PROFILE,
                sessionToken,
                new MessageBody.UpdateProfile(username, password, photo, nationality, dob));
    }

    public static Message buildSearchRequest(String protocolVersion, String query) {
        return new Message(
                UUID.randomUUID(),
                MessageType.REQUEST,
                protocolVersion,
                ActionType.SEARCH_USERS,
                null,
                new MessageBody.SearchUsersRequest(
                        query
                )
        );
    }

    public static Message createMoveRequest(String protocolVersion, UUID sessionUUID, UUID gameId, String rawMove) {
        return new Message(
                UUID.randomUUID(),
                MessageType.REQUEST,
                protocolVersion,
                ActionType.GAME_MOVE,
                sessionUUID,
                new MessageBody.GameMove(gameId, rawMove)
        );
    }

    public static Message buildSendInviteRequest(String protocolVersion, UUID sessionToken, UUID targetId) {
        return new Message(
                UUID.randomUUID(),
                MessageType.REQUEST,
                protocolVersion,
                ActionType.GAME_INVITE,
                sessionToken,
                new MessageBody.GameInviteRequest(targetId)
        );
    }

    public static Message buildAcceptInviteRequest(String protocolVersion, UUID sessionUUID, UUID gameId, boolean response) {
        return new Message(
                UUID.randomUUID(),
                MessageType.REQUEST,
                protocolVersion,
                ActionType.GAME_INVITE_RESPONSE,
                sessionUUID,
                new MessageBody.GameInviteResponseRequest(gameId, response)
        );
    }
}
