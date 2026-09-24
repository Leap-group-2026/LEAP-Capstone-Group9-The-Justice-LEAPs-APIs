-- Schema for H2 Test Database

-- User Information Table
CREATE TABLE IF NOT EXISTS user_info (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    date_of_birth DATE,
    address VARCHAR(255),
    ssn_hash VARCHAR(255) UNIQUE,
    pass_hash VARCHAR(255) NOT NULL,
    code VARCHAR(255)
);

-- Admin Table
CREATE TABLE IF NOT EXISTS admin (
    admin_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    pass_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Instruments Table
CREATE TABLE IF NOT EXISTS instruments (
    instrument_id INT AUTO_INCREMENT PRIMARY KEY,
    ticker VARCHAR(20),
    asset_type VARCHAR(50),
    asset_name VARCHAR(255),
    price DECIMAL(15, 2),
    currency VARCHAR(10)
);

-- Accounts Table
CREATE TABLE IF NOT EXISTS accounts (
    account_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    balance DECIMAL(15, 2) DEFAULT 0.00,
    portfolio_size VARCHAR(50),
    trade_type VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Orders Table
CREATE TABLE IF NOT EXISTS orders (
    order_id INT AUTO_INCREMENT PRIMARY KEY,
    side VARCHAR(10) NOT NULL,
    account_id INT NOT NULL,
    instrument_id INT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    quantity INT NOT NULL,
    total_price DECIMAL(15, 2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Positions Table
CREATE TABLE IF NOT EXISTS positions (
    position_id INT AUTO_INCREMENT PRIMARY KEY,
    account_id INT NOT NULL,
    instrument_id INT NOT NULL,
    quantity INT NOT NULL,
    average_cost DECIMAL(15, 2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Transactions Table
CREATE TABLE IF NOT EXISTS transactions (
    transaction_id INT AUTO_INCREMENT PRIMARY KEY,
    amount DECIMAL(15, 2) NOT NULL,
    side VARCHAR(10),
    account_id INT NOT NULL,
    transaction_type VARCHAR(50),
    happened_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Historical Orders Table
CREATE TABLE IF NOT EXISTS historical_orders (
    historical_order_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT,
    account_id INT NOT NULL,
    order_information_json VARCHAR(2000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
