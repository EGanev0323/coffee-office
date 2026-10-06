-- Рисунки с маркер върху общата дъска. points е JSON масив от [x, y],
-- нормализирани спрямо ширината на дъската, за да изглеждат еднакво на всеки екран.
create table board_stroke (
    id         bigserial primary key,
    user_id    bigint      not null references app_user(id),
    color      varchar(7)  not null,
    points     text        not null,
    created_at timestamptz not null default now()
);