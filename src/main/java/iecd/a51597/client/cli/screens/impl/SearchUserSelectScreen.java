package iecd.a51597.client.cli.screens.impl;

import iecd.a51597.client.Client;
import iecd.a51597.client.cli.StateMachine;
import iecd.a51597.client.cli.screens.OptionScreen;
import iecd.a51597.common.protocol.Message;
import iecd.a51597.common.store.UserDTO;

import java.util.List;

public class SearchUserSelectScreen extends OptionScreen {

    public SearchUserSelectScreen(StateMachine sm, Client client) {
        super(sm, client);
        addOption("back", sm::back);
    }

    @Override
    public void handlePush(Message message) {

    }

    @Override
    public void handleArgs(Object[] args) {
        clearOptions();
        addOption("go back", sm::back);

        if (args == null || args.length == 0 || !(args[0] instanceof List<?> rawUsers)) {
            logger.warn("SearchUserSelectScreen expected List<UserDTO> in args[0], got: {}",
                    args == null || args.length == 0 ? "<missing>" : args[0].getClass().getName());
            return;
        }

        for (Object rawUser : rawUsers) {
            if (!(rawUser instanceof UserDTO user)) {
                logger.warn("SearchUserSelectScreen received non-UserDTO entry: {}",
                        rawUser == null ? "null" : rawUser.getClass().getName());
                continue;
            }
            addOption(user.username(), () -> sm.transitionTo("view-profile", new Object[]{user}));
        }
    }

    @Override
    public void onEnter() {
        logger.info("Entering SearchUserSelectScreen");
    }

    @Override
    public void onExit() {
        logger.info("Exiting SearchUserSelectScreen");
    }
}
