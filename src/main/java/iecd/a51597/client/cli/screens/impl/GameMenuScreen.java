package iecd.a51597.client.cli.screens.impl;

import iecd.a51597.client.Client;
import iecd.a51597.client.cli.StateMachine;
import iecd.a51597.client.cli.screens.OptionScreen;
import iecd.a51597.client.cli.screens.Screen;
import iecd.a51597.common.protocol.Message;

public class GameMenuScreen extends OptionScreen {

    protected GameMenuScreen(StateMachine sm, Client client) {
        super(sm, client);
        addOption("Invite another player", () -> sm.changeState(new SearchInviteScreen(sm, client)));
        addOption("View Invites (" + client.getPendingInvites().size() + ")", () -> sm.changeState(new ViewInvitesScreen(sm, client)));
        addOption("Back to main menu", () -> sm.changeState(new MainMenuScreen(sm, client)));
    }

    @Override
    public void handlePush(Message message) {

    }

    @Override
    public void onEnter() {

    }

    @Override
    public void onExit() {

    }
}
