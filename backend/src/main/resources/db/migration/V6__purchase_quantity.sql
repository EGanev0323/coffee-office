-- Колко пъти е купен пакетът наведнъж (бутоните − / +). coffee_count и amount са общо за покупката.
alter table purchase add column quantity integer not null default 1 check (quantity > 0);
