package dev.jacid.hrApplication.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Single-row table {@code app_settings} (id is always {@value #SINGLETON_ID}). */
@Entity
@Table(name = "app_settings")
@Getter
@Setter
public class AppSettingsJpaEntity {
    public static final int SINGLETON_ID = 1;

    @Id
    private Integer id;

    @Column(name = "sentiment_analysis_enabled", nullable = false)
    private boolean sentimentAnalysisEnabled;
}
