-- Tạo table: dữ liệu tích lũy của người chơi
create table "dotman_player_data"
(
    "uuid"         CHARACTER VARYING(36)  not null,
    "name"         CHARACTER VARYING(32)  not null,
    "key"          CHARACTER VARYING(100) not null,
    "value"        INTEGER default 0      not null,
    "last_updated" BIGINT  default 0      not null,
    constraint DOTMAN_PLAYER_DATA_PK
        unique ("uuid", "key")
);

