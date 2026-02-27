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
    subject VARCHAR(200) NOT NULL,
    user_message TEXT NOT NULL,
    ai_response TEXT,
    confidence DOUBLE PRECISION NOT NULL,
    status VARCHAR(50) NOT NULL,
    category VARCHAR(50),
    sender_email VARCHAR(100) NOT NULL,
    operator_id BIGINT,
    operator_response TEXT,
    operator_notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    responded_at TIMESTAMP,
    FOREIGN KEY (operator_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_requests_status ON requests(status);
CREATE INDEX IF NOT EXISTS idx_requests_operator_id ON requests(operator_id);
CREATE INDEX IF NOT EXISTS idx_requests_created_at ON requests(created_at);

