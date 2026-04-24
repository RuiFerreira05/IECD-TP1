package iecd.a51597.client.cli.screens.impl;

import iecd.a51597.client.Client;
import iecd.a51597.client.cli.StateMachine;
import iecd.a51597.client.cli.screens.Screen;
import iecd.a51597.client.cli.screens.handlers.ClientSearchHandler;
import iecd.a51597.common.protocol.Message;
import iecd.a51597.common.store.UserDTO;

import java.util.List;

public class SearchForPlayerScreen extends Screen {

    public SearchForPlayerScreen(StateMachine sm, Client client) {
        super(sm, client);
    }

    @Override
    public void display() {
        System.out.println("Type to search for new players (\"back\" to go to menu):");
    }

    @Override
    public void handleInput(String input) {
        if (input.equals("back")) {
            sm.transitionTo("main");
        }
        if (client.getServerConnection().getSearchHandler().searchPlayers(input) instanceof ClientSearchHandler.SearchPlayerResult.SUCCESS(List<UserDTO> newUsers)) {
            sm.transitionTo("searchSelect", new Object[]{newUsers});
        }
    }

    @Override
    public void handlePush(Message message) {

    }

    @Override
    public void handleArgs(Object[] args) {

    }

    @Override
    public void onEnter() {
        logger.info("Entering SearchForPlayerScreen");
    }

    @Override
    public void onExit() {
        logger.info("Exiting SearchForPlayerScreen");
    }
}
