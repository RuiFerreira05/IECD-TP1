package iecd.a51597.common.store;

import java.time.LocalDate;
import java.util.UUID;

public record UserDTO(
        UUID userId,
        String username,
        String photo, // can be null
        String nationality, // can be null
        LocalDate dob, // can be null
        PlayerStats stats // can be null
) {
    public int getAge() {
        return dob.until(LocalDate.now()).getYears();
    }
}
