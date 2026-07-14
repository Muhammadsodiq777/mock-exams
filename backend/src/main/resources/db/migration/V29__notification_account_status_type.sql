alter table notifications drop constraint chk_notifications_type;

alter table notifications add constraint chk_notifications_type
    check (type in ('system', 'test_moderation', 'payment', 'withdrawal', 'ai_feedback', 'account_status'));
