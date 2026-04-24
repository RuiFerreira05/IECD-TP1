package iecd.a51597.client.cli.screens.impl;

import iecd.a51597.client.Client;
import iecd.a51597.client.cli.StateMachine;
import iecd.a51597.client.cli.screens.Screen;
import iecd.a51597.client.cli.screens.handlers.ClientInviteHandler;
import iecd.a51597.client.game.GameController;
import iecd.a51597.common.game.dotsandboxes.DotsAndBoxesGame;
import iecd.a51597.common.protocol.Message;
import iecd.a51597.common.protocol.MessageBody;
import iecd.a51597.common.store.UserDTO;

import java.util.UUID;

public class InvitePendingScreen extends Screen {

    private final UserDTO target;

    public InvitePendingScreen(StateMachine sm, Client client, UserDTO target) {
        super(sm, client);
        this.target = target;
    }

    @Override
    public void display() {

    }

    @Override
    public void handleInput(String input) {

    }

    @Override
    public void handlePush(Message message) {
        if (message.body() instanceof MessageBody.GameInviteResponsePush(
                UUID gameId, boolean accepted, String opponentUsername
        )) {
            if (accepted) {
                System.out.println(opponentUsername + " accepted your invite! Press Enter to start the game");
                sm.changeState(new GameScreen(sm, client, new GameController(new DotsAndBoxesGame(
                        gameId,
                        client.getSessionManager().getUser().userId(),
                        target.userId()
                ),
                        client.getServerConnection(),
                        client.getSessionManager().getUser().userId(),
                        client.getSessionManager().getUser().username(),
                        target.username()
                )));
            } else {
                System.out.println(opponentUsername + " rejected your invite. Press Enter to return to the main menu.");
                sm.changeState(new MainMenuScreen(sm, client));
            }
        }
    }

    @Override
    public void onEnter() {
        switch (client.getServerConnection().getInviteHandler().sendInvite(target)) {
            case ClientInviteHandler.InviteResult.Success(UUID gameId) -> {
                System.out.println("Invite sent to " + target.username() + ", waiting for response...");
            }
            case ClientInviteHandler.InviteResult.Error(String message) -> System.out.println("Failed to send invite to " + target.username() + ": " + message);
        }
    }

    @Override
    public void onExit() {

    }
}
