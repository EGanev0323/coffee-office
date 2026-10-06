-- Колеги с безкрайни кафета: балансът им не намалява при изпито кафе.
alter table app_user add column unlimited boolean not null default false;
