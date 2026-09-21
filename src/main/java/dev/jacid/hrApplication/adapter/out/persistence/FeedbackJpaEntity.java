package dev.jacid.hrApplication.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import dev.jacid.hrApplication.domain.model.FeedbackValue;
import dev.jacid.hrApplication.domain.model.SentimentLabel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Table {@code feedback}, created by the Flyway migrations in {@code db/migration}. */
@Entity
@Table(name = "feedback")
@Getter
@Setter
public class FeedbackJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private EmployeeJpaEntity recipient;

    /** {@code null} once the author's employee record has been deleted. */
    @ManyToOne
    @JoinColumn(name = "author_id")
    private EmployeeJpaEntity author;

    @Column(nullable = false)
    private boolean anonymous;

    /** "value" is a reserved word in H2, hence the column name. */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "core_value", length = 32)
    private FeedbackValue value;

    @Column(nullable = false, length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "sentiment_label", length = 16)
    private SentimentLabel sentimentLabel;

    @Column(name = "sentiment_score")
    private Double sentimentScore;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
