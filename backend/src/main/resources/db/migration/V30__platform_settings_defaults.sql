update platform_settings
set setting_value = '{"value": 10000.00, "currency": "UZS"}'
where setting_key = 'wallet.minimum_withdrawal_amount'
  and lower(cast(setting_value as varchar)) not like '%"currency"%'
  and cast(setting_value as varchar) like '%10000%';

insert into platform_settings (setting_key, setting_value)
select 'platform.default_commission_percent', '{"value": 25.00}'
where not exists (select 1 from platform_settings where setting_key = 'platform.default_commission_percent');

insert into platform_settings (setting_key, setting_value)
select 'live_speaking.platform_commission_percent', '{"value": 25.00}'
where not exists (select 1 from platform_settings where setting_key = 'live_speaking.platform_commission_percent');

insert into platform_settings (setting_key, setting_value)
select 'ai.hard_monthly_requests', '{"value": 50, "period": "monthly"}'
where not exists (select 1 from platform_settings where setting_key = 'ai.hard_monthly_requests');

insert into platform_settings (setting_key, setting_value)
select 'ai.default_model', '{"provider": "openai", "model": "gpt-4.1-mini"}'
where not exists (select 1 from platform_settings where setting_key = 'ai.default_model');

insert into platform_settings (setting_key, setting_value)
select 'ai.rubric_defaults', '{"writingRubricVersion": "ielts-writing-v1", "speakingRubricVersion": "ielts-speaking-v1"}'
where not exists (select 1 from platform_settings where setting_key = 'ai.rubric_defaults');

insert into platform_settings (setting_key, setting_value)
select 'ai.premium_monthly_price', '{"amount": 0.00, "currency": "UZS"}'
where not exists (select 1 from platform_settings where setting_key = 'ai.premium_monthly_price');

insert into platform_settings (setting_key, setting_value)
select 'payments.provider_mode', '{"enabled": false, "provider": "mock", "mode": "mock"}'
where not exists (select 1 from platform_settings where setting_key = 'payments.provider_mode');

insert into platform_settings (setting_key, setting_value)
select 'payments.provider_options', '{"defaultProvider": "mock", "allowedProviders": ["mock"]}'
where not exists (select 1 from platform_settings where setting_key = 'payments.provider_options');

insert into platform_settings (setting_key, setting_value)
select 'moderation.behavior', '{"autoHideThreshold": 3, "manualReviewRequired": true}'
where not exists (select 1 from platform_settings where setting_key = 'moderation.behavior');

insert into platform_settings (setting_key, setting_value)
select 'rich_text.limits', '{"maxDocumentBytes": 65536, "allowedEmbedPurposes": ["passage_image", "diagram", "document"]}'
where not exists (select 1 from platform_settings where setting_key = 'rich_text.limits');

insert into platform_settings (setting_key, setting_value)
select 'google.calendar_meet', '{"enabled": false, "mode": "disabled", "calendarOwner": "platform"}'
where not exists (select 1 from platform_settings where setting_key = 'google.calendar_meet');

insert into platform_settings (setting_key, setting_value)
select 'live_speaking.cancellation_policy', '{"studentCancellationWindowHours": 24, "teacherCancellationWindowHours": 2}'
where not exists (select 1 from platform_settings where setting_key = 'live_speaking.cancellation_policy');
