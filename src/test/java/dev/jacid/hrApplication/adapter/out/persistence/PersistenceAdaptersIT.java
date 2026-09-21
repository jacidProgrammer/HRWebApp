package dev.jacid.hrApplication.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import dev.jacid.hrApplication.adapter.out.persistence.mappers.EmployeePersistenceMapperImpl;
import dev.jacid.hrApplication.adapter.out.persistence.mappers.FeedbackPersistenceMapperImpl;
import dev.jacid.hrApplication.domain.model.Employee;
import dev.jacid.hrApplication.domain.model.Feedback;
import dev.jacid.hrApplication.domain.model.Sentiment;

/**
 * Runs the JPA adapters, the Hibernate schema and the seed script of the {@code postgres} profile
 * against a real PostgreSQL started by Testcontainers. Requires Docker; run with
 * {@code ./mvnw verify -Pintegration-tests}.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("postgres")
@Testcontainers
@Import({EmployeeRepositoryAdapter.class, FeedbackRepositoryAdapter.class,
        EmployeePersistenceMapperImpl.class, FeedbackPersistenceMapperImpl.class})
class PersistenceAdaptersIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private EmployeeRepositoryAdapter employees;

    @Autowired
    private FeedbackRepositoryAdapter feedbacks;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void seedDataIsLoadedOnStartup() {
        assertThat(employees.findAll()).extracting(Employee::name).containsExactlyInAnyOrder("Jose", "Louisa");
        assertThat(feedbacks.findByEmployeeName("Jose"))
                .singleElement()
                .satisfies(feedback -> {
                    assertThat(feedback.reporter().name()).isEqualTo("Louisa");
                    assertThat(feedback.sentiment()).isNull();
                });
    }

    @Test
    void seedScriptIsIdempotent() {
        new ResourceDatabasePopulator(new ClassPathResource("import-postgres.sql")).execute(dataSource);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM employees", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM feedbacks", Integer.class)).isEqualTo(2);
    }

    @Test
    void employeeRoundTrip() {
        Employee saved = employees.save(new Employee(null, "Maria", "HR", "Recruiter", "maria@test.com", 52536.89, "Berlin"));

        assertThat(saved.id()).isNotNull();
        assertThat(employees.findByName("MARIA")).contains(saved);

        employees.deleteByName("Maria");
        assertThat(employees.findByName("Maria")).isEmpty();
    }

    @Test
    void feedbackIsStoredWithSentimentAndLinkedEmployees() {
        Employee jose = employees.findByName("Jose").orElseThrow();
        Employee louisa = employees.findByName("Louisa").orElseThrow();

        Feedback saved = feedbacks.save(new Feedback(null, louisa, jose, "Clear retrospectives", new Sentiment("positive", 0.91)));

        assertThat(saved.id()).isNotNull();
        List<Feedback> louisasFeedback = feedbacks.findByEmployeeName("Louisa");
        assertThat(louisasFeedback)
                .filteredOn(feedback -> feedback.id().equals(saved.id()))
                .singleElement()
                .satisfies(feedback -> {
                    assertThat(feedback.reporter().name()).isEqualTo("Jose");
                    assertThat(feedback.sentiment()).isEqualTo(new Sentiment("positive", 0.91));
                });
    }
}
