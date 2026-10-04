create table platform_job (
    id uuid primary key,
    type varchar(100) not null,
    deduplication_key varchar(300) not null,
    payload jsonb not null,
    state varchar(20) not null check (state in ('READY', 'RUNNING', 'SUCCEEDED')),
    available_at timestamptz not null,
    lease_until timestamptz,
    attempts integer not null default 0,
    last_error varchar(300),
    created_at timestamptz not null,
    completed_at timestamptz,
    unique (type, deduplication_key)
);

create index platform_job_available_idx
    on platform_job (state, available_at);

create table platform_audit_entry (
    id uuid primary key,
    subject_type varchar(100) not null,
    subject_id uuid not null,
    event_type varchar(150) not null,
    actor_type varchar(20) not null check (actor_type in ('SOURCE', 'AI', 'MANAGER', 'SYSTEM')),
    occurred_at timestamptz not null,
    details jsonb not null,
    evidence_ids jsonb not null,
    created_at timestamptz not null
);

create index platform_audit_subject_idx
    on platform_audit_entry (subject_type, subject_id, occurred_at, id);

create function reject_audit_mutation() returns trigger
language plpgsql as $$
begin
    raise exception 'audit entries are append-only';
end;
$$;

create trigger platform_audit_no_update_or_delete
    before update or delete on platform_audit_entry
    for each row execute function reject_audit_mutation();

create table platform_integration_status (
    provider varchar(100) primary key,
    last_success_at timestamptz,
    last_failure_at timestamptz,
    safe_message varchar(300)
);
