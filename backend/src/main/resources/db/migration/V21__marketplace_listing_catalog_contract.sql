alter table marketplace_listings
    add column language varchar(20) not null default 'en';

alter table marketplace_listings
    add constraint chk_marketplace_listings_language_not_blank
    check (char_length(trim(language)) > 0);

create index idx_marketplace_listings_active_language on marketplace_listings (is_active, language);
create index idx_marketplace_listings_active_price on marketplace_listings (is_active, price_amount);
