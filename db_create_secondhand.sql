-- Run this SQL in your MySQL (adjust database name if needed)
USE FireBird_DB;

-- items table
CREATE TABLE IF NOT EXISTS items (
  id VARCHAR(36) NOT NULL,
  title VARCHAR(255) NOT NULL,
  price DECIMAL(10,2) NOT NULL DEFAULT 0,
  thumb VARCHAR(255) DEFAULT NULL,
  images TEXT DEFAULT NULL,
  description TEXT DEFAULT NULL,
  seller_id BIGINT DEFAULT NULL,
  seller_name VARCHAR(100) DEFAULT NULL,
  date DATETIME DEFAULT CURRENT_TIMESTAMP,
  category VARCHAR(100) DEFAULT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- cart table
CREATE TABLE IF NOT EXISTS cart (
  id VARCHAR(36) NOT NULL,
  item_id VARCHAR(36) NOT NULL,
  title VARCHAR(255) NOT NULL,
  price DECIMAL(10,2) NOT NULL DEFAULT 0,
  thumb VARCHAR(255) DEFAULT NULL,
  quantity INT NOT NULL DEFAULT 1,
  user_id BIGINT NOT NULL,
  PRIMARY KEY (id),
  INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- messages table
CREATE TABLE IF NOT EXISTS messages (
  id VARCHAR(36) NOT NULL,
  from_user_id BIGINT NOT NULL,
  from_username VARCHAR(100) DEFAULT NULL,
  to_user_id BIGINT NOT NULL,
  content TEXT NOT NULL,
  date DATETIME DEFAULT CURRENT_TIMESTAMP,
  -- item_id and item_title are used to associate messages with an item (may be NULL)
  item_id VARCHAR(36) DEFAULT NULL,
  item_title VARCHAR(255) DEFAULT NULL,
  is_read TINYINT(1) DEFAULT 0,
  PRIMARY KEY (id),
  INDEX idx_to_user (to_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- reviews table
CREATE TABLE IF NOT EXISTS reviews (
  id VARCHAR(36) NOT NULL,
  item_id VARCHAR(36) NOT NULL,
  user_id BIGINT NOT NULL,
  username VARCHAR(100) DEFAULT NULL,
  rating INT DEFAULT 5,
  comment TEXT DEFAULT NULL,
  date DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_item (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- orders and order_items
CREATE TABLE IF NOT EXISTS orders (
  order_id VARCHAR(36) NOT NULL,
  user_id BIGINT NOT NULL,
  total_amount DECIMAL(12,2) DEFAULT 0,
  status VARCHAR(50) DEFAULT 'created',
  date DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (order_id),
  INDEX idx_order_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS order_items (
  id VARCHAR(36) NOT NULL,
  order_id VARCHAR(36) NOT NULL,
  item_id VARCHAR(36) NOT NULL,
  title VARCHAR(255) NOT NULL,
  price DECIMAL(10,2) DEFAULT 0,
  quantity INT DEFAULT 1,
  PRIMARY KEY (id),
  INDEX idx_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- If you have already created the messages table without item_id/item_title,
-- run the following ALTER statements to add the missing columns:
-- ALTER TABLE messages ADD COLUMN item_id VARCHAR(36) DEFAULT NULL;
-- ALTER TABLE messages ADD COLUMN item_title VARCHAR(255) DEFAULT NULL;


