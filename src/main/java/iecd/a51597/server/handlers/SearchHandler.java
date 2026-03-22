package iecd.a51597.server.handlers;

import iecd.a51597.server.network.Connection;
import iecd.a51597.server.protocol.Message;
import iecd.a51597.server.protocol.MessageBody;
import iecd.a51597.server.protocol.builders.MessageBuilder;
import iecd.a51597.server.store.UserStore;

public class SearchHandler {

    private final MessageBuilder messageBuilder;
    private final UserStore userStore;

    public SearchHandler(MessageBuilder messageBuilder, UserStore userStore) {
        this.messageBuilder = messageBuilder;
        this.userStore = userStore;
    }

    public void searchUsers(Message message, Connection connection) {
        MessageBody.SearchUsers body = (MessageBody.SearchUsers) message.body();
        connection.sendMessage(messageBuilder.searchUsersSuccess(
                message.messageId(),
                userStore.searchByUsername(body.query())
        ));
    }
}
