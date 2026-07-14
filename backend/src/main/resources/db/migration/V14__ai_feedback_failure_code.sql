alter table ai_feedbacks
    add column failure_code varchar(64);

create index idx_ai_feedbacks_failure_code_created_at on ai_feedbacks (failure_code, created_at);
