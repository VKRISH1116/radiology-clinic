-- ============================================================
-- V4 — Refresh tokens
-- Backs a revocable refresh-token flow. We store only a SHA-256 HASH of each
-- token (like a password), so a DB leak can't be replayed. Rows are revoked on
-- rotation and on logout — which is how "logout" works at all with otherwise
-- stateless JWT access tokens.
-- ============================================================
CREATE TABLE refresh_tokens (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users(id),
    token_hash TEXT NOT NULL UNIQUE,                 -- SHA-256 hex of the opaque token
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
