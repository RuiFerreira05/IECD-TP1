package iecd.a51597.server.persistence;

import iecd.a51597.server.store.UserStore;

public interface UserRepository {
    void loadInto(UserStore userStore);
    void saveFrom(UserStore userStore);
}
