package iecd.a51597.server.handlers;

import iecd.a51597.server.network.Connection;
import iecd.a51597.common.protocol.Message;
import iecd.a51597.common.protocol.MessageBody;
import iecd.a51597.common.protocol.builders.server.ServerMessageBuilder;
import iecd.a51597.server.store.UserStore;

/**
 * Handles username search requests.
 */
public class SearchHandler {

    private final ServerMessageBuilder messageBuilder;
    private final UserStore userStore;

    /**
     * Creates a search handler.
     */
    public SearchHandler(ServerMessageBuilder messageBuilder, UserStore userStore) {
        this.messageBuilder = messageBuilder;
        this.userStore = userStore;
    }

    /**
     * Executes a username search and returns matching users.
     */
    public void searchUsers(Message message, Connection connection) {
        MessageBody.SearchUsersRequest body = (MessageBody.SearchUsersRequest) message.body();
        connection.sendMessage(messageBuilder.searchUsersSuccess(
                message.messageId(),
                userStore.searchByUsername(body.query())
        ));
    }
}
