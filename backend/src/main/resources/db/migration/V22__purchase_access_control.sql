alter table purchases
    add column status varchar(24) not null default 'granted';

alter table purchases
    add column source_type varchar(40) not null default 'legacy';

alter table purchases
    add column source_reference varchar(160);

alter table purchases
    add column source_metadata jsonb;

alter table purchases
    add column amount numeric(14,2) not null default 0;

alter table purchases
    add column currency varchar(3) not null default 'UZS';

alter table purchases
    add column updated_at timestamp with time zone not null default current_timestamp;

alter table purchases
    add constraint chk_purchases_status
    check (status in ('granted', 'revoked', 'refunded'));

alter table purchases
    add constraint chk_purchases_source_type
    check (source_type in ('free_listing', 'payment', 'manual_admin', 'subscription', 'legacy'));

alter table purchases
    add constraint chk_purchases_amount_non_negative
    check (amount >= 0);

alter table purchases
    add constraint chk_purchases_currency_length
    check (char_length(currency) = 3);

create index idx_purchases_student_listing_status_expiry
    on purchases (student_user_id, listing_id, status, access_expires_at);

create index idx_purchases_student_status_purchased
    on purchases (student_user_id, status, purchased_at);

create index idx_subscriptions_student_listing_status_window
    on subscriptions (student_user_id, listing_id, status, starts_at, ends_at);
