alter table speaking_submissions
    add column transcription_status varchar(24) not null default 'queued';

alter table speaking_submissions
    add column transcription_provider varchar(80);

alter table speaking_submissions
    add column transcription_model varchar(120);

alter table speaking_submissions
    add column transcription_failure_code varchar(64);

alter table speaking_submissions
    add column transcription_error_message text;

alter table speaking_submissions
    add column transcription_estimated_cost numeric(12,6) not null default 0;

alter table speaking_submissions
    add column transcription_started_at timestamp with time zone;

alter table speaking_submissions
    add column transcription_completed_at timestamp with time zone;

alter table speaking_submissions
    add constraint chk_speaking_submissions_transcription_status
        check (transcription_status in ('queued', 'processing', 'completed', 'failed'));

alter table speaking_submissions
    add constraint chk_speaking_submissions_transcription_cost
        check (transcription_estimated_cost >= 0);

alter table speaking_submissions
    add constraint chk_speaking_submissions_transcription_completed_at
        check (transcription_completed_at is null or transcription_started_at is null
            or transcription_completed_at >= transcription_started_at);

create index idx_speaking_submissions_audio_file on speaking_submissions (audio_file_id);
create index idx_speaking_submissions_transcription_status_created
    on speaking_submissions (transcription_status, created_at);
