-- Версия на токените: увеличава се при смяна на паролата и така обезсилва
-- всички издадени досега токени („Запомни ме“ на други устройства).
alter table app_user add column token_version integer not null default 0;