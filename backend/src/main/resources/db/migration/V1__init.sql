create table app_user (
    id            bigserial primary key,
    username      varchar(50)  not null unique,
    password_hash varchar(100) not null,
    display_name  varchar(100) not null,
    role          varchar(20)  not null,
    balance       integer      not null default 0 check (balance >= 0),
    active        boolean      not null default true,
    created_at    timestamptz  not null default now()
);

create table coffee_package (
    id           bigserial primary key,
    name         varchar(100)  not null,
    coffee_count integer       not null check (coffee_count > 0),
    price        numeric(10,2) not null check (price >= 0),
    active       boolean       not null default true,
    sort_order   integer       not null default 0
);

create table purchase (
    id            bigserial primary key,
    user_id       bigint        not null references app_user(id),
    package_name  varchar(100)  not null,
    coffee_count  integer       not null check (coffee_count > 0),
    amount        numeric(10,2) not null,
    created_at    timestamptz   not null default now(),
    created_by_id bigint        references app_user(id)
);
create index idx_purchase_created_at on purchase(created_at);
create index idx_purchase_user on purchase(user_id, created_at desc);

create table consumption (
    id         bigserial primary key,
    user_id    bigint      not null references app_user(id),
    created_at timestamptz not null default now()
);
create index idx_consumption_created_at on consumption(created_at);
create index idx_consumption_user on consumption(user_id, created_at desc);

insert into coffee_package (name, coffee_count, price, sort_order) values
    ('5 кафета', 5, 2.00, 1),
    ('10 кафета', 10, 4.00, 2);
