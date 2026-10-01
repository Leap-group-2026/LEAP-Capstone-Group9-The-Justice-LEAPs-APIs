CREATE TABLE IF NOT EXISTS user_info(
	user_id 		SERIAL PRIMARY KEY,
	name			TEXT NOT NULL,
	email			TEXT NOT NULL UNIQUE,
	date_of_birth	DATE NOT NULL,
	address			TEXT NOT NULL,
	ssn_hash		TEXT NOT NULL UNIQUE,
	pass_hash		TEXT NOT NULL,
	code			VARCHAR(6)
);

CREATE TABLE IF NOT EXISTS admin (
	admin_id		SERIAL PRIMARY KEY,
	email 			TEXT NOT NULL UNIQUE,
	pass_hash 		TEXT NOT NULL,
	created_at 		TIMESTAMP NOT NULL DEFAULT now(),
	role 			TEXT
);

CREATE TABLE IF NOT EXISTS accounts (
	account_id		SERIAL PRIMARY KEY,
	user_id 		INTEGER NOT NULL REFERENCES user_info(user_id),
	balance			NUMERIC(18, 4) NOT NULL DEFAULT 0,
	portfolio_size	TEXT NOT NULL CHECK(portfolio_size IN ('LOW', 'BALANCED', 'HIGH')),
	trade_type      TEXT NOT NULL,
	created_at		TIMESTAMP NOT NULL DEFAULT now(),
	account_active  BOOLEAN NOT NULL DEFAULT TRUE
);

-- Reference data only. Instrument existing here making it tradable
-- see current_prices for market price

CREATE TABLE IF NOT EXISTS instruments (
	instrument_id	SERIAL PRIMARY KEY,
    ticker			TEXT NOT NULL UNIQUE,
	asset_type		TEXT NOT NULL,
	asset_name		TEXT NOT NULL,
	currency 		TEXT NOT NULL DEFAULT 'USD'
);

CREATE TABLE IF NOT EXISTS current_prices (
	instrument_id	INTEGER PRIMARY KEY REFERENCES instruments(instrument_id),
	price			NUMERIC(18, 4) NOT NULL CHECK (price > 0),
	quote_time		TIMESTAMP WITH TIME ZONE NOT NULL,
	retrieved_at	TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS positions (
	position_id		SERIAL PRIMARY KEY,
	account_id		INTEGER NOT NULL REFERENCES accounts(account_id),
	quantity		INTEGER NOT NULL,
	instrument_id	INTEGER NOT NULL REFERENCES instruments(instrument_id),
	opened_at		TIMESTAMP NOT NULL DEFAULT NOW(),
	closed_at		TIMESTAMP,
	total_price		NUMERIC(18, 4) NOT NULL,
	average_price	NUMERIC(18, 4) NOT NULL
);


CREATE TABLE IF NOT EXISTS orders (
	order_id		SERIAL PRIMARY KEY,
	side			TEXT NOT NULL CHECK (side IN ('BUY', 'SELL')),
	account_id		INTEGER NOT NULL REFERENCES accounts(account_id),
	instrument_id	INTEGER NOT NULL REFERENCES instruments(instrument_id),
	status			TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN('PENDING', 'FILLED', 'DECLINED', 'FAILED', 'CANCELED')),
	quantity		INTEGER NOT NULL CHECK (quantity > 0),
	total_price		NUMERIC(18, 4) NOT NULL,
	created_at		TIMESTAMP NOT NULL DEFAULT now(),	
	updated_at		TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS transactions(
	transaction_id		SERIAL PRIMARY KEY,
	amount				NUMERIC(18, 4) NOT NULL,
	side				TEXT NOT NULL CHECK(side IN('OUT', 'IN')),
	account_id			INTEGER NOT NULL REFERENCES accounts(account_id),
	transaction_type	TEXT NOT NULL CHECK(transaction_type IN('TRADE', 'WITHDRAWAL', 'DEPOSIT')),
	happened_at			TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS historical_orders(
    historical_order_id     SERIAL PRIMARY KEY,
    order_id                INTEGER NOT NULL REFERENCES orders(order_id),
    account_id              INTEGER NOT NULL REFERENCES accounts(account_id),
    order_information_json  VARCHAR(4000) NOT NULL,
    created_at              TIMESTAMP NOT NULL DEFAULT now()
);
