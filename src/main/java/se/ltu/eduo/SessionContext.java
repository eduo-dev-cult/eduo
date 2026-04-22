package se.ltu.eduo;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import se.ltu.eduo.model.User;
import se.ltu.eduo.model.UserPreferences;

/**
 * Ska hålla reda på saker relaterat till nuvarande session och användare.
 * Mest för att testa databasimplementationen för tillfället, kan tas bort!
 */
@Component
@Getter
@Setter
public class SessionContext {
    private User currentUser;
    private UserPreferences preferences;
}
