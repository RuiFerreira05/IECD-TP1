package iecd.a51597.server.session;

import iecd.a51597.server.network.Connection;
import iecd.a51597.server.store.User;

import java.time.Instant;
import java.util.UUID;

public class Session {

    private final User user;
    private final UUID token;
    private final UUID userId;
    private final Connection connection;
    private volatile Instant lastActivity;

    public Session(User user, Connection connection) {
        this.token = UUID.randomUUID();
        this.user = user;
        this.userId = user.getUserId();
        this.connection = connection;
        this.lastActivity = Instant.now();
    }

    public boolean isExpired(long timeoutSeconds) {
        return Instant.now().isAfter(lastActivity.plusSeconds(timeoutSeconds));
    }

    public void refresh() {
        this.lastActivity = Instant.now();
    }

    public UUID getToken()            { return token; }
    public UUID getUserId()           { return userId; }
    public User getUser()             { return user; }
    public Connection getConnection() { return connection; }
    public Instant getLastActivity()  { return lastActivity; }
}