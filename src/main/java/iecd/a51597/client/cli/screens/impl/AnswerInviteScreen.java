package iecd.a51597.client.cli.screens.impl;

import iecd.a51597.client.Client;
import iecd.a51597.client.cli.StateMachine;
import iecd.a51597.client.cli.screens.OptionScreen;
import iecd.a51597.client.cli.screens.Screen;
import iecd.a51597.client.cli.screens.handlers.ClientInviteHandler;
import iecd.a51597.client.game.GameController;
import iecd.a51597.common.game.dotsandboxes.DotsAndBoxesGame;
import iecd.a51597.common.protocol.Message;
import iecd.a51597.common.protocol.MessageBody;

public class AnswerInviteScreen extends OptionScreen {

    private MessageBody.GameInvitePush invite;

    public AnswerInviteScreen(StateMachine sm, Client client, MessageBody.GameInvitePush messageBody) {
        super(sm, client);
        addOption("Accept", this::accept);
        addOption("Decline", this::decline);
        addOption("Back", sm::back);
        this.invite = messageBody;
    }

    private void accept() {
        ClientInviteHandler.AnswerInviteResponse result = client.getServerConnection().getInviteHandler().answerInvite(invite, true);
        switch (result) {
            case ClientInviteHandler.AnswerInviteResponse.Success success -> {
                sm.changeState(new GameScreen(sm, client, new GameController(new DotsAndBoxesGame(
                        invite.gameId(),
                        invite.fromUserId(),
                        client.getSessionManager().getUser().userId()
                ),
                        client.getServerConnection(),
                        client.getSessionManager().getUser().userId(),
                        client.getSessionManager().getUser().username(),
                        invite.fromUsername()
                )));
            }

            case ClientInviteHandler.AnswerInviteResponse.Error error -> {
                logger.error("Failed to accept invite: ", error);
                System.out.println("Failed to accept invite: " + error.message());
                sm.changeState(new MainMenuScreen(sm, client));
            }
        }
    }

    private void decline() {
        ClientInviteHandler.AnswerInviteResponse result = client.getServerConnection().getInviteHandler().answerInvite(invite, false);
        switch (result) {
            case ClientInviteHandler.AnswerInviteResponse.Success ignored -> {
                sm.changeState(new ViewInvitesScreen(sm, client));
            }
            case ClientInviteHandler.AnswerInviteResponse.Error error -> {
                logger.error("Failed to decline invite: ", error);
                System.out.println("Failed to decline invite: " + error.message());
                sm.changeState(new ViewInvitesScreen(sm, client));
            }
        }
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
