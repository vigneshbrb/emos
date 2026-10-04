create table disposition_obligation (
    id uuid primary key,
    expectation_key varchar(100) not null,
    policy_version integer not null,
    subject_case_id uuid not null,
    source_resolved_at timestamptz not null,
    deadline timestamptz not null,
    state varchar(20) not null check (state in ('PENDING', 'BREACHED', 'SATISFIED', 'CANCELLED')),
    breached_at timestamptz,
    cancelled_at timestamptz,
    unique (expectation_key, subject_case_id, source_resolved_at)
);

create unique index disposition_obligation_one_active_idx
    on disposition_obligation(expectation_key, subject_case_id)
    where state in ('PENDING', 'BREACHED');
create index disposition_obligation_due_idx on disposition_obligation(state, deadline);

create table attention_item (
    id uuid primary key,
    obligation_id uuid not null unique references disposition_obligation(id),
    subject_case_id uuid not null,
    reason varchar(300) not null,
    state varchar(20) not null check (state in ('OPEN', 'RESOLVED')),
    opened_at timestamptz not null,
    resolved_at timestamptz
);

create index attention_item_open_idx on attention_item(state, opened_at);
