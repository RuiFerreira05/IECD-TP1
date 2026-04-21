package iecd.a51597.client.cli.screens;

import iecd.a51597.client.Client;
import iecd.a51597.client.cli.StateMachine;
import iecd.a51597.client.cli.network.Push;
import iecd.a51597.client.config.ClientConfiguration;

public abstract class Screen {

    protected StateMachine sm;
    protected Client client;
    public String prompt;

    protected Screen(StateMachine sm, Client client){
        this.sm = sm;
        this.client = client;
        this.prompt = ClientConfiguration.DEFAULT_PROMPT;
    }

    abstract public void display();

    abstract public void handleInput(String input);

    abstract public void handlePush(Push push);

    abstract public void onEnter();

    abstract public void onExit();
}
