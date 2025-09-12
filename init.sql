-- Database initialization script for Expense Tracker
-- This script will run when the MySQL container starts for the first time

-- Create the database if it doesn't exist
CREATE DATABASE IF NOT EXISTS expenseDB;
USE expenseDB;

-- Create a user with proper permissions for MySQL 8
-- Note: Root user should already have access, but let's ensure permissions
GRANT ALL PRIVILEGES ON expenseDB.* TO 'root'@'%';
FLUSH PRIVILEGES;

-- Create users table if it doesn't exist
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Create expenses table if it doesn't exist
CREATE TABLE IF NOT EXISTS expenses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    amount DECIMAL(10, 2) NOT NULL,
    category VARCHAR(100) NOT NULL,
    date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Insert sample data (optional)
INSERT IGNORE INTO users (id, email, first_name, last_name, password) VALUES 
(1, 'demo@example.com', 'Demo', 'User', '$2a$10$example_hashed_password');

INSERT IGNORE INTO expenses (user_id, title, description, amount, category, date) VALUES 
(1, 'Groceries', 'Weekly grocery shopping', 150.00, 'Food', CURDATE()),
(1, 'Gas', 'Fuel for car', 45.00, 'Transport', CURDATE()),
(1, 'Netflix', 'Monthly subscription', 15.99, 'Entertainment', CURDATE());