create table operational_source_event (
    id uuid primary key,
    source_system varchar(30) not null,
    source_id varchar(200) not null,
    source_updated_at timestamptz not null,
    ingested_at timestamptz not null,
    raw_payload jsonb not null,
    normalization_status varchar(20) not null check (normalization_status in ('RECEIVED', 'APPLIED', 'IGNORED')),
    unique (source_system, source_id, source_updated_at)
);

create table operational_alert (
    case_id uuid primary key,
    source_id varchar(200) not null unique,
    status varchar(20) not null check (status in ('ACTIVE', 'RESOLVED')),
    source_url varchar(1000),
    monitor_url varchar(1000),
    runbook text,
    severity varchar(20),
    triggered_at timestamptz,
    resolved_at timestamptz,
    source_updated_at timestamptz not null,
    domain_version bigint not null
);

create index operational_alert_status_idx on operational_alert (status, source_updated_at);

create table operational_jsm_poll_state (
    singleton boolean primary key default true check (singleton),
    cursor varchar(100),
    updated_since timestamptz not null
);

insert into operational_jsm_poll_state (singleton, cursor, updated_since)
values (true, null, '1970-01-01T00:00:00Z');
