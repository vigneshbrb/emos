create table recommendation (
    id uuid primary key,
    deduplication_key varchar(300) not null unique,
    case_id uuid not null,
    evidence_version varchar(100) not null,
    prompt_version varchar(100) not null,
    status varchar(20) not null check (status in ('PENDING','READY','FAILED')),
    structured_output jsonb,
    requested_at timestamptz not null,
    generated_at timestamptz,
    failure_message varchar(300)
);
create index recommendation_case_idx on recommendation(case_id, requested_at desc);
