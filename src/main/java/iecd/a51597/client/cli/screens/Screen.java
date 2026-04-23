package iecd.a51597.client.cli.screens;

import iecd.a51597.client.Client;
import iecd.a51597.client.cli.StateMachine;
import iecd.a51597.client.config.ClientConfiguration;
import iecd.a51597.common.protocol.Message;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class Screen {

    protected StateMachine sm;
    protected Client client;
    public String prompt;

    protected Logger logger = LogManager.getLogger(this.getClass());

    protected Screen(StateMachine sm, Client client){
        this.sm = sm;
        this.client = client;
        this.prompt = ClientConfiguration.DEFAULT_PROMPT;
    }

    abstract public void display();

    abstract public void handleInput(String input);

    abstract public void handlePush(Message message);

    abstract public void onEnter();

    abstract public void onExit();
}
