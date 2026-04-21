package iecd.a51597.client.cli.network;

public record Push(
        PushType pushType,
        String message
) {}
