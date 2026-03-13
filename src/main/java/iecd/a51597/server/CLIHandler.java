package iecd.a51597.server;

import java.util.Scanner;

public class CLIHandler {

    public boolean running = false;
    public Scanner scanner = new Scanner(System.in);

    void loop() {
        running = true;
        while(running) {
            System.out.print(">> ");
            handleCommand(scanner.nextLine());
        }
    }

    private void handleCommand(String s) {

    }
}
