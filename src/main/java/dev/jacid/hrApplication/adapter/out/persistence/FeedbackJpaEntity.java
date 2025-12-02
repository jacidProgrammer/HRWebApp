package dev.jacid.hrApplication.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "feedbacks")
@Getter
@Setter
public class FeedbackJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private EmployeeJpaEntity employee;

    @ManyToOne
    @JoinColumn(name = "reporter_id", nullable = false)
    private EmployeeJpaEntity reporter;
    
    @Column(nullable = false)
    private String message;

    @Column(nullable = true)
    private String label;

    @Column(nullable = true)
    private Double score;
}
