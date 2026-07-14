alter table withdrawal_requests
    add column paid_at timestamp with time zone;

alter table withdrawal_requests
    add constraint chk_withdrawal_requests_paid_at
    check (paid_at is null or paid_at >= created_at);

insert into platform_settings (setting_key, setting_value)
values ('wallet.minimum_withdrawal_amount', '{"value": 10000.00}');
