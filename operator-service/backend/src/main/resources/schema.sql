-- Create tables
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(50) NOT NULL,
    first_login BOOLEAN DEFAULT TRUE,
    active BOOLEAN DEFAULT TRUE,
    super_admin BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS requests (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(200) NOT NULL,
    organization VARCHAR(255),
    fio VARCHAR(255),
    phone VARCHAR(50),
    device_type VARCHAR(255),
    serial_number VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    category VARCHAR(50),
    project VARCHAR(255),
    inn VARCHAR(20),
    country_region VARCHAR(255),
    file_data BYTEA,
    confidence_score REAL NOT NULL DEFAULT 0,
    ai_generated_answer TEXT,
    operator_id BIGINT,
    operator_answer TEXT,
    operator_notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    responded_at TIMESTAMPTZ,
    FOREIGN KEY (operator_id) REFERENCES users(id) ON DELETE SET NULL
);

ALTER TABLE requests ADD COLUMN IF NOT EXISTS email VARCHAR(200);
ALTER TABLE requests ADD COLUMN IF NOT EXISTS organization VARCHAR(255);
ALTER TABLE requests ADD COLUMN IF NOT EXISTS fio VARCHAR(255);
ALTER TABLE requests ADD COLUMN IF NOT EXISTS phone VARCHAR(50);
ALTER TABLE requests ADD COLUMN IF NOT EXISTS device_type VARCHAR(255);
ALTER TABLE requests ADD COLUMN IF NOT EXISTS serial_number VARCHAR(255);
ALTER TABLE requests ADD COLUMN IF NOT EXISTS project VARCHAR(255);
ALTER TABLE requests ADD COLUMN IF NOT EXISTS inn VARCHAR(20);
ALTER TABLE requests ADD COLUMN IF NOT EXISTS country_region VARCHAR(255);
ALTER TABLE requests ADD COLUMN IF NOT EXISTS file_data BYTEA;
ALTER TABLE requests ADD COLUMN IF NOT EXISTS confidence_score REAL;
ALTER TABLE requests ADD COLUMN IF NOT EXISTS ai_generated_answer TEXT;
ALTER TABLE requests ADD COLUMN IF NOT EXISTS operator_answer TEXT;

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_requests_status ON requests(status);
CREATE INDEX IF NOT EXISTS idx_requests_operator_id ON requests(operator_id);
CREATE INDEX IF NOT EXISTS idx_requests_created_at ON requests(created_at);

