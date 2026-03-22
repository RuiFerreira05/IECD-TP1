package iecd.a51597.server.store;

import java.util.UUID;

public class User {

    private UUID userId;
    private String username;
    private String passwordHash;
    private String photo;

    public User(UUID userId, String username, String passwordHash, String photo) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.photo = photo;
    }

    public String getUsername() {
        return username;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getPhoto() {
        return photo;
    }
}
