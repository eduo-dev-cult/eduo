package se.ltu.eduo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import se.ltu.eduo.model.User;
import se.ltu.eduo.service.AuthService;

//implements commandlinerunner to run after initialise finishes for testing
@SpringBootApplication
public class EduoApplication implements CommandLineRunner {

    @Autowired
    private final AuthService authService;

    public EduoApplication(AuthService authService)
    {
        this.authService = authService;
    }

    public static void main(String[] args)
    {
        SpringApplication.run(EduoApplication.class, args);
    }

    //runs after init, only for testing
    //todo remove
    @Override
    public void run(String... args)
    {
        //User testuser = authService.createUser("asdf", "fdsa", "asddsa-1", "hunter2");
        //System.out.println("Created an user for this one: "+testuser.getFirstName());

        System.out.println("Did we log in? answer is " +authService.LogInUser("asddsa-1", "hunter2"));
    }
}
