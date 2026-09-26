create table if not exists shedlock (
    name       varchar(64)  not null,
    lock_until timestamp with time zone not null,
    locked_at  timestamp with time zone not null,
    locked_by  varchar(255) not null,
    primary key (name)
);
