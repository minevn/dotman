-- điều chỉnh uuid các table cũ thành char(36)
alter table dotman_napthe_log
    modify uuid char(36) not null;

alter table dotman_player_data
    modify uuid char(36) not null;

alter table dotman_point_log
    modify uuid char(36) not null;

-- them index cho uuid
create index dotman_point_log_uuid_index
    on dotman_point_log (uuid);

create index dotman_napthe_log_uuid_index
    on dotman_napthe_log (uuid);

-- tạo bảng lưu uuid
create table dotman_player_info
(
    uuid char(36)    not null,
    name varchar(32) not null,
    last_updated bigint not null,
    constraint dotman_player_info_pk
        primary key (uuid)
);
create index dotman_player_info_name_index
    on dotman_player_info (name);
