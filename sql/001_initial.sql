CREATE DATABASE IF NOT EXISTS workpilot CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE workpilot;

CREATE TABLE IF NOT EXISTS sys_department (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(80) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_department_name (name)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    login_name VARCHAR(64) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(80) NOT NULL,
    user_type VARCHAR(16) NOT NULL,
    department_id BIGINT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_user_login_name (login_name),
    CONSTRAINT fk_user_department FOREIGN KEY (department_id) REFERENCES sys_department (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS auth_token (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_hash CHAR(64) NOT NULL,
    expires_at DATETIME(3) NOT NULL,
    revoked_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_auth_token_hash (token_hash),
    KEY idx_auth_token_user (user_id),
    CONSTRAINT fk_auth_token_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS ticket_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(80) NOT NULL,
    responsible_department_id BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    UNIQUE KEY uk_ticket_category_name (name),
    CONSTRAINT fk_category_department FOREIGN KEY (responsible_department_id) REFERENCES sys_department (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS ticket (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    requester_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    title VARCHAR(160) NOT NULL,
    description TEXT NOT NULL,
    priority VARCHAR(16) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'NEW',
    assignee_id BIGINT NULL,
    due_at DATETIME(3) NOT NULL,
    resolved_at DATETIME(3) NULL,
    closed_at DATETIME(3) NULL,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    KEY idx_ticket_requester_created (requester_id, created_at, id),
    KEY idx_ticket_department_status (department_id, status, created_at),
    CONSTRAINT fk_ticket_requester FOREIGN KEY (requester_id) REFERENCES sys_user (id),
    CONSTRAINT fk_ticket_department FOREIGN KEY (department_id) REFERENCES sys_department (id),
    CONSTRAINT fk_ticket_category FOREIGN KEY (category_id) REFERENCES ticket_category (id),
    CONSTRAINT fk_ticket_assignee FOREIGN KEY (assignee_id) REFERENCES sys_user (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS operation_request (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    actor_id BIGINT NOT NULL,
    operation_type VARCHAR(40) NOT NULL,
    request_key VARCHAR(64) COLLATE utf8mb4_bin NOT NULL,
    request_hash CHAR(64) NOT NULL,
    resource_id BIGINT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_operation_request (actor_id, operation_type, request_key),
    CONSTRAINT fk_operation_actor FOREIGN KEY (actor_id) REFERENCES sys_user (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS ticket_action_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ticket_id BIGINT NOT NULL,
    operator_id BIGINT NOT NULL,
    action VARCHAR(32) NOT NULL,
    to_status VARCHAR(32) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    KEY idx_ticket_log_ticket (ticket_id, created_at),
    CONSTRAINT fk_ticket_log_ticket FOREIGN KEY (ticket_id) REFERENCES ticket (id),
    CONSTRAINT fk_ticket_log_operator FOREIGN KEY (operator_id) REFERENCES sys_user (id)
) ENGINE=InnoDB;

INSERT INTO sys_department (id, name, status)
VALUES (1, '客服组', 'ACTIVE')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO ticket_category (id, name, responsible_department_id, status)
VALUES (1, '账号问题', 1, 'ACTIVE'), (2, '使用问题', 1, 'ACTIVE')
ON DUPLICATE KEY UPDATE name = VALUES(name);
