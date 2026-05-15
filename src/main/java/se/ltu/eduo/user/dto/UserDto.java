package se.ltu.eduo.user.dto;

import lombok.Value;
import se.ltu.eduo.user.model.User;

import java.io.Serializable;
import java.time.Instant;

/**
 * DTO for {@link User}
 */
@Value
public class UserDto implements Serializable {
    Integer id;
    String firstName;
    String lastName;
    Instant createdAt;
    Instant updatedAt;
}