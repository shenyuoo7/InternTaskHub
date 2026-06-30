DROP TABLE IF EXISTS tasks;
DROP TABLE IF EXISTS news_items;
DROP TABLE IF EXISTS app_users;

CREATE TABLE app_users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(60) NOT NULL UNIQUE,
    display_name VARCHAR(80) NOT NULL,
    password VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    avatar_color VARCHAR(120)
);

CREATE TABLE tasks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(120) NOT NULL,
    description VARCHAR(1200),
    status VARCHAR(30) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    assignee_id BIGINT NOT NULL,
    creator_id BIGINT NOT NULL,
    due_date DATE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_tasks_assignee FOREIGN KEY (assignee_id) REFERENCES app_users (id),
    CONSTRAINT fk_tasks_creator FOREIGN KEY (creator_id) REFERENCES app_users (id),
    INDEX idx_tasks_status (status),
    INDEX idx_tasks_assignee (assignee_id),
    INDEX idx_tasks_due_date (due_date)
);

CREATE TABLE news_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(240) NOT NULL,
    summary VARCHAR(1200),
    link VARCHAR(600) NOT NULL UNIQUE,
    source VARCHAR(80) NOT NULL,
    keyword VARCHAR(120),
    published_at DATETIME,
    fetched_at DATETIME NOT NULL,
    INDEX idx_news_keyword (keyword),
    INDEX idx_news_published_at (published_at)
);
