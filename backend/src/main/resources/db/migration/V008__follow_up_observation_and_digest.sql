alter table improvement_follow_up add column jira_status text;
alter table improvement_follow_up add column observed_at timestamptz;
alter table improvement_follow_up add column source_accessible boolean not null default true;
alter table improvement_follow_up add column final_rationale_required boolean not null default false;
create table notification_delivery(id uuid primary key,local_date date not null,recipient text not null,status text not null check(status in ('PENDING','SENT','FAILED')),attempted_at timestamptz not null,error_message text,unique(local_date,recipient));
