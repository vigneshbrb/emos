create table interaction_session(id uuid primary key,case_id uuid,started_at timestamptz not null,last_heartbeat_at timestamptz not null,last_visible_active boolean not null,stopped_at timestamptz,active_seconds bigint not null default 0);
create table interaction_heartbeat(session_id uuid not null references interaction_session(id),occurred_at timestamptz not null,visible_active boolean not null,primary key(session_id,occurred_at));
alter table disposition_obligation add column satisfied_at timestamptz;
