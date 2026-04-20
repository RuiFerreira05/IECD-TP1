package iecd.a51597.server.store;

import iecd.a51597.server.store.exceptions.UsernameAlreadyTakenException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class UserStore {

    // UUID -> User
    private final ConcurrentHashMap<UUID, User> userMap = new ConcurrentHashMap<>();

    //Username -> User
    private final ConcurrentHashMap<String, User> usernameIndex = new ConcurrentHashMap<>();

    // REGISTER
    public User register(String username, String password) throws UsernameAlreadyTakenException {
        UUID userId = UUID.randomUUID();
        String passwordHash = hash(password);
        User user = new User(userId, username, passwordHash, null);

        if (usernameIndex.putIfAbsent(username, user) != null) {
            throw new UsernameAlreadyTakenException(username);
        }

        userMap.put(userId, user);
        return user;
    }

    // Package-private to allow controlled password hashing
    static String hash(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    // LOOKUP
    public Optional<User> findByCredentials(String username, String password) {
        User user = usernameIndex.get(username);
        String passwordHash = hash(password);
        if (user != null && user.getPasswordHash().equals(passwordHash)) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    public Optional<User> findById(UUID userId) {
        return Optional.ofNullable(userMap.get(userId));
    }

    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(usernameIndex.get(username));
    }

    public List<User> searchByUsername(String query) {
        List<User> users = new ArrayList<>();
        query = query.toLowerCase();
        for (User user : userMap.values()) {
            if (user.getUsername().toLowerCase().contains(query)) {
                users.add(user);
            }
        }
        return users;
    }

     // UPDATE
    public void updateUsername(User user, String newUsername) throws UsernameAlreadyTakenException {
        if (usernameIndex.putIfAbsent(newUsername, user) != null) {
            throw new UsernameAlreadyTakenException(newUsername);
        }
        usernameIndex.remove(user.getUsername());
        user.setUsername(newUsername);
    }

    /**
     * Updates a user's password. The provided password will be hashed internally.
     * @param user The user to update
     * @param newPlaintextPassword The new plaintext password (will be hashed)
     */
    public void updatePassword(User user, String newPlaintextPassword) {
        user.setPasswordHash(hash(newPlaintextPassword));
    }

    public void updatePhoto(User user, String photo) {
        user.setPhoto(photo);
    }

    // DELETION
    public void delete(User user) {
        userMap.remove(user.getUserId());
        usernameIndex.remove(user.getUsername());
    }

    // PERSISTENCE
    public void loadUser(User user) {
        userMap.put(user.getUserId(), user);
        usernameIndex.put(user.getUsername(), user);
    }

    public Collection<User> getAllUsers() {
        return userMap.values();
    }
}
