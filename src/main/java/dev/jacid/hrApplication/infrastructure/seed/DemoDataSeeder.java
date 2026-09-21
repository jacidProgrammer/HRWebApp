package dev.jacid.hrApplication.infrastructure.seed;

import static dev.jacid.hrApplication.domain.model.FeedbackValue.CRAFT;
import static dev.jacid.hrApplication.domain.model.FeedbackValue.CUSTOMER_FOCUS;
import static dev.jacid.hrApplication.domain.model.FeedbackValue.GROWTH;
import static dev.jacid.hrApplication.domain.model.FeedbackValue.OWNERSHIP;
import static dev.jacid.hrApplication.domain.model.FeedbackValue.TEAMWORK;
import static dev.jacid.hrApplication.domain.model.SentimentLabel.NEGATIVE;
import static dev.jacid.hrApplication.domain.model.SentimentLabel.NEUTRAL;
import static dev.jacid.hrApplication.domain.model.SentimentLabel.POSITIVE;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import dev.jacid.hrApplication.application.port.out.EmployeeRepository;
import dev.jacid.hrApplication.application.port.out.FeedbackRepository;
import dev.jacid.hrApplication.application.port.out.TimeProvider;
import dev.jacid.hrApplication.domain.model.Employee;
import dev.jacid.hrApplication.domain.model.Feedback;
import dev.jacid.hrApplication.domain.model.FeedbackValue;
import dev.jacid.hrApplication.domain.model.Sentiment;
import dev.jacid.hrApplication.domain.model.SentimentLabel;

/**
 * Loads demo employees and feedback on startup when the database has no employees yet
 * ({@code app.seed.demo-data=true}, off in the {@code prod} build). Dates are relative to the moment of
 * seeding, so the dashboard always shows the last six months. The feedback comes with a pre-set sentiment,
 * so the dashboard works without a Hugging Face token; maria's positive share drops from 0.8 to 0.4
 * between the two last 30-day windows, which raises an alert.
 * <p>
 * The usernames {@code jose}, {@code louisa}, {@code maria} and {@code lukas} have Keycloak accounts in
 * {@code realm-export/hr-realm.json}; the other employees exist only in this database.
 */
