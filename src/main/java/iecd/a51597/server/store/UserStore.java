package iecd.a51597.server.store;

import iecd.a51597.server.store.exceptions.UsernameAlreadyTakenException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class UserStore {

    // UUID -> User
    private final ConcurrentHashMap<UUID, User> userMap = new ConcurrentHashMap<>();

    //Username -> User
    private final ConcurrentHashMap<String, User> usernameIndex = new ConcurrentHashMap<>();

    // REGISTER
    public User register(String username, String passwordHash) throws UsernameAlreadyTakenException {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, username, passwordHash, null);

        if (usernameIndex.putIfAbsent(username, user) != null) {
            throw new UsernameAlreadyTakenException(username);
        }

        userMap.put(userId, user);
        return user;
    }

    // LOOKUP
    public Optional<User> findByCredentials(String username, String passwordHash) {
        User user = usernameIndex.get(username);
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

    public void updatePassword(User user, String newPasswordHash) {
        user.setPasswordHash(newPasswordHash);
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
