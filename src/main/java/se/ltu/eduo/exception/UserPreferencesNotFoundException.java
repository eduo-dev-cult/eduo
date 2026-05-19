package se.ltu.eduo.exception;

public class UserPreferencesNotFoundException extends RuntimeException {
    public UserPreferencesNotFoundException(Integer userId) {
        super("User preferences not found for userId: " + userId);
    }
}