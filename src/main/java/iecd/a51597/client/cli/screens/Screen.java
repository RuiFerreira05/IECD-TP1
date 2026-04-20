package iecd.a51597.client.cli.screens;

import iecd.a51597.client.Client;
import iecd.a51597.client.cli.StateMachine;

public abstract class Screen {

    protected StateMachine sm;
    protected Client client;
    public String prompt;

    protected Screen(StateMachine sm, Client client){
        this.sm = sm;
        this.client = client;
        this.prompt = ">> ";
    }

    abstract public void display();

    abstract public void handleInput(String input);

    abstract public void onEnter();

    abstract public void onExit();
}
