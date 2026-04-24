package iecd.a51597.client.cli.screens.impl;

import iecd.a51597.client.Client;
import iecd.a51597.client.cli.StateMachine;
import iecd.a51597.client.cli.screens.OptionScreen;
import iecd.a51597.client.cli.screens.Screen;
import iecd.a51597.common.protocol.Message;
import iecd.a51597.common.protocol.MessageBody;

public class ViewInvitesScreen extends OptionScreen {

    public ViewInvitesScreen(StateMachine sm, Client client) {
        super(sm, client);
        for (MessageBody.GameInvitePush messageBody : client.getPendingInvites()) {
            addOption(messageBody.fromUsername(), () -> sm.changeState(new AnswerInviteScreen(sm, client, messageBody)));
        }
        addOption("Back", sm::back);
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
