CREATE TABLE IF NOT EXISTS customers (
  customer_id VARCHAR(190) PRIMARY KEY,
  provider VARCHAR(24) NOT NULL,
  email VARCHAR(255),
  full_name VARCHAR(160) NOT NULL,
  phone VARCHAR(40),
  delivery_address TEXT,
  address_lat DOUBLE,
  address_lng DOUBLE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS products (
  product_id VARCHAR(64) PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  description VARCHAR(500) NOT NULL,
  category VARCHAR(60) NOT NULL,
  price DECIMAL(10,2) NOT NULL,
  image_url VARCHAR(1000),
  is_available BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS store_settings (
  setting_key VARCHAR(100) PRIMARY KEY,
  setting_value TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS bakery_orders (
  order_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_code VARCHAR(40) NOT NULL UNIQUE,
  customer_id VARCHAR(190) NOT NULL,
  customer_name VARCHAR(160) NOT NULL,
  customer_phone VARCHAR(40) NOT NULL,
  delivery_address TEXT NOT NULL,
  delivery_lat DOUBLE,
  delivery_lng DOUBLE,
  fulfillment_at TIMESTAMP NOT NULL,
  distance_km DECIMAL(8,2) NOT NULL,
  subtotal DECIMAL(10,2) NOT NULL,
  delivery_fee DECIMAL(10,2) NOT NULL,
  shipping_discount DECIMAL(10,2) NOT NULL,
  total DECIMAL(10,2) NOT NULL,
  payment_method VARCHAR(24) NOT NULL,
  payment_status VARCHAR(40) NOT NULL,
  order_status VARCHAR(40) NOT NULL,
  gcash_reference VARCHAR(100),
  no_refund_ack BOOLEAN NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS bakery_order_items (
  item_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  product_id VARCHAR(64) NOT NULL,
  product_name VARCHAR(120) NOT NULL,
  quantity INT NOT NULL,
  unit_price DECIMAL(10,2) NOT NULL,
  line_total DECIMAL(10,2) NOT NULL
);
