-- Tạo table config dạng EAV nhưng dành cho head server
CREATE TABLE `dotman_config_head`
(
    `key`   VARCHAR(64)  NOT NULL PRIMARY KEY,
    `value` VARCHAR(255) NOT NULL
);
