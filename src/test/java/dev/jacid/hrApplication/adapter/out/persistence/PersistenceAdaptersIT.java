package dev.jacid.hrApplication.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import dev.jacid.hrApplication.adapter.out.persistence.mappers.EmployeePersistenceMapperImpl;
import dev.jacid.hrApplication.adapter.out.persistence.mappers.FeedbackPersistenceMapperImpl;
import dev.jacid.hrApplication.domain.model.AppSettings;
import dev.jacid.hrApplication.domain.model.Employee;
import dev.jacid.hrApplication.domain.model.Feedback;
import dev.jacid.hrApplication.domain.model.FeedbackFilter;
import dev.jacid.hrApplication.domain.model.FeedbackValue;
import dev.jacid.hrApplication.domain.model.Sentiment;
import dev.jacid.hrApplication.domain.model.SentimentFilter;
import dev.jacid.hrApplication.domain.model.SentimentLabel;
import dev.jacid.hrApplication.infrastructure.seed.DemoDataSeeder;
import dev.jacid.hrApplication.infrastructure.time.SystemTimeProvider;

/**
 * Runs the Flyway migrations, the JPA adapters (Hibernate validates the entities against the migrated
 * schema) and the demo data seeder against a real PostgreSQL started by Testcontainers. Requires Docker;
 * run with {@code ./mvnw verify -Pintegration-tests}. Every test is rolled back.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("postgres")
@Testcontainers
@Import({EmployeeRepositoryAdapter.class, FeedbackRepositoryAdapter.class, SettingsRepositoryAdapter.class,
        EmployeePersistenceMapperImpl.class, FeedbackPersistenceMapperImpl.class,
        DemoDataSeeder.class, SystemTimeProvider.class})
class PersistenceAdaptersIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private EmployeeRepositoryAdapter employees;

    @Autowired
    private FeedbackRepositoryAdapter feedback;

    @Autowired
    private SettingsRepositoryAdapter settings;

    @Autowired
    private DemoDataSeeder seeder;

    @Autowired
    private JdbcTemplate jdbc;

    private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.SECONDS);

    private Employee newEmployee(String username) {
        return employees.save(new Employee(null, username, "Name of " + username, "Finance", "Analyst",
                username + "@example.com", 50000.0, "Mainz", NOW));
    }

    @Test
    void flywayCreatedTheSchema() {
        assertThat(jdbc.queryForObject("SELECT max(version) FROM flyway_schema_history WHERE success", String.class))
                .isEqualTo("1");
        assertThat(jdbc.queryForObject("SELECT data_type FROM information_schema.columns "
                + "WHERE table_name = 'employees' AND column_name = 'id'", String.class)).isEqualTo("uuid");
    }

    @Test
    void demoDataIsSeededOnStartupOnlyOnce() {
        assertThat(employees.count()).isEqualTo(12);
        assertThat(feedback.count()).isEqualTo(50);

        assertThat(seeder.seedIfEmpty()).isFalse();

        assertThat(employees.count()).isEqualTo(12);
        assertThat(feedback.count()).isEqualTo(50);
        assertThat(employees.findByUsername("jose")).get().extracting(Employee::name).isEqualTo("José Antonio Cid");
    }

    @Test
    void employeeRoundTrip() {
        Employee saved = newEmployee("nora");

        assertThat(saved.id()).isNotNull();
        assertThat(employees.findByUsername("NORA")).contains(saved);
        assertThat(employees.findById(saved.id())).contains(saved);

        Employee renamed = employees.save(new Employee(saved.id(), "nora", "Nora Weber", "People", "Recruiter",
                "nora@example.com", 51000.0, "Berlin", saved.createdAt()));
        assertThat(employees.findById(saved.id())).contains(renamed);

        employees.deleteById(saved.id());
        assertThat(employees.findById(saved.id())).isEmpty();
    }

    @Test
    void usernamesAreUnique() {
        newEmployee("nora");

        assertThatThrownBy(() -> {
            newEmployee("nora");
            employees.findAll(); // the query flushes the pending insert
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void feedbackRoundTripWithAllFields() {
        Employee jose = employees.findByUsername("jose").orElseThrow();
        Employee nora = newEmployee("nora");

        Feedback saved = feedback.save(new Feedback(null, nora, jose, true, FeedbackValue.GROWTH, "Clear retrospectives",
                new Sentiment(SentimentLabel.POSITIVE, 0.91), NOW));

        assertThat(saved.id()).isNotNull();
        assertThat(feedback.findByRecipientId(nora.id())).singleElement().satisfies(stored -> {
            assertThat(stored.author()).isEqualTo(jose);
            assertThat(stored.recipient()).isEqualTo(nora);
            assertThat(stored.anonymous()).isTrue();
            assertThat(stored.value()).isEqualTo(FeedbackValue.GROWTH);
            assertThat(stored.sentiment()).isEqualTo(new Sentiment(SentimentLabel.POSITIVE, 0.91));
            assertThat(stored.createdAt()).isEqualTo(NOW);
        });
        assertThat(feedback.findByAuthorId(jose.id()).get(0).id()).isEqualTo(saved.id()); // newest first
    }

    @Test
    void searchCombinesTheFiltersNewestFirst() {
        List<Feedback> sales = feedback.search(new FeedbackFilter(null, "SALES", null, null, null));
        assertThat(sales).isNotEmpty().allSatisfy(f -> assertThat(f.recipient().department()).isEqualTo("Sales"));
        assertThat(sales).extracting(Feedback::createdAt).isSortedAccordingTo((a, b) -> b.compareTo(a));

        assertThat(feedback.search(new FeedbackFilter(null, null, null, null, SentimentFilter.NONE)))
                .hasSize(3).allSatisfy(f -> assertThat(f.sentiment()).isNull());

        Employee maria = employees.findByUsername("maria").orElseThrow();
        List<Feedback> mariaNegativeLastMonth = feedback.search(new FeedbackFilter(maria.id(), null,
                NOW.minus(30, ChronoUnit.DAYS), NOW.plusSeconds(60), SentimentFilter.NEGATIVE));
        assertThat(mariaNegativeLastMonth).hasSize(2);

        assertThat(feedback.findCreatedSince(NOW.minus(30, ChronoUnit.DAYS))).hasSize(13);
    }

    @Test
    void deletingAnEmployeeDeletesFeedbackAboutThemAndKeepsTheirFeedbackWithoutAuthor() {
        Employee jose = employees.findByUsername("jose").orElseThrow();
        int written = feedback.findByAuthorId(jose.id()).size();
        int about = feedback.findByRecipientId(jose.id()).size();

        employees.deleteById(jose.id());

        assertThat(feedback.count()).isEqualTo(50 - about);
        assertThat(feedback.findByRecipientId(jose.id())).isEmpty();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM feedback WHERE author_id IS NULL", Integer.class))
                .isEqualTo(written);
    }

    @Test
    void settingsAreASingleRow() {
        assertThat(settings.load()).isEqualTo(new AppSettings(true));

        settings.save(new AppSettings(false));

        assertThat(settings.load()).isEqualTo(new AppSettings(false));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM app_settings", Integer.class)).isEqualTo(1);
    }
}
