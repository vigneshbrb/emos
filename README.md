# EMOS — Engineering Management Operating System

EMOS answers one question for a manager: **Where does my team need attention today?** This first vertical slice observes standalone JSM alerts, enriches resolved alerts with Datadog evidence, enforces a 24-working-hour improvement-disposition expectation, and supports a manager-approved Jira improvement through follow-up.

This is a local-first trial build. It does not group confirmed incidents, support the other three dispositions, or assess employees.

## Prerequisites

- Java 21
- Docker with Compose
- Node.js 22 and npm

## Run locally

```bash
cp .env.example .env
docker compose up -d postgres
set -a; source .env; set +a
./mvnw -pl backend spring-boot:run
```

In a second terminal:

```bash
npm --prefix frontend ci
npm --prefix frontend run dev
```

Open `http://localhost:5173`. The backend listens on `127.0.0.1:8080`; Vite proxies `/api` during development. Flyway applies all database changes automatically—never edit the database schema by hand.

## Provider configuration

Copy `.env.example` and populate only the integrations being enabled. Secrets must stay in `.env` or the process environment; `.env` is ignored by Git. Use least-privilege credentials:

- JSM: read alerts and their lifecycle/runbook data. Polling defaults to `PT2M` (`EMOS_JSM_POLL_INTERVAL`).
- Datadog: read monitors and alert history.
- GitHub: read repository metadata only within `EMOS_GITHUB_ORGANIZATION`.
- Jira: read/create issues in the configured default project. Jira completion status is currently the provider status `Done`; validate this mapping during setup.
- OpenAI/Codex: optional. With `EMOS_RECOMMENDATIONS_ENABLED=false`, the deterministic workflow remains usable.
- Email: optional SMTP delivery to the one manager address. The digest job is seeded daily at 00:00 UTC.

`EMOS_CALENDAR_ZONE` defaults to `Asia/Kolkata`. `EMOS_CALENDAR_HOLIDAYS` is a comma-separated list of ISO dates; weekends and configured holidays do not consume the 24-working-hour disposition window. The initial product decision was to exclude holidays from elapsed time; leave the list blank only if that is intentionally acceptable for the trial.

Interaction telemetry uses a 60-second browser heartbeat and 120-second idle cutoff. It records session/case identifiers, timestamps, visibility/focus, and computed active seconds—never keystrokes, page content, mood, or employee activity.

## Verification and fixture replay

```bash
docker compose config
./mvnw verify
npm --prefix frontend ci
npm --prefix frontend test -- --run
npm --prefix frontend run build
npm --prefix e2e ci
npx --prefix e2e playwright install chromium
npm --prefix e2e test
```

The Playwright replay uses deterministic files under `e2e/fixtures` and exercises the active-alert, deadline breach, recommendation, repository confirmation, approved Jira draft, uncertain-write reconciliation, follow-up, completion, and audit presentation path. It never calls production providers.

## Operations and data

- Check `/actuator/health` and `/api/system/diagnostics` for application/provider freshness.
- Provider polling and outbound operations are durable jobs; restarts retry expired leases.
- PostgreSQL data lives in the `emos-postgres-data` Docker volume. Back it up with your normal PostgreSQL tooling before upgrades.
- Evidence, audit, and trial telemetry are retained indefinitely until manually deleted. Deletion is an operator database action in this MVP; back up first and record what was removed.
- Stop application processes with `Ctrl-C`. `docker compose stop postgres` preserves the volume. `docker compose down -v` permanently deletes local EMOS data and should only be used intentionally.

Use [the four-week worksheet](docs/trial/first-four-weeks.md) to decide whether the slice meets the target: reduce manager incident follow-up time from 120 to 30 minutes per day.
