CREATE DATABASE IF NOT EXISTS tickethub
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;

-- 1. ticket table
CREATE TABLE IF NOT EXISTS `tickethub`.`tbl_ticket`
(
    `id`          VARCHAR(36) NOT NULL COMMENT 'Primary key',
    `name`        VARCHAR(50) NOT NULL COMMENT 'ticket name',
    `description` TEXT COMMENT 'ticket description',
    `start_time`  DATETIME    NOT NULL COMMENT 'ticket sale start time',
    `end_time`    DATETIME    NOT NULL COMMENT 'ticket sale end time',
    `status`      INT(11)     NOT NULL DEFAULT 0 COMMENT 'ticket sale activity status',
    `updated_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Last update time',
    `created_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
    PRIMARY KEY (`id`),
    KEY `idx_end_time` (`end_time`),
    KEY `idx_start_time` (`start_time`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = 'ticket table';

-- 2. ticket detail (item) table
CREATE TABLE IF NOT EXISTS `tickethub`.`tbl_ticket_item`
(
    `id`                VARCHAR(36) NOT NULL COMMENT 'Primary key',
    `name`              VARCHAR(50) NOT NULL COMMENT 'Ticket title',
    `description`       TEXT COMMENT 'Ticket description',
    `stock_initial`     INT(11)     NOT NULL DEFAULT 0 COMMENT 'Initial stock quantity (e.g., 1000 tickets)',
    `stock_available`   INT(11)     NOT NULL DEFAULT 0 COMMENT 'Current available stock (e.g., 900 tickets)',
    `is_stock_prepared` BOOLEAN     NOT NULL DEFAULT 0 COMMENT 'Indicates if stock is pre-warmed (0/1)',
    `price_original`    BIGINT(20)  NOT NULL COMMENT 'Original ticket price',
    `price_flash`       BIGINT(20)  NOT NULL COMMENT 'Discounted price during flash sale',
    `sale_start_time`   DATETIME    NOT NULL COMMENT 'Flash sale start time',
    `sale_end_time`     DATETIME    NOT NULL COMMENT 'Flash sale end time',
    `status`            INT(11)     NOT NULL DEFAULT 0 COMMENT 'Ticket status (e.g., active/inactive)',
    `activity_id`       VARCHAR(36) NOT NULL COMMENT 'ID of associated activity',
    `updated_at`        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Timestamp of the last update',
    `created_at`        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
    PRIMARY KEY (`id`),
    KEY `idx_end_time` (`sale_end_time`),
    KEY `idx_start_time` (`sale_start_time`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = 'Table for ticket details';

-- -- INSERT MOCK DATA
-- -- Insert data into `ticket` table
INSERT INTO `tickethub`.`tbl_ticket` (`id`, `name`, `description`, `start_time`, `end_time`, `status`, `updated_at`,
                                      `created_at`)
VALUES ('550e8400-e29b-41d4-a716-446655440000', 'Đợt Mở Bán Vé Ngày 12/12', 'Sự kiện mở bán vé đặc biệt cho ngày 12/12',
        '2024-12-12 00:00:00', '2024-12-12 23:59:59', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('6ba7b810-9dad-41d1-80b4-00c04fd430c8', 'Đợt Mở Bán Vé Ngày 01/01',
        'Sự kiện mở bán vé cho ngày đầu năm mới 01/01', '2025-01-01 00:00:00', '2025-01-01 23:59:59', 1,
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- -- Insert data into `ticket_item` table corresponding to each event in `ticket` table
INSERT INTO `tickethub`.`tbl_ticket_item` (`id`, `name`, `description`, `stock_initial`, `stock_available`,
                                           `is_stock_prepared`, `price_original`, `price_flash`, `sale_start_time`,
                                           `sale_end_time`, `status`, `activity_id`, `updated_at`, `created_at`)
VALUES
    -- Ticket items for the 12/12 event
    ('f47ac10b-58cc-4372-a567-0e02b2c3d479', 'Vé Sự Kiện 12/12 - Hạng Phổ Thông', 'Vé phổ thông cho sự kiện ngày 12/12',
     1000, 1000, 0, 100000, 10000, '2024-12-12 00:00:00', '2024-12-12 23:59:59', 1,
     '550e8400-e29b-41d4-a716-446655440000', CURRENT_TIMESTAMP,
     CURRENT_TIMESTAMP),
    ('7d444840-9dc0-11d1-b245-5ffdce74fad2', 'Vé Sự Kiện 12/12 - Hạng VIP', 'Vé VIP cho sự kiện ngày 12/12', 500, 500,
     0, 200000, 15000, '2024-12-12 00:00:00', '2024-12-12 23:59:59', 1, '550e8400-e29b-41d4-a716-446655440000',
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    -- Ticket items for the 01/01 event
    ('9f8c7d6e-5b4a-4321-9c8d-7e6f5a4b3c2d', 'Vé Sự Kiện 01/01 - Hạng Phổ Thông', 'Vé phổ thông cho sự kiện ngày 01/01',
     2000, 2000, 0, 100000, 10000, '2025-01-01 00:00:00', '2025-01-01 23:59:59', 1,
     '6ba7b810-9dad-41d1-80b4-00c04fd430c8', CURRENT_TIMESTAMP,
     CURRENT_TIMESTAMP),
    ('3f2504e0-4f89-41d3-9a0c-0305e82c3301', 'Vé Sự Kiện 01/01 - Hạng VIP', 'Vé VIP cho sự kiện ngày 01/01', 1000, 1000,
     0, 200000, 15000, '2025-01-01 00:00:00', '2025-01-01 23:59:59', 1, '6ba7b810-9dad-41d1-80b4-00c04fd430c8',
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 3. order queue table
CREATE TABLE IF NOT EXISTS `tickethub`.`tbl_order_queue`
(
    id           VARCHAR(36)  NOT NULL PRIMARY KEY,
    token        VARCHAR(38)  NOT NULL UNIQUE,
    ticket_id    VARCHAR(36)  NOT NULL,
    quantity     INT          NOT NULL,
    user_id      VARCHAR(36)  NOT NULL,
    status       TINYINT      NOT NULL DEFAULT 0,
    order_number VARCHAR(64)  NULL,
    message      VARCHAR(255) NULL,
    created_at   DATETIME              DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 4. Outbox Event
CREATE TABLE IF NOT EXISTS `tickethub`.`tbl_outbox_event`
(
    id           VARCHAR(36) NOT NULL PRIMARY KEY,
    aggregate_id VARCHAR(38) NOT NULL COMMENT 'Token của order — dùng để idempotency check phía consumer',
    event_type   VARCHAR(64) NOT NULL COMMENT 'Loại event, ví dụ: ORDER_PLACED',
    payload      TEXT        NOT NULL COMMENT 'JSON của PlaceOrderMQMessage',
    status       TINYINT     NOT NULL DEFAULT 0 COMMENT '0=PENDING, 1=PUBLISHED',
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at DATETIME    NULL COMMENT 'Thời điểm Kafka Broker ACK thành công',
    INDEX `idx_status_created` (status, created_at),
    INDEX `idx_status_type_created` (status, event_type, created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = 'Outbox event table — đảm bảo atomicity giữa DB write và Kafka publish';

-- 5. idempotency_key table - consumer gate
CREATE TABLE IF NOT EXISTS `tickethub`.`tbl_idempotency_key`
(
    token      VARCHAR(38) NOT NULL PRIMARY KEY COMMENT 'Unique token từ PlaceOrderMQMessage',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Thời điểm consumer nhận lần đầu',
    expired_at DATETIME    NOT NULL COMMENT 'TTL — dùng cho cleanup job',
    INDEX idx_idempotency_expires_at (expired_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = 'Idempotency gate — INSERT IGNORE prevents duplicate processing';

-- 6. user table
CREATE TABLE IF NOT EXISTS `tickethub`.`tbl_user`
(
    id           VARCHAR(36)  NOT NULL PRIMARY KEY,
    username     VARCHAR(255) NOT NULL UNIQUE,
    email        VARCHAR(255) NOT NULL UNIQUE,
    phone_number VARCHAR(10)  NOT NULL UNIQUE,
    password     VARCHAR(255) NOT NULL,
    status       TINYINT      NOT NULL DEFAULT 0 COMMENT '0=PENDING, 1=ACTIVE, 2=DELETED',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_id (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = 'Table for user';

# INSERT INTO `tickethub`.`tbl_user`(id, username, email, phone_number, password, created_at, updated_at)
# VALUES (``);

-- 7. role table
CREATE TABLE IF NOT EXISTS `tickethub`.`tbl_role`
(
    `id`         VARCHAR(36)  NOT NULL PRIMARY KEY,
    `name`       VARCHAR(30)  NOT NULL COMMENT 'Tên vai trò',
    `key`        VARCHAR(100) NOT NULL COMMENT 'Chuỗi định danh vai trò',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = 'Table for role';

INSERT INTO `tickethub`.tbl_role (id, name, `key`, created_at, updated_at)
VALUES ('01a105ff-97f5-77e8-9df7-8e0ff3338c04', 'admin', 'ADMIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('01a105ff-b296-7120-9674-a36228395518', 'member', 'MEMBER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('01a109f8-430b-74da-a56e-c68df7676a9e', 'guest', 'GUEST', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 8. permission table
CREATE TABLE IF NOT EXISTS `tickethub`.`tbl_permission`
(
    id              VARCHAR(36)  NOT NULL PRIMARY KEY,
    permission_name VARCHAR(30)  NOT NULL COMMENT 'Tên quyền',
    permission_key  VARCHAR(100) NOT NULL COMMENT 'Chuỗi định danh quyền',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = 'Table for permission';

-- 9. user_role table
CREATE TABLE IF NOT EXISTS `tickethub`.`tbl_user_has_role`
(
    `user_id` VARCHAR(36) NOT NULL COMMENT 'ID người dùng',
    `role_id` VARCHAR(36) NOT NULL COMMENT 'ID vai trò',
    PRIMARY KEY (user_id, role_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = 'Bảng liên kết user với role';

-- 10. role_permission table
CREATE TABLE IF NOT EXISTS `tickethub`.`tbl_role_has_permission`
(
    `role_id`       VARCHAR(36) NOT NULL COMMENT 'ID vai trò',
    `permission_id` VARCHAR(36) NOT NULL COMMENT 'ID quyền',
    PRIMARY KEY (role_id, permission_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = 'Bảng liên kết role với permission';

-- 11. payment transaction table
CREATE TABLE IF NOT EXISTS `tickethub`.`tbl_payment_transaction`
(
    `id`                     VARCHAR(36)    NOT NULL,
    `payment_id`             VARCHAR(64)    NOT NULL COMMENT 'Mã thanh toán duy nhất (UUID)',
    `order_number`           VARCHAR(64)    NOT NULL COMMENT 'Liên kết với bảng Order',
    `user_id`                VARCHAR(36)    NOT NULL,

    `amount`                 DECIMAL(16, 3) NOT NULL COMMENT 'Số tiền thanh toán',
    `payment_method`         VARCHAR(20)    NOT NULL COMMENT 'VNPAY, MOMO, LINKED_BANK...',
    `payment_status`         TINYINT        NOT NULL DEFAULT 0 COMMENT '0:INIT, 1:IN_PROGRESS, 2:SUCCESS, 3:FAILED',

    `gateway_transaction_id` VARCHAR(100)   NULL COMMENT 'Mã từ phía Ngân hàng trả về',
    `payment_url`            TEXT           NULL COMMENT 'Link thanh toán trả về cho User',
    `create_date`           VARCHAR(16)    NOT NULL COMMENT 'Thời gian tạo link url được vnpay gửi',

    `updated_at`             DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_at`             DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_payment_id` (`payment_id`),
    KEY `idx_order_number` (`order_number`),
    KEY `idx_user_id` (`user_id`),
    INDEX idx_status_created (payment_status, created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='Bảng ghi nhận lịch sử thanh toán';