@Component
@ConditionalOnProperty(name = "app.seed.demo-data", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final EmployeeRepository employees;
    private final FeedbackRepository feedback;
    private final TimeProvider timeProvider;

    public DemoDataSeeder(EmployeeRepository employees, FeedbackRepository feedback, TimeProvider timeProvider) {
        this.employees = employees;
        this.feedback = feedback;
        this.timeProvider = timeProvider;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedIfEmpty();
    }

    /** @return whether demo data was inserted */
    @Transactional
    public boolean seedIfEmpty() {
        if (employees.count() > 0 || feedback.count() > 0) {
            return false;
        }
        Instant now = timeProvider.now();
        Map<String, Employee> byUsername = new HashMap<>();
        for (Employee employee : EMPLOYEES) {
            Employee saved = employees.save(new Employee(null, employee.username(), employee.name(), employee.department(),
                    employee.role(), employee.email(), employee.salary(), employee.address(),
                    now.minus(Duration.ofDays(400 + 37L * byUsername.size()))));
            byUsername.put(saved.username(), saved);
        }
        for (int i = 0; i < FEEDBACK.size(); i++) {
            DemoFeedback item = FEEDBACK.get(i);
            Instant createdAt = now.minus(Duration.ofDays(item.daysAgo())).minus(Duration.ofHours(i * 5L % 9)).minusSeconds(i * 97L % 3600);
            feedback.save(new Feedback(null, byUsername.get(item.recipient()), byUsername.get(item.author()),
                    item.anonymous(), item.value(), item.message(),
                    item.label() == null ? null : new Sentiment(item.label(), item.score()), createdAt));
        }
        log.info("Seeded {} demo employees and {} feedback items", EMPLOYEES.size(), FEEDBACK.size());
        return true;
    }

    private static Employee employee(String username, String name, String department, String role, double salary, String address) {
        return new Employee(null, username, name, department, role, username + "@example.com", salary, address, null);
    }

    private record DemoFeedback(int daysAgo, String author, String recipient, FeedbackValue value, boolean anonymous,
                                SentimentLabel label, double score, String message) {
    }

    private static DemoFeedback fb(int daysAgo, String author, String recipient, FeedbackValue value, boolean anonymous,
                                   SentimentLabel label, double score, String message) {
        return new DemoFeedback(daysAgo, author, recipient, value, anonymous, label, score, message);
    }

    static final List<Employee> EMPLOYEES = List.of(
            employee("jose", "José Antonio Cid", "IT", "Java Senior Backend", 75600, "Mainz, Germany"),
            employee("louisa", "Louisa Becker", "IT", "Senior Agile Coach", 79600, "Mainz, Germany"),
            employee("lukas", "Lukas Schneider", "IT", "Frontend Developer", 64000, "Wiesbaden, Germany"),
            employee("priya", "Priya Nair", "IT", "DevOps Engineer", 71000, "Frankfurt am Main, Germany"),
            employee("maria", "María García", "Sales", "Account Executive", 58000, "Frankfurt am Main, Germany"),
            employee("tobias", "Tobias Wagner", "Sales", "Head of Sales", 88000, "Mainz, Germany"),
            employee("elena", "Elena Rossi", "Sales", "Sales Development Representative", 46000, "Darmstadt, Germany"),
            employee("sophie", "Sophie Müller", "People", "HR Business Partner", 62000, "Mainz, Germany"),
            employee("david", "David Okafor", "People", "Talent Acquisition Specialist", 54000, "Wiesbaden, Germany"),
            employee("anna", "Anna Kowalski", "Finance", "Financial Controller", 68000, "Frankfurt am Main, Germany"),
            employee("jonas", "Jonas Fischer", "Finance", "Accountant", 52000, "Mainz, Germany"),
            employee("clara", "Clara Hoffmann", "Finance", "Payroll Specialist", 50000, "Mainz, Germany"));

    static final List<DemoFeedback> FEEDBACK = List.of(
            // last 30 days
            fb(1, "louisa", "jose", CRAFT, false, POSITIVE, 0.97, "The refactoring of the payroll export made the code much easier to follow. Thanks for pairing with me on it."),
            fb(2, "tobias", "maria", CUSTOMER_FOCUS, true, NEGATIVE, 0.81, "Client follow-ups slipped a few times this month and two customers had to chase us for answers."),
            fb(3, "jose", "louisa", TEAMWORK, false, POSITIVE, 0.98, "The retro format you tried this sprint finally got the quieter people talking. Great facilitation."),
            fb(5, "elena", "maria", TEAMWORK, false, POSITIVE, 0.91, "Thanks for taking over my demo at short notice, the client loved it."),
            fb(6, "lukas", "priya", CRAFT, false, POSITIVE, 0.95, "The new deployment pipeline cut our release time in half."),
            fb(8, "jonas", "maria", null, true, NEGATIVE, 0.77, "Handover notes for the Q3 renewals were incomplete, which caused extra work in Finance."),
            fb(10, "maria", "louisa", GROWTH, false, POSITIVE, 0.96, "Your coaching session on negotiation techniques helped me close a difficult deal."),
            fb(12, "anna", "maria", OWNERSHIP, false, NEUTRAL, 0.62, "The forecast numbers arrived on time, but some deals were missing context."),
            fb(14, "sophie", "david", TEAMWORK, false, POSITIVE, 0.93, "Onboarding for the new joiners ran smoothly thanks to your checklist."),
            fb(17, "tobias", "maria", CUSTOMER_FOCUS, false, POSITIVE, 0.88, "Good recovery with the Hansen account: you turned a complaint into an upsell."),
            fb(20, "priya", "louisa", TEAMWORK, false, POSITIVE, 0.94, "Thanks for mediating the discussion between the platform and product teams."),
            fb(24, "jose", "lukas", CRAFT, false, null, 0, "Nice accessibility improvements in the employee directory."),
            fb(27, "lukas", "louisa", GROWTH, false, POSITIVE, 0.97, "The mentoring sessions are really helping me grow into a tech lead role."),
            // 30 to 60 days ago
            fb(32, "tobias", "maria", OWNERSHIP, false, POSITIVE, 0.95, "You owned the trade fair preparation end to end. Excellent work."),
            fb(35, "louisa", "lukas", TEAMWORK, false, POSITIVE, 0.90, "Great collaboration with the designers on the new feedback screen."),
            fb(38, "elena", "maria", GROWTH, false, POSITIVE, 0.92, "Thanks for showing me how you structure discovery calls."),
            fb(41, "clara", "anna", OWNERSHIP, false, POSITIVE, 0.90, "The month-end close went smoothly because you anticipated the blockers."),
            fb(44, "david", "maria", CUSTOMER_FOCUS, false, POSITIVE, 0.96, "Your talk about customer needs at the all-hands was clear and inspiring."),
            fb(47, "jose", "louisa", TEAMWORK, false, POSITIVE, 0.95, "The team agreement workshop gave us clear rules for code reviews."),
            fb(50, "anna", "maria", null, false, NEUTRAL, 0.58, "Expense reports are complete now, although a few came in late."),
            fb(53, "sophie", "louisa", GROWTH, false, POSITIVE, 0.93, "Thanks for co-hosting the feedback culture training."),
            fb(56, "tobias", "elena", GROWTH, false, POSITIVE, 0.89, "Your pipeline doubled this quarter. Keep it up!"),
            fb(57, "jose", "maria", CUSTOMER_FOCUS, false, POSITIVE, 0.90, "Thanks for the clear requirements from the customer workshop."),
            fb(58, "maria", "tobias", TEAMWORK, true, NEUTRAL, 0.55, "Team meetings could be shorter and more focused on decisions."),
            // 60 to 90 days ago
            fb(62, "jose", "priya", OWNERSHIP, false, POSITIVE, 0.94, "Thanks for staying on call during the database migration weekend."),
            fb(64, "maria", "louisa", TEAMWORK, false, POSITIVE, 0.92, "Your workshop helped Sales and IT agree on a shared roadmap."),
            fb(67, "clara", "jonas", CRAFT, false, POSITIVE, 0.87, "The reconciliation spreadsheet you built saves me hours every week."),
            fb(70, "tobias", "louisa", null, true, NEUTRAL, 0.60, "Sprint reviews are useful, but sometimes run over time."),
            fb(73, "elena", "david", GROWTH, false, POSITIVE, 0.90, "The interview training was practical and well organised."),
            fb(76, "priya", "jose", OWNERSHIP, false, POSITIVE, 0.96, "You tracked down the memory leak that had been bothering us for weeks."),
            fb(80, "anna", "louisa", GROWTH, false, POSITIVE, 0.91, "Thanks for introducing OKRs to the Finance team in such a practical way."),
            fb(84, "lukas", "jose", TEAMWORK, true, NEGATIVE, 0.70, "Code review comments sometimes feel too blunt; a bit more context would help."),
            fb(88, "sophie", "clara", CUSTOMER_FOCUS, false, POSITIVE, 0.90, "Employees keep mentioning how quickly you answer payroll questions."),
            // 90 to 180 days ago
            fb(93, "jose", "louisa", TEAMWORK, false, POSITIVE, 0.95, "Thanks for protecting the team's focus time during the release crunch."),
            fb(97, "maria", "elena", CUSTOMER_FOCUS, false, POSITIVE, 0.90, "Great first quarter! Customers already ask for you by name."),
            fb(101, "david", "sophie", OWNERSHIP, false, POSITIVE, 0.92, "You handled the works council consultation with great care."),
            fb(105, "jonas", "anna", CRAFT, false, null, 0, "The new budget template is much clearer."),
            fb(109, "louisa", "jose", GROWTH, false, POSITIVE, 0.93, "Great talk at the Java user group, the team learned a lot."),
            fb(114, "tobias", "maria", CUSTOMER_FOCUS, false, POSITIVE, 0.94, "Record renewals this quarter, well done."),
            fb(118, "priya", "lukas", CRAFT, false, NEUTRAL, 0.57, "The component library is promising, but the documentation is still thin."),
            fb(123, "elena", "tobias", GROWTH, true, NEGATIVE, 0.74, "It is hard to get time for one-to-ones; I would appreciate more regular check-ins."),
            fb(128, "anna", "jonas", OWNERSHIP, false, POSITIVE, 0.88, "You took over the audit preparation without being asked. Thank you."),
            fb(133, "louisa", "sophie", TEAMWORK, false, POSITIVE, 0.93, "The new onboarding buddy programme is a big improvement."),
            fb(138, "jose", "priya", CRAFT, true, POSITIVE, 0.91, "The monitoring dashboards made the last incident easy to diagnose."),
            fb(143, "clara", "sophie", null, false, NEUTRAL, 0.52, "The new leave policy is clearer, although the announcement came late."),
            fb(149, "lukas", "louisa", TEAMWORK, false, POSITIVE, 0.95, "Thanks for making our planning meetings shorter and more useful."),
            fb(155, "maria", "jose", CUSTOMER_FOCUS, false, POSITIVE, 0.90, "The CRM integration you built saves the Sales team a lot of copy-pasting."),
            fb(161, "david", "elena", TEAMWORK, false, POSITIVE, 0.87, "Thanks for joining the campus recruiting day and talking about Sales."),
            fb(167, "jonas", "clara", null, false, null, 0, "Thanks for covering the payroll run while I was on holiday."),
            fb(172, "tobias", "david", OWNERSHIP, false, POSITIVE, 0.90, "You filled the Sales openings faster than planned."));
}
