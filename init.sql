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
    name VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL
);

-- Create expenses table if it doesn't exist
CREATE TABLE IF NOT EXISTS expenses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    description TEXT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    category VARCHAR(100) NOT NULL,
    date VARCHAR(255) NOT NULL
);

-- Insert sample data (optional)
INSERT IGNORE INTO users (id, email, name, password) VALUES 
(1, 'demo@example.com', 'Demo User', '$2a$10$example_hashed_password');

INSERT IGNORE INTO expenses (description, amount, category, date) VALUES 
('Weekly grocery shopping', 150.00, 'Food', '2025-09-12'),
('Fuel for car', 45.00, 'Transport', '2025-09-12'),
('Monthly subscription', 15.99, 'Entertainment', '2025-09-12');