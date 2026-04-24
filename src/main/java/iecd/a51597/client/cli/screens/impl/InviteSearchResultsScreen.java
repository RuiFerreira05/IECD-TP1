package iecd.a51597.client.cli.screens.impl;

import iecd.a51597.client.Client;
import iecd.a51597.client.cli.StateMachine;
import iecd.a51597.client.cli.screens.OptionScreen;
import iecd.a51597.common.protocol.Message;
import iecd.a51597.common.store.UserDTO;

import java.util.List;

public class InviteSearchResultsScreen extends OptionScreen {

    public InviteSearchResultsScreen(StateMachine sm, Client client, List<UserDTO> users) {
        super(sm, client);
        addOption("back", sm::back);
        for (UserDTO user : users) {
            addOption(user.username(), () -> sm.changeState(new InvitePendingScreen(sm, client, user)));
        }
    }

    @Override
    public void handlePush(Message message) {

    }

    @Override
    public void onEnter() {
        logger.info("Entering SearchResultsScreen");
    }

    @Override
    public void onExit() {
        logger.info("Exiting SearchResultsScreen");
    }
}
