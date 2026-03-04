CREATE TABLE IF NOT EXISTS exercise (
    id VARCHAR(64) PRIMARY KEY,
    subject VARCHAR(20) NOT NULL,
    chapter VARCHAR(100) NOT NULL,
    chapter_slug VARCHAR(100),
    stem TEXT NOT NULL,
    option_a VARCHAR(255),
    option_b VARCHAR(255),
    option_c VARCHAR(255),
    option_d VARCHAR(255),
    answer VARCHAR(4) NOT NULL,
    analysis TEXT,
    difficulty INT DEFAULT 2,
    knowledge_points TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_exercise_subject(subject)
);

CREATE TABLE IF NOT EXISTS user_answer (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    subject VARCHAR(20) NOT NULL,
    exercise_id VARCHAR(64) NOT NULL,
    is_correct TINYINT NOT NULL,
    chosen_option VARCHAR(4),
    correct_answer VARCHAR(4),
    answered_at DATETIME,
    INDEX idx_answer_user_subject(user_id, subject),
    INDEX idx_answer_exercise(exercise_id)
);

CREATE TABLE IF NOT EXISTS knowledge_relation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    subject VARCHAR(20) NOT NULL,
    source_kp VARCHAR(100) NOT NULL,
    target_kp VARCHAR(100) NOT NULL,
    relation_type VARCHAR(32) DEFAULT 'related',
    weight DECIMAL(6,2) DEFAULT 1.00,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_kg_subject_source(subject, source_kp),
    INDEX idx_kg_subject_target(subject, target_kp)
);
