-- TalentTrack AI initial schema (MySQL 8, also runs on H2 in MySQL mode for tests)

CREATE TABLE users (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name     VARCHAR(120) NOT NULL,
    email         VARCHAR(180) NOT NULL,
    password      VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    company_name  VARCHAR(150),
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE refresh_tokens (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    token       VARCHAR(120) NOT NULL,
    user_id     BIGINT       NOT NULL,
    expires_at  DATETIME(6)  NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  DATETIME(6)  NOT NULL,
    CONSTRAINT uk_refresh_token UNIQUE (token),
    CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE candidate_profiles (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT        NOT NULL,
    headline            VARCHAR(200),
    bio                 TEXT,
    phone               VARCHAR(30),
    location            VARCHAR(120),
    experience_years    INT           NOT NULL DEFAULT 0,
    education           VARCHAR(500),
    skills              VARCHAR(2000),
    linkedin_url        VARCHAR(255),
    github_url          VARCHAR(255),
    portfolio_url       VARCHAR(255),
    resume_file_name    VARCHAR(255),
    resume_stored_name  VARCHAR(255),
    resume_text         LONGTEXT,
    resume_uploaded_at  DATETIME(6),
    updated_at          DATETIME(6)   NOT NULL,
    CONSTRAINT uk_profile_user UNIQUE (user_id),
    CONSTRAINT fk_profile_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE jobs (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    recruiter_id    BIGINT        NOT NULL,
    title           VARCHAR(150)  NOT NULL,
    company_name    VARCHAR(150)  NOT NULL,
    location        VARCHAR(120)  NOT NULL,
    job_type        VARCHAR(20)   NOT NULL,
    work_mode       VARCHAR(20)   NOT NULL,
    min_experience  INT           NOT NULL DEFAULT 0,
    salary_min      INT,
    salary_max      INT,
    description     TEXT          NOT NULL,
    skills          VARCHAR(1000) NOT NULL,
    openings        INT           NOT NULL DEFAULT 1,
    status          VARCHAR(20)   NOT NULL,
    deadline        DATE,
    created_at      DATETIME(6)   NOT NULL,
    updated_at      DATETIME(6)   NOT NULL,
    CONSTRAINT fk_job_recruiter FOREIGN KEY (recruiter_id) REFERENCES users (id) ON DELETE CASCADE
);
CREATE INDEX idx_jobs_status ON jobs (status);
CREATE INDEX idx_jobs_recruiter ON jobs (recruiter_id);

CREATE TABLE applications (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id            BIGINT        NOT NULL,
    candidate_id      BIGINT        NOT NULL,
    stage             VARCHAR(20)   NOT NULL,
    cover_letter      TEXT,
    ats_score         INT           NOT NULL DEFAULT 0,
    skill_score       INT           NOT NULL DEFAULT 0,
    semantic_score    INT           NOT NULL DEFAULT 0,
    experience_score  INT           NOT NULL DEFAULT 0,
    matched_skills    VARCHAR(1000),
    missing_skills    VARCHAR(1000),
    ai_summary        TEXT,
    recruiter_rating  INT,
    board_position    INT           NOT NULL DEFAULT 0,
    applied_at        DATETIME(6)   NOT NULL,
    updated_at        DATETIME(6)   NOT NULL,
    CONSTRAINT uk_application_job_candidate UNIQUE (job_id, candidate_id),
    CONSTRAINT fk_application_job FOREIGN KEY (job_id) REFERENCES jobs (id) ON DELETE CASCADE,
    CONSTRAINT fk_application_candidate FOREIGN KEY (candidate_id) REFERENCES users (id) ON DELETE CASCADE
);
CREATE INDEX idx_applications_stage ON applications (stage);

CREATE TABLE application_activities (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id  BIGINT       NOT NULL,
    actor_id        BIGINT,
    type            VARCHAR(30)  NOT NULL,
    from_stage      VARCHAR(20),
    to_stage        VARCHAR(20),
    message         TEXT,
    created_at      DATETIME(6)  NOT NULL,
    CONSTRAINT fk_activity_application FOREIGN KEY (application_id) REFERENCES applications (id) ON DELETE CASCADE,
    CONSTRAINT fk_activity_actor FOREIGN KEY (actor_id) REFERENCES users (id) ON DELETE SET NULL
);

CREATE TABLE interviews (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id  BIGINT       NOT NULL,
    scheduled_at    DATETIME(6)  NOT NULL,
    duration_min    INT          NOT NULL DEFAULT 30,
    mode            VARCHAR(20)  NOT NULL,
    meeting_link    VARCHAR(500),
    notes           TEXT,
    created_at      DATETIME(6)  NOT NULL,
    CONSTRAINT fk_interview_application FOREIGN KEY (application_id) REFERENCES applications (id) ON DELETE CASCADE
);

CREATE TABLE notifications (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT        NOT NULL,
    title       VARCHAR(200)  NOT NULL,
    message     VARCHAR(1000) NOT NULL,
    link        VARCHAR(255),
    is_read     BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at  DATETIME(6)   NOT NULL,
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
CREATE INDEX idx_notifications_user ON notifications (user_id, is_read);
