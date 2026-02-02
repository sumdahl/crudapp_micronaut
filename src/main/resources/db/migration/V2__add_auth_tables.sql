-- Migration: Add authentication support
-- Rename password column to password_hash and add roles column to users table
-- Create refresh_tokens table for JWT refresh token management

-- Step 1: Alter users table
ALTER TABLE users 
    RENAME COLUMN password TO password_hash;

ALTER TABLE users 
    ADD COLUMN roles VARCHAR(255) DEFAULT 'USER';

-- Step 2: Create refresh_tokens table
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create index on user_id for faster lookups
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);

-- Create index on token_hash for faster validation
CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens(token_hash);

-- Create index on expires_at for cleanup queries
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);
