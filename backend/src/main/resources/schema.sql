CREATE DATABASE IF NOT EXISTS trading_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE trading_db;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role ENUM('BUYER','SELLER','ADMIN') NOT NULL DEFAULT 'BUYER',
    status ENUM('ACTIVE','DISABLED') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    status ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE'
);

CREATE TABLE IF NOT EXISTS products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    status ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    image_url VARCHAR(500),
    seller_id BIGINT NOT NULL,
    category_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (seller_id) REFERENCES users(id),
    FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE TABLE IF NOT EXISTS cart_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    buyer_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_buyer_product (buyer_id, product_id),
    FOREIGN KEY (buyer_id) REFERENCES users(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE IF NOT EXISTS buyer_addresses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    buyer_id BIGINT NOT NULL,
    receiver_name VARCHAR(50) NOT NULL,
    receiver_phone VARCHAR(30) NOT NULL,
    province VARCHAR(50) NOT NULL,
    city VARCHAR(50) NOT NULL,
    district VARCHAR(50) NOT NULL,
    detail_address VARCHAR(300) NOT NULL,
    full_address VARCHAR(500) NOT NULL,
    longitude DECIMAL(10,7),
    latitude DECIMAL(10,7),
    formatted_address VARCHAR(500),
    validation_status VARCHAR(20) NOT NULL DEFAULT 'UNVERIFIED',
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_buyer_addresses_buyer_default (buyer_id, is_default),
    INDEX idx_buyer_addresses_buyer_updated (buyer_id, updated_at),
    FOREIGN KEY (buyer_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS coupons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    threshold_amount DECIMAL(12,2) NOT NULL,
    discount_amount DECIMAL(12,2) NOT NULL,
    total_quantity INT NOT NULL,
    claimed_quantity INT NOT NULL DEFAULT 0,
    per_user_limit INT NOT NULL,
    valid_from TIMESTAMP NOT NULL,
    valid_to TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    audience VARCHAR(20) NOT NULL DEFAULT 'PUBLIC',
    stackable BOOLEAN NOT NULL DEFAULT FALSE,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_coupons_status_valid (status, valid_from, valid_to),
    INDEX idx_coupons_created_at (created_at),
    FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS buyer_coupons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    buyer_id BIGINT NOT NULL,
    coupon_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'UNUSED',
    claimed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    used_at TIMESTAMP NULL,
    used_order_id BIGINT,
    INDEX idx_buyer_coupons_buyer_status (buyer_id, status),
    INDEX idx_buyer_coupons_coupon_buyer (coupon_id, buyer_id),
    INDEX idx_buyer_coupons_used_order (used_order_id),
    FOREIGN KEY (buyer_id) REFERENCES users(id),
    FOREIGN KEY (coupon_id) REFERENCES coupons(id)
);

CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    buyer_id BIGINT NOT NULL,
    status ENUM('PENDING_PAYMENT','PAID','SHIPPED','COMPLETED','CANCELLED','REFUND_REQUESTED','REFUNDED','REFUND_REJECTED') NOT NULL DEFAULT 'PENDING_PAYMENT',
    total_amount DECIMAL(12,2) NOT NULL,
    original_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    coupon_id BIGINT,
    buyer_coupon_id BIGINT,
    coupon_name VARCHAR(100),
    coupon_threshold_amount DECIMAL(12,2),
    coupon_discount_amount DECIMAL(12,2),
    membership_plan_id BIGINT,
    membership_plan_name VARCHAR(100),
    membership_discount_rate DECIMAL(5,4),
    membership_discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    address VARCHAR(500) NOT NULL,
    receiver_name VARCHAR(50),
    receiver_phone VARCHAR(30),
    receiver_province VARCHAR(50),
    receiver_city VARCHAR(50),
    receiver_district VARCHAR(50),
    receiver_detail_address VARCHAR(300),
    receiver_full_address VARCHAR(500),
    receiver_longitude DECIMAL(10,7),
    receiver_latitude DECIMAL(10,7),
    receiver_address_validation_status VARCHAR(20) NOT NULL DEFAULT 'UNVERIFIED',
    tracking_number VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (buyer_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    shop_name VARCHAR(100),
    price DECIMAL(10,2) NOT NULL,
    quantity INT NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE IF NOT EXISTS order_coupon_usages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    coupon_id BIGINT NOT NULL,
    buyer_coupon_id BIGINT NOT NULL,
    coupon_name VARCHAR(100) NOT NULL,
    audience VARCHAR(20) NOT NULL,
    stackable BOOLEAN NOT NULL DEFAULT FALSE,
    threshold_amount DECIMAL(12,2) NOT NULL,
    coupon_discount_amount DECIMAL(12,2) NOT NULL,
    applied_discount_amount DECIMAL(12,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_order_coupon_usages_order (order_id),
    INDEX idx_order_coupon_usages_buyer_coupon (buyer_coupon_id),
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (coupon_id) REFERENCES coupons(id),
    FOREIGN KEY (buyer_coupon_id) REFERENCES buyer_coupons(id)
);

CREATE TABLE IF NOT EXISTS membership_plans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    price DECIMAL(12,2) NOT NULL,
    duration_months INT NOT NULL,
    discount_rate DECIMAL(5,4) NOT NULL,
    monthly_coupon_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_membership_plans_status (status),
    INDEX idx_membership_plans_created_at (created_at),
    FOREIGN KEY (monthly_coupon_id) REFERENCES coupons(id)
);

CREATE TABLE IF NOT EXISTS buyer_memberships (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    buyer_id BIGINT NOT NULL UNIQUE,
    plan_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    started_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    last_paid_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_buyer_memberships_buyer (buyer_id),
    INDEX idx_buyer_memberships_status_expires (status, expires_at),
    FOREIGN KEY (buyer_id) REFERENCES users(id),
    FOREIGN KEY (plan_id) REFERENCES membership_plans(id)
);

CREATE TABLE IF NOT EXISTS membership_purchases (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    buyer_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING_PAYMENT',
    out_trade_no VARCHAR(64) UNIQUE,
    alipay_trade_no VARCHAR(64),
    paid_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_membership_purchases_buyer_created (buyer_id, created_at),
    INDEX idx_membership_purchases_out_trade_no (out_trade_no),
    FOREIGN KEY (buyer_id) REFERENCES users(id),
    FOREIGN KEY (plan_id) REFERENCES membership_plans(id)
);

CREATE TABLE IF NOT EXISTS membership_monthly_benefits (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    buyer_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    benefit_month VARCHAR(7) NOT NULL,
    coupon_id BIGINT NOT NULL,
    buyer_coupon_id BIGINT NOT NULL,
    claimed_at TIMESTAMP NOT NULL,
    UNIQUE KEY uk_membership_monthly_benefit (buyer_id, plan_id, benefit_month),
    INDEX idx_membership_monthly_benefits_buyer (buyer_id, benefit_month),
    FOREIGN KEY (buyer_id) REFERENCES users(id),
    FOREIGN KEY (plan_id) REFERENCES membership_plans(id),
    FOREIGN KEY (coupon_id) REFERENCES coupons(id),
    FOREIGN KEY (buyer_coupon_id) REFERENCES buyer_coupons(id)
);

CREATE TABLE IF NOT EXISTS shops (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    seller_id BIGINT NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    province VARCHAR(50),
    city VARCHAR(50),
    district VARCHAR(50),
    detail_address VARCHAR(300),
    full_address VARCHAR(500),
    longitude DECIMAL(10,7),
    latitude DECIMAL(10,7),
    address_validation_status VARCHAR(20) NOT NULL DEFAULT 'UNVERIFIED',
    status ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (seller_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS shop_favorites (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    buyer_id BIGINT NOT NULL,
    shop_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_shop_favorites_buyer_shop (buyer_id, shop_id),
    INDEX idx_shop_favorites_buyer_created (buyer_id, created_at),
    INDEX idx_shop_favorites_shop (shop_id),
    FOREIGN KEY (buyer_id) REFERENCES users(id),
    FOREIGN KEY (shop_id) REFERENCES shops(id)
);

CREATE TABLE IF NOT EXISTS price_alerts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    buyer_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    target_price DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_notified_price DECIMAL(10,2),
    triggered_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_price_alerts_buyer_product (buyer_id, product_id),
    INDEX idx_price_alerts_buyer_updated (buyer_id, updated_at),
    INDEX idx_price_alerts_product_status_target (product_id, status, target_price),
    FOREIGN KEY (buyer_id) REFERENCES users(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE IF NOT EXISTS buyer_notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    buyer_id BIGINT NOT NULL,
    type VARCHAR(30) NOT NULL,
    title VARCHAR(120) NOT NULL,
    content VARCHAR(500) NOT NULL,
    product_id BIGINT,
    shop_id BIGINT,
    read_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_buyer_notifications_buyer_created (buyer_id, created_at),
    INDEX idx_buyer_notifications_buyer_read (buyer_id, read_at),
    FOREIGN KEY (buyer_id) REFERENCES users(id),
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (shop_id) REFERENCES shops(id)
);

CREATE TABLE IF NOT EXISTS operation_logs (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT,
    username    VARCHAR(50),
    module      VARCHAR(50),
    action      VARCHAR(100),
    resource_id VARCHAR(50),
    detail      VARCHAR(500),
    ip          VARCHAR(50),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS shipments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE,
    carrier_code VARCHAR(50) NOT NULL,
    carrier_name VARCHAR(100) NOT NULL,
    tracking_number VARCHAR(100) NOT NULL,
    status ENUM('SHIPPED','IN_TRANSIT','OUT_FOR_DELIVERY','DELIVERED','EXCEPTION') NOT NULL DEFAULT 'SHIPPED',
    origin_name VARCHAR(100),
    origin_province VARCHAR(50),
    origin_city VARCHAR(50),
    origin_district VARCHAR(50),
    origin_detail_address VARCHAR(300),
    origin_full_address VARCHAR(500),
    origin_longitude DECIMAL(10,7),
    origin_latitude DECIMAL(10,7),
    origin_address_validation_status VARCHAR(20),
    destination_name VARCHAR(50),
    destination_phone VARCHAR(30),
    destination_province VARCHAR(50),
    destination_city VARCHAR(50),
    destination_district VARCHAR(50),
    destination_detail_address VARCHAR(300),
    destination_full_address VARCHAR(500),
    destination_longitude DECIMAL(10,7),
    destination_latitude DECIMAL(10,7),
    destination_address_validation_status VARCHAR(20),
    shipped_at TIMESTAMP NOT NULL,
    estimated_delivered_at TIMESTAMP NULL,
    delivered_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_shipments_carrier_tracking (carrier_code, tracking_number),
    FOREIGN KEY (order_id) REFERENCES orders(id)
);

CREATE TABLE IF NOT EXISTS shipment_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    shipment_id BIGINT NOT NULL,
    status ENUM('SHIPPED','IN_TRANSIT','OUT_FOR_DELIVERY','DELIVERED','EXCEPTION') NOT NULL,
    event_time TIMESTAMP NOT NULL,
    location VARCHAR(100),
    description VARCHAR(500) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (shipment_id) REFERENCES shipments(id)
);

INSERT IGNORE INTO categories (name) VALUES ('电子产品'),('服装'),('食品'),('图书'),('家居');

-- Migration for existing DBs: ALTER TABLE users MODIFY role ENUM('BUYER','SELLER','ADMIN') NOT NULL DEFAULT 'BUYER';
-- Migration for existing DBs: ALTER TABLE users ADD COLUMN IF NOT EXISTS status ENUM('ACTIVE','DISABLED') NOT NULL DEFAULT 'ACTIVE';
-- Migration for existing DBs:
-- ALTER TABLE shops ADD COLUMN IF NOT EXISTS province VARCHAR(50);
-- ALTER TABLE shops ADD COLUMN IF NOT EXISTS city VARCHAR(50);
-- ALTER TABLE shops ADD COLUMN IF NOT EXISTS district VARCHAR(50);
-- ALTER TABLE shops ADD COLUMN IF NOT EXISTS detail_address VARCHAR(300);
-- ALTER TABLE shops ADD COLUMN IF NOT EXISTS full_address VARCHAR(500);
-- ALTER TABLE shops ADD COLUMN IF NOT EXISTS longitude DECIMAL(10,7);
-- ALTER TABLE shops ADD COLUMN IF NOT EXISTS latitude DECIMAL(10,7);
-- ALTER TABLE shops ADD COLUMN IF NOT EXISTS address_validation_status VARCHAR(20) NOT NULL DEFAULT 'UNVERIFIED';
-- ALTER TABLE orders ADD COLUMN IF NOT EXISTS receiver_name VARCHAR(50);
-- ALTER TABLE orders ADD COLUMN IF NOT EXISTS receiver_phone VARCHAR(30);
-- ALTER TABLE orders ADD COLUMN IF NOT EXISTS receiver_province VARCHAR(50);
-- ALTER TABLE orders ADD COLUMN IF NOT EXISTS receiver_city VARCHAR(50);
-- ALTER TABLE orders ADD COLUMN IF NOT EXISTS receiver_district VARCHAR(50);
-- ALTER TABLE orders ADD COLUMN IF NOT EXISTS receiver_detail_address VARCHAR(300);
-- ALTER TABLE orders ADD COLUMN IF NOT EXISTS receiver_full_address VARCHAR(500);
-- ALTER TABLE orders ADD COLUMN IF NOT EXISTS receiver_longitude DECIMAL(10,7);
-- ALTER TABLE orders ADD COLUMN IF NOT EXISTS receiver_latitude DECIMAL(10,7);
-- ALTER TABLE orders ADD COLUMN IF NOT EXISTS receiver_address_validation_status VARCHAR(20) NOT NULL DEFAULT 'UNVERIFIED';
-- ALTER TABLE orders ADD COLUMN IF NOT EXISTS original_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00;
-- ALTER TABLE orders ADD COLUMN IF NOT EXISTS discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00;
-- ALTER TABLE orders ADD COLUMN IF NOT EXISTS coupon_id BIGINT;
-- ALTER TABLE orders ADD COLUMN IF NOT EXISTS buyer_coupon_id BIGINT;
-- ALTER TABLE orders ADD COLUMN IF NOT EXISTS coupon_name VARCHAR(100);
-- ALTER TABLE orders ADD COLUMN IF NOT EXISTS coupon_threshold_amount DECIMAL(12,2);
-- ALTER TABLE orders ADD COLUMN IF NOT EXISTS coupon_discount_amount DECIMAL(12,2);
-- ALTER TABLE coupons ADD COLUMN IF NOT EXISTS audience VARCHAR(20) NOT NULL DEFAULT 'PUBLIC';
-- ALTER TABLE coupons ADD COLUMN IF NOT EXISTS stackable BOOLEAN NOT NULL DEFAULT FALSE;
-- CREATE TABLE IF NOT EXISTS order_coupon_usages (... see definition above ...);
-- ALTER TABLE orders ADD COLUMN IF NOT EXISTS membership_plan_id BIGINT;
-- ALTER TABLE orders ADD COLUMN IF NOT EXISTS membership_plan_name VARCHAR(100);
-- ALTER TABLE orders ADD COLUMN IF NOT EXISTS membership_discount_rate DECIMAL(5,4);
-- ALTER TABLE orders ADD COLUMN IF NOT EXISTS membership_discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00;
-- Migration for existing DBs:
-- CREATE TABLE IF NOT EXISTS buyer_addresses (... see definition above ...);
-- CREATE TABLE IF NOT EXISTS coupons (... see definition above ...);
-- CREATE TABLE IF NOT EXISTS buyer_coupons (... see definition above ...);
-- CREATE TABLE IF NOT EXISTS membership_plans (... see definition above ...);
-- CREATE TABLE IF NOT EXISTS buyer_memberships (... see definition above ...);
-- CREATE TABLE IF NOT EXISTS membership_purchases (... see definition above ...);
-- CREATE TABLE IF NOT EXISTS membership_monthly_benefits (... see definition above ...);
-- CREATE TABLE IF NOT EXISTS shop_favorites (... see definition above ...);
-- CREATE TABLE IF NOT EXISTS price_alerts (... see definition above ...);
-- CREATE TABLE IF NOT EXISTS buyer_notifications (... see definition above ...);
-- Migration for existing DBs:
-- CREATE TABLE IF NOT EXISTS shipments (... see definition above ...);
-- CREATE TABLE IF NOT EXISTS shipment_events (... see definition above ...);

-- Initial ADMIN account: email=admin@trading.com, password=Admin@123456
INSERT IGNORE INTO users (username, email, password, role, status)
VALUES ('admin3', 'admin@trading.com',
        '$2a$10$pkqoMC7xbE3Nau.fWoxsZOwHgsD1Q9OUE/5nQUd43dGGhQcUkoI9.',
        'ADMIN', 'ACTIVE');
