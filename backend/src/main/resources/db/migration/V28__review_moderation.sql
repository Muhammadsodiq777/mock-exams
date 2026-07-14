alter table reviews
    add column updated_at timestamp with time zone not null default current_timestamp;

update reviews
set updated_at = created_at;

alter table reviews
    add column hidden_at timestamp with time zone;

alter table reviews
    add column hidden_by_user_id bigint;

alter table reviews
    add constraint fk_reviews_hidden_by_user
    foreign key (hidden_by_user_id) references users (id) on delete set null;

create index idx_reviews_student_created on reviews (student_user_id, created_at);
create index idx_reviews_hidden_created on reviews (hidden_at, created_at);
