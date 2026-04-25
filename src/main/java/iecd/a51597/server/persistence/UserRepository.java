package iecd.a51597.server.persistence;

import iecd.a51597.server.store.UserStore;

public interface UserRepository {
    void loadInto(UserStore userStore);
    void saveFrom(UserStore userStore);

    /**
     * saves a photo in the way it's implementation decides (e.g. on the file filesystem).
     * @param photo the photo to be saved
     * @return a reference to the stored photo
     */
    String savePhoto(byte[] photo, String oldPhoto);
}
