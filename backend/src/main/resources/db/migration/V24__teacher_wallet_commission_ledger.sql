alter table teacher_transactions
    add column payment_id bigint;

alter table teacher_transactions
    add column source_type varchar(40);

alter table teacher_transactions
    add column source_reference varchar(160);

alter table teacher_transactions
    add constraint fk_teacher_transactions_payment
    foreign key (payment_id) references payments (id) on delete set null;

alter table teacher_transactions
    add constraint chk_teacher_transactions_source_type
    check (
        source_type is null
        or source_type in (
            'marketplace_purchase',
            'live_speaking_booking',
            'withdrawal_request',
            'refund',
            'adjustment'
        )
    );

create unique index uq_teacher_transactions_purchase_type
    on teacher_transactions (purchase_id, transaction_type);

create unique index uq_teacher_transactions_source_type_reference_type
    on teacher_transactions (source_type, source_reference, transaction_type);

create index idx_teacher_transactions_payment on teacher_transactions (payment_id);
create index idx_teacher_transactions_source on teacher_transactions (source_type, source_reference);

update platform_settings
set setting_value = '{"value": 25.00}'
where setting_key = 'marketplace.default_commission_percent';
