-- 扩展数据库字段
-- Run this SQL in your MySQL to add missing fields

USE FireBird_DB;

-- 1. 为user表添加余额字段（充值功能）
ALTER TABLE user ADD COLUMN balance DECIMAL(12,2) DEFAULT 0;

-- 2. 为user表添加审核状态字段（开店审核功能）
ALTER TABLE user ADD COLUMN status VARCHAR(50) DEFAULT 'approved';
ALTER TABLE user MODIFY COLUMN role VARCHAR(20) DEFAULT 'user';

-- 3. 为items表添加审核状态字段（上架审核功能，如果不存在的话）
ALTER TABLE items ADD COLUMN IF NOT EXISTS status VARCHAR(50) DEFAULT 'approved';

-- 4. 为orders表添加支付时间、发货时间、物流单号字段
ALTER TABLE orders ADD COLUMN IF NOT EXISTS pay_time DATETIME DEFAULT NULL;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS ship_time DATETIME DEFAULT NULL;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS deliver_time DATETIME DEFAULT NULL;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS tracking_number VARCHAR(100) DEFAULT NULL;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS shipping_address TEXT DEFAULT NULL;

-- 5. 为orders表添加收货确认时间字段
ALTER TABLE orders ADD COLUMN IF NOT EXISTS receive_time DATETIME DEFAULT NULL;

-- 6. 为orders表添加退款状态字段（退货退款功能）
ALTER TABLE orders ADD COLUMN IF NOT EXISTS refund_status VARCHAR(50) DEFAULT NULL;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS refund_time DATETIME DEFAULT NULL;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS refund_reason TEXT DEFAULT NULL;

-- 7. 添加充值记录表
CREATE TABLE IF NOT EXISTS recharge_records (
  id VARCHAR(36) NOT NULL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  payment_method VARCHAR(50) DEFAULT 'alipay',
  status VARCHAR(50) DEFAULT 'completed',
  date DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 8. 添加退款记录表
CREATE TABLE IF NOT EXISTS refund_records (
  id VARCHAR(36) NOT NULL PRIMARY KEY,
  order_id VARCHAR(36) NOT NULL,
  user_id BIGINT NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  reason TEXT DEFAULT NULL,
  status VARCHAR(50) DEFAULT 'pending',
  date DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_order (order_id),
  INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;