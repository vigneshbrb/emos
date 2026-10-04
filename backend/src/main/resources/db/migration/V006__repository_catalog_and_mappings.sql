create table repository_catalog (
 repository_id text primary key, description text not null, topics jsonb not null,
 permitted_documents jsonb not null, revision text not null, retrieved_at timestamptz not null, accessible boolean not null
);
create table repository_mapping_history (
 id uuid primary key, case_id uuid not null, monitor_id text not null, repository_id text not null,
 rationale text not null, confirmed_at timestamptz not null
);
create table repository_mapping_current (
 monitor_id text primary key, history_id uuid not null references repository_mapping_history(id),
 repository_id text not null, rationale text not null, confirmed_at timestamptz not null
);
