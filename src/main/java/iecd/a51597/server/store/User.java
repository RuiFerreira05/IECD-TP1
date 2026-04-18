package iecd.a51597.server.store;

import java.time.LocalDate;
import java.util.UUID;

public class User {

    private final UUID userId;
    private String username;
    private String passwordHash;
    private String photo; // can be null
    private String nationality;
    private LocalDate dob;
    private PlayerStats stats;

    public User(UUID userId, String username, String passwordHash, String photo) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.photo = photo;
        this.stats = new PlayerStats();
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

    public LocalDate getDob() {
        return dob;
    }

    public void setDob(LocalDate dob) {
        this.dob = dob;
    }

    public PlayerStats getStats() {
        return stats;
    }

    public void setStats(PlayerStats stats) {
        this.stats = stats;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }
}
