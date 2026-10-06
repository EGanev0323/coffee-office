-- Дали колегата се показва на общата дъска (админът може да го скрие).
alter table app_user add column board_visible boolean not null default true;