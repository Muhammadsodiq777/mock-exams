alter table ai_feedbacks
    add column rubric_version varchar(80);

alter table ai_feedbacks
    add column prompt_audit jsonb;

alter table ai_feedbacks
    add column validation_errors jsonb;

alter table ai_feedbacks
    add column retry_of_feedback_id bigint;

alter table ai_feedbacks
    add column retry_count integer not null default 0;

alter table ai_feedbacks
    add constraint fk_ai_feedbacks_retry_of
        foreign key (retry_of_feedback_id) references ai_feedbacks (id) on delete set null;

alter table ai_feedbacks
    add constraint chk_ai_feedbacks_retry_count check (retry_count >= 0);

create index idx_ai_feedbacks_writing_type_status
    on ai_feedbacks (writing_submission_id, feedback_type, status, created_at);

create index idx_ai_feedbacks_retry_of
    on ai_feedbacks (retry_of_feedback_id);
