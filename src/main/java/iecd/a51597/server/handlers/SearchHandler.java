package iecd.a51597.server.handlers;

import iecd.a51597.server.network.Connection;
import iecd.a51597.common.protocol.Message;
import iecd.a51597.common.protocol.MessageBody;
import iecd.a51597.common.protocol.builders.MessageBuilder;
import iecd.a51597.server.store.UserStore;

/**
 * Handles username search requests.
 */
public class SearchHandler {

    private final MessageBuilder messageBuilder;
    private final UserStore userStore;

    /**
     * Creates a search handler.
     */
    public SearchHandler(MessageBuilder messageBuilder, UserStore userStore) {
        this.messageBuilder = messageBuilder;
        this.userStore = userStore;
    }

    /**
     * Executes a username search and returns matching users.
     */
    public void searchUsers(Message message, Connection connection) {
        MessageBody.SearchUsers body = (MessageBody.SearchUsers) message.body();
        connection.sendMessage(messageBuilder.searchUsersSuccess(
                message.messageId(),
                userStore.searchByUsername(body.query())
        ));
    }
}
