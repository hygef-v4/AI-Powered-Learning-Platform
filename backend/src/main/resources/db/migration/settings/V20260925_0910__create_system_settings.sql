-- U03: cài đặt hệ thống dạng khóa–giá trị (UC 14, 15). FK updated_by -> accounts do migration của U01 thêm.
create table system_settings (
    key           varchar(100) primary key,
    setting_group varchar(20)  not null,
    value         jsonb,
    version       bigint       not null default 1,
    updated_by    uuid,
    updated_at    timestamptz  not null default now()
);
