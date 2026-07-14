alter table study_plan_tasks
    add column skill varchar(32) not null default 'diagnostic';

alter table study_plan_tasks
    add column estimated_minutes integer not null default 30;

alter table study_plan_tasks
    add column recommended_resource_type varchar(32);

alter table study_plan_tasks
    add column recommended_resource_title varchar(180);

alter table study_plan_tasks
    add column rationale text;

alter table study_plan_tasks
    add constraint chk_study_plan_tasks_estimated_minutes check (estimated_minutes > 0);
