-- Adjust uuid of old tables to varchar(36)
ALTER TABLE "dotman_napthe_log"
    ALTER COLUMN "uuid" SET DATA TYPE VARCHAR(36);
ALTER TABLE "dotman_napthe_log"
    ALTER COLUMN "uuid" SET NOT NULL;

ALTER TABLE "dotman_player_data"
    ALTER COLUMN "uuid" SET DATA TYPE VARCHAR(36);
ALTER TABLE "dotman_player_data"
    ALTER COLUMN "uuid" SET NOT NULL;

ALTER TABLE "dotman_point_log"
    ALTER COLUMN "uuid" SET DATA TYPE VARCHAR(36);
ALTER TABLE "dotman_point_log"
    ALTER COLUMN "uuid" SET NOT NULL;

-- tao index cho uuid
create index dotman_napthe_log_uuid_index
    on "dotman_napthe_log" ("uuid");

create index dotman_point_log_uuid_index
    on "dotman_point_log" ("uuid");

-- Create table to store uuid
CREATE TABLE "dotman_player_info"
(
    "uuid" VARCHAR(36) NOT NULL,
    "name" VARCHAR(32) NOT NULL,
    "last_updated" BIGINT NOT NULL,
    CONSTRAINT "dotman_player_info_pk"
        PRIMARY KEY ("uuid")
);
create index dotman_player_info_name_index
    on "dotman_player_info" ("name");
