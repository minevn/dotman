-- Tạo table: Config dạng EAV
CREATE TABLE "dotman_config" (
    "key"      VARCHAR(64) NOT NULL PRIMARY KEY,
    "value"    VARCHAR(255) NOT NULL
);

-- Tạo table: Log nạp thẻ
CREATE TABLE "dotman_napthe_log" (
    "id"                INT AUTO_INCREMENT PRIMARY KEY,
    "name"              VARCHAR(64) NOT NULL,
    "uuid"              VARCHAR(100) NOT NULL,
    "seri"              VARCHAR(20) NOT NULL,
    "pin"               VARCHAR(20) NOT NULL,
    "type"              VARCHAR(20) NOT NULL,
    "price"             INT NOT NULL,
    "time"              BIGINT NOT NULL,
    "success"           TINYINT NOT NULL DEFAULT 0,
    "waiting"           TINYINT NOT NULL DEFAULT 0,
    "server"            VARCHAR(32) NOT NULL DEFAULT 'web',
    "pointsnhan"        INT NOT NULL DEFAULT 0,
    "thucnhan"          INT NOT NULL DEFAULT 0,
    "transaction_id"    VARCHAR(32) DEFAULT NULL
);

-- Tạo table: Log sử dụng point
CREATE TABLE "dotman_point_log" (
    "id"            INT AUTO_INCREMENT PRIMARY KEY,
    "name"          VARCHAR(64) NOT NULL,
    "uuid"          VARCHAR(100) NOT NULL,
    "amount"        INT NOT NULL,
    "point_from"    INT NOT NULL,
    "point_to"      INT NOT NULL,
    "time"          BIGINT NOT NULL,
    "server"        VARCHAR(32) NOT NULL DEFAULT 'web',
    "content"       VARCHAR(255) DEFAULT NULL
);
