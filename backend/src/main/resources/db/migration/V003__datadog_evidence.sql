create table operational_evidence_snapshot (
    id uuid primary key,
    snapshot_key varchar(255) not null unique,
    case_id uuid not null references operational_alert(case_id),
    observed_at timestamptz not null,
    monitor_id varchar(100) not null,
    jsm_severity varchar(50),
    latest_duration_seconds bigint,
    recurrence_24h integer not null,
    recurrence_7d integer not null,
    recurrence_30d integer not null,
    provider_occurrence_ids jsonb not null,
    source_links jsonb not null
);

create index operational_evidence_case_idx on operational_evidence_snapshot(case_id, observed_at desc);
