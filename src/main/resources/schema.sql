CREATE TABLE problem_ticket (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_no VARCHAR(64) NOT NULL UNIQUE,
    description VARCHAR(1024) NOT NULL,
    submit_time TIMESTAMP NOT NULL,
    status VARCHAR(32) NOT NULL,
    status_changed_at TIMESTAMP NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE problem_ticket_status_flow (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    from_status VARCHAR(32),
    to_status VARCHAR(32) NOT NULL,
    operate_time TIMESTAMP NOT NULL,
    operator_id BIGINT,
    source VARCHAR(32) NOT NULL,
    remark VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_problem_ticket_status_flow_ticket FOREIGN KEY (ticket_id) REFERENCES problem_ticket (id)
);

CREATE INDEX idx_problem_ticket_status_flow_ticket_time
    ON problem_ticket_status_flow (ticket_id, operate_time);

CREATE TABLE problem_ticket_metric (
    ticket_id BIGINT PRIMARY KEY,
    submit_time TIMESTAMP NOT NULL,
    current_status VARCHAR(32) NOT NULL,
    current_stage_entered_at TIMESTAMP NOT NULL,
    analysis_duration_sec BIGINT NOT NULL DEFAULT 0,
    analysis_completed BOOLEAN NOT NULL DEFAULT FALSE,
    modify_duration_sec BIGINT NOT NULL DEFAULT 0,
    modify_eligible BOOLEAN NOT NULL DEFAULT FALSE,
    push_duration_sec BIGINT NOT NULL DEFAULT 0,
    push_eligible BOOLEAN NOT NULL DEFAULT FALSE,
    closed_loop_duration_sec BIGINT,
    closed_loop_completed BOOLEAN NOT NULL DEFAULT FALSE,
    trace_complete BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_problem_ticket_metric_ticket FOREIGN KEY (ticket_id) REFERENCES problem_ticket (id)
);
