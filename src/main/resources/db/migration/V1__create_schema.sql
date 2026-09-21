-- HRWebApp schema v2 (API contract v2). Portable SQL: runs on PostgreSQL and on H2.

CREATE TABLE employees (
    id          UUID                     NOT NULL,
    username    VARCHAR(64)              NOT NULL,  -- Keycloak username, stored in lower case
    name        VARCHAR(255)             NOT NULL,  -- display name, not unique
    department  VARCHAR(255)             NOT NULL,
    role        VARCHAR(255)             NOT NULL,
    email       VARCHAR(255)             NOT NULL,
    salary      DOUBLE PRECISION         NOT NULL,
    address     VARCHAR(255)             NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_employees PRIMARY KEY (id),
    CONSTRAINT uk_employees_username UNIQUE (username)
);

-- Deleting an employee deletes the feedback about them and keeps the feedback they wrote without author.
CREATE TABLE feedback (
    id               UUID                     NOT NULL,
    recipient_id     UUID                     NOT NULL,
    author_id        UUID,
    anonymous        BOOLEAN                  NOT NULL DEFAULT FALSE,
    core_value       VARCHAR(32),
    message          VARCHAR(500)             NOT NULL,
    sentiment_label  VARCHAR(16),
    sentiment_score  DOUBLE PRECISION,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_feedback PRIMARY KEY (id),
    CONSTRAINT fk_feedback_recipient FOREIGN KEY (recipient_id) REFERENCES employees (id) ON DELETE CASCADE,
    CONSTRAINT fk_feedback_author FOREIGN KEY (author_id) REFERENCES employees (id) ON DELETE SET NULL,
    CONSTRAINT ck_feedback_value CHECK (core_value IN ('TEAMWORK', 'OWNERSHIP', 'CRAFT', 'CUSTOMER_FOCUS', 'GROWTH')),
    CONSTRAINT ck_feedback_sentiment CHECK (sentiment_label IN ('POSITIVE', 'NEUTRAL', 'NEGATIVE'))
);

CREATE INDEX ix_feedback_recipient ON feedback (recipient_id, created_at);
CREATE INDEX ix_feedback_author ON feedback (author_id, created_at);
CREATE INDEX ix_feedback_created_at ON feedback (created_at);

-- Runtime settings changed by managers: exactly one row.
CREATE TABLE app_settings (
    id                          INTEGER NOT NULL,
    sentiment_analysis_enabled  BOOLEAN NOT NULL,
    CONSTRAINT pk_app_settings PRIMARY KEY (id),
    CONSTRAINT ck_app_settings_single_row CHECK (id = 1)
);

INSERT INTO app_settings (id, sentiment_analysis_enabled) VALUES (1, TRUE);
