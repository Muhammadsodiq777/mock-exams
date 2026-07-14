alter table payments
    add column target_type varchar(40) not null default 'legacy';

alter table payments
    add column target_listing_id bigint;

alter table payments
    add column target_subscription_id bigint;

alter table payments
    add column target_live_speaking_booking_id bigint;

alter table payments
    add column provider_checkout_url varchar(500);

alter table payments
    add column provider_event_id varchar(160);

alter table payments
    add column provider_amount numeric(14,2);

alter table payments
    add column provider_currency varchar(3);

alter table payments
    add column failure_code varchar(80);

alter table payments
    add column failure_message varchar(500);

alter table payments
    add column confirmation_dispatched_at timestamp with time zone;

alter table payments
    add constraint fk_payments_target_listing
    foreign key (target_listing_id) references marketplace_listings (id) on delete set null;

alter table payments
    add constraint fk_payments_target_subscription
    foreign key (target_subscription_id) references subscriptions (id) on delete set null;

alter table payments
    add constraint chk_payments_target_type
    check (target_type in ('marketplace_listing', 'subscription', 'live_speaking_booking', 'legacy'));

alter table payments
    add constraint chk_payments_provider_currency_length
    check (provider_currency is null or char_length(provider_currency) = 3);

alter table payments
    add constraint chk_payments_confirmation_dispatch_after_create
    check (confirmation_dispatched_at is null or confirmation_dispatched_at >= created_at);

create index idx_payments_target_listing_status
    on payments (target_listing_id, status, created_at);

create index idx_payments_target_subscription_status
    on payments (target_subscription_id, status, created_at);

create index idx_payments_confirmation_dispatch
    on payments (status, confirmation_dispatched_at);
