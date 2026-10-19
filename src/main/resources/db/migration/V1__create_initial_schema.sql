CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(120) NOT NULL,
    email VARCHAR(160) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    avatar_url VARCHAR(500),
    provider VARCHAR(30) NOT NULL,
    provider_id VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE goals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(180) NOT NULL,
    description TEXT,
    category VARCHAR(100) NOT NULL,
    status VARCHAR(40) NOT NULL,
    priority VARCHAR(30) NOT NULL,
    deadline DATE,
    progress INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE goal_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    goal_id UUID NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(30) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_goal_members_goal_user UNIQUE (goal_id, user_id)
);

CREATE TABLE board_columns (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    goal_id UUID NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
    title VARCHAR(80) NOT NULL,
    color VARCHAR(20) NOT NULL,
    position INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    goal_id UUID NOT NULL REFERENCES goals(id) ON DELETE CASCADE,
    column_id UUID NOT NULL REFERENCES board_columns(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    priority VARCHAR(30) NOT NULL,
    deadline DATE,
    position INTEGER NOT NULL DEFAULT 0,
    attachments_count INTEGER NOT NULL DEFAULT 0,
    comments_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE task_assigned_members (
    task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (task_id, user_id)
);

CREATE TABLE activities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    author_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    goal_id UUID REFERENCES goals(id) ON DELETE CASCADE,
    task_id UUID REFERENCES tasks(id) ON DELETE SET NULL,
    action_type VARCHAR(40) NOT NULL,
    title VARCHAR(255) NOT NULL,
    from_column VARCHAR(80),
    to_column VARCHAR(80),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_goal_members_goal_id ON goal_members(goal_id);
CREATE INDEX idx_goal_members_user_id ON goal_members(user_id);
CREATE INDEX idx_board_columns_goal_id ON board_columns(goal_id);
CREATE INDEX idx_tasks_goal_id ON tasks(goal_id);
CREATE INDEX idx_tasks_column_id ON tasks(column_id);
CREATE INDEX idx_task_assigned_members_task_id ON task_assigned_members(task_id);
CREATE INDEX idx_task_assigned_members_user_id ON task_assigned_members(user_id);
CREATE INDEX idx_activities_author_id ON activities(author_id);
CREATE INDEX idx_activities_goal_id ON activities(goal_id);
CREATE INDEX idx_activities_created_at ON activities(created_at DESC);
