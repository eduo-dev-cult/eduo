package se.ltu.eduo.dto;

import lombok.Value;

import java.io.Serializable;
import java.time.Instant;

/**
 * DTO for {@link se.ltu.eduo.model.User}
 */
@Value
public class UserDto implements Serializable {
    Integer id;
    String firstName;
    String lastName;
    Instant createdAt;
    Instant updatedAt;
}