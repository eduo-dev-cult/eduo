package se.ltu.eduo;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.boot.test.util.TestPropertyValues;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Starts a PostgreSQL Testcontainer and injects its connection details
 * into the Spring context before it finishes loading. This ensures the
 * datasource URL, username and password are available before Hibernate
 * or any other bean tries to connect.
 *
 * withInitScript runs create-schema.sql against the container immediately
 * after it starts, so the eduo schema exists before Hibernate creates tables.
 */
public class TestContainersInitializer implements
        ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("eduo-test")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("create-schema.sql");

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        if (!postgres.isRunning()) {
            postgres.start();
        }

        TestPropertyValues.of(
                "spring.datasource.url=" + postgres.getJdbcUrl(),
                "spring.datasource.username=" + postgres.getUsername(),
                "spring.datasource.password=" + postgres.getPassword()
        ).applyTo(applicationContext.getEnvironment());
    }
}