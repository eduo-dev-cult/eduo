package se.ltu.eduo.config;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import se.ltu.eduo.repository.UserRepository;
import se.ltu.eduo.service.AuthService;

@Component
public class DemoDataLoader {

    private final UserRepository userRepository;
    private final AuthService authService;

    public DemoDataLoader(
            UserRepository userRepository,
            AuthService authService
    ) {
        this.userRepository = userRepository;
        this.authService = authService;
    }

    @PostConstruct
    public void loadDemoUser() {

        boolean demoUserExists = userRepository.findAll()
                .stream()
                .anyMatch(user ->
                        "Demo".equals(user.getFirstName())
                                && "User".equals(user.getLastName())
                );

        if (!demoUserExists) {

            authService.createUser(
                    "Demo",
                    "User",
                    "demo",
                    "demo"
            );
        }
    }
}