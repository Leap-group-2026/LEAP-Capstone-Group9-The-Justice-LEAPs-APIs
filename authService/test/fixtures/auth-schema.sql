-- The tables the auth service touches, copied from the shared database's schema.
-- Keep in step with it: a column change there must be made here too, or these tests prove nothing.

CREATE TABLE user_info(
	user_id 		SERIAL PRIMARY KEY,
	name			TEXT NOT NULL,
	email			TEXT NOT NULL UNIQUE,
	date_of_birth	DATE NOT NULL,
	address			TEXT NOT NULL,
	ssn_hash		TEXT NOT NULL UNIQUE,
	pass_hash		TEXT NOT NULL,
	code			VARCHAR(6)
);

CREATE TABLE admin(
	admin_id		SERIAL PRIMARY KEY,
	email 			TEXT NOT NULL UNIQUE,
	pass_hash 		TEXT NOT NULL,
	role			TEXT NOT NULL,
	created_at 		TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE refresh_tokens (
	token_hash		TEXT PRIMARY KEY CHECK (char_length(token_hash) = 64),
	session_id		UUID NOT NULL,
	user_id			INTEGER REFERENCES user_info(user_id) ON DELETE CASCADE,
	admin_id		INTEGER REFERENCES admin(admin_id) ON DELETE CASCADE,
	auth_time		TIMESTAMPTZ NOT NULL,
	expires_at		TIMESTAMPTZ NOT NULL,
	revoked_at		TIMESTAMPTZ,
	created_at		TIMESTAMPTZ NOT NULL DEFAULT now(),
	CHECK ((user_id IS NULL) <> (admin_id IS NULL))
);

CREATE INDEX idx_refresh_tokens_session_id ON refresh_tokens(session_id);
