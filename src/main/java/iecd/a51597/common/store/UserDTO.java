package iecd.a51597.common.store;

import java.time.LocalDate;
import java.util.UUID;

public record UserDTO(
        UUID userId,
        String username,
        String photo, // can be null
        String nationality,
        LocalDate dob,
        PlayerStats stats
) {
}
