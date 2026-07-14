alter table promo_codes
    add column code_normalized varchar(60);

update promo_codes
set code_normalized = lower(code);

alter table promo_codes
    alter column code_normalized set not null;

alter table promo_codes
    add constraint uq_promo_codes_code_normalized unique (code_normalized);

alter table promo_codes
    add column updated_at timestamp with time zone;

update promo_codes
set updated_at = created_at;

alter table promo_codes
    alter column updated_at set not null;

alter table payments
    add column promo_code_id bigint;

alter table payments
    add column promo_code varchar(60);

alter table payments
    add column promo_discount_amount numeric(14,2);

alter table payments
    add constraint fk_payments_promo_code
    foreign key (promo_code_id) references promo_codes (id) on delete set null;

alter table payments
    add constraint chk_payments_promo_discount_non_negative
    check (promo_discount_amount is null or promo_discount_amount >= 0);

create unique index uq_promo_redemptions_code_payment
    on promo_code_redemptions (promo_code_id, payment_id);

create index idx_promo_redemptions_code_redeemed
    on promo_code_redemptions (promo_code_id, redeemed_at);

create index idx_payments_promo_code
    on payments (promo_code_id);
