package iecd.a51597.server.store;

import java.util.UUID;

public class User {

    private UUID userId;
    private String username;
    private String passwordHash;
    private String photo; // can be null

    public User(UUID userId, String username, String passwordHash, String photo) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.photo = photo;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }
}
