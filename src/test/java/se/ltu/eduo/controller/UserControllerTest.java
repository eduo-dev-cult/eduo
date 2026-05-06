package se.ltu.eduo.controller;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import se.ltu.eduo.TestContainersInitializer;
import se.ltu.eduo.service.AuthService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestContainersInitializer.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    // ---------------------------------------------------------------
    // POST /users  (register)
    // ---------------------------------------------------------------

    @Test
    void register_returns201WithIdAndLocationHeader() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Anna","lastName":"Larsson",
                                 "username":"alarsson","password":"hunter2"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.firstName").value("Anna"))
                .andExpect(jsonPath("$.lastName").value("Larsson"));
    }

    @Test
    void register_returns409_whenUsernameAlreadyExists() throws Exception {
        authService.createUser("Anna", "Larsson", "alarsson", "hunter2");

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Bob","lastName":"Smith",
                                 "username":"alarsson","password":"pass"}
                                """))
                .andExpect(status().isConflict());
    }

    // ---------------------------------------------------------------
    // POST /users/login
    // ---------------------------------------------------------------

    @Test
    void login_returns200WithUserData_whenCredentialsAreValid() throws Exception {
        authService.createUser("Anna", "Larsson", "alarsson", "hunter2");

        mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alarsson","password":"hunter2"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Anna"));
    }

    @Test
    void login_returns401_whenPasswordIsWrong() throws Exception {
        authService.createUser("Anna", "Larsson", "alarsson", "hunter2");

        mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alarsson","password":"wrong"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_returns401_whenUsernameIsUnknown() throws Exception {
        mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"nobody","password":"irrelevant"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------
    // GET /users/{id}
    // currently disabled because it's unclear if retrieving users should be supported outside of login
    // ---------------------------------------------------------------

    @Test
    @Disabled("Endpoint disabled pending evaluation of needs")
    void getUser_returns200WithUserData_whenExists() throws Exception {
        Integer id = authService.createUser("Anna", "Larsson", "alarsson", "hunter2").getId();

        mockMvc.perform(get("/users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.firstName").value("Anna"));
    }

    @Test
    @Disabled("Endpoint disabled pending evaluation of needs")
    void getUser_returns404_whenNotFound() throws Exception {
        mockMvc.perform(get("/users/{id}", Integer.MAX_VALUE))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------
    // DELETE /users/{id}
    // ---------------------------------------------------------------

    @Test
    void deleteUser_returns204_whenExists() throws Exception {
        Integer id = authService.createUser("Anna", "Larsson", "alarsson", "hunter2").getId();

        mockMvc.perform(delete("/users/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteUser_returns204_whenUserDoesNotExist() throws Exception {
        // deleteById fails silently — documented behaviour
        mockMvc.perform(delete("/users/{id}", Integer.MAX_VALUE))
                .andExpect(status().isNoContent());
    }
}
