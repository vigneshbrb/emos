# EMOS MVP Design

**Status:** Approved conversational design, pending written-spec review  
**Date:** 2026-10-03  
**Product:** Engineering Management Operating System (EMOS)  
**Initial value stream:** Operational alert and incident follow-through

## 1. Purpose

EMOS is an expectation-monitoring and follow-through system for engineering management. It helps an Engineering Manager answer:

> Where does my team need attention today?

The system observes external facts and human decisions, compares them with explicit expectations, and surfaces breached obligations as actionable AttentionItems.

The operating loop is:

> Observe -> Understand -> Act -> Follow Up -> Learn

The first product hypothesis is:

> Within a four-week trial, EMOS becomes the manager's default place for incident follow-up and reduces associated manager time by at least 75 percent, without increasing missed follow-up obligations.

The current baseline is approximately six alerts and 120 minutes of manager effort per day. The target is approximately 30 minutes per day or less.

## 2. Initial User and Job to Be Done

### Persona

The MVP has one user: the Engineering Manager who is building and trialing EMOS with their current team.

Engineers, other managers, and administrators do not interact with EMOS in the MVP. Engineering work continues through JSM, Datadog, GitHub, Jira, and existing team communication channels.

### Primary job to be done

> When operational alerts occur and resolve, help me decide and retain what must happen next, so I do not repeatedly inspect systems, reconstruct context, locate repositories, create follow-up work, or remember when to revisit it.

### Success measurement

Success is measured through both:

1. EMOS-recorded active interaction time per operational case, excluding idle time.
2. A lightweight before-and-after weekly time study that includes work performed outside EMOS.

The four-week trial succeeds when average daily effort is approximately 30 minutes or less. As a completeness guardrail, every resolved OperationalCase must have exactly one pending, breached, satisfied, superseded, or cancelled Disposition Obligation; no resolved case may disappear from follow-through. The trial also records the proportion of cases dispositioned within the 24-working-hour expectation for comparison with the preceding four-week baseline when historical data permits that baseline to be reconstructed.

## 3. Product Boundary

### MVP capabilities

EMOS shall:

- Present active JSM alerts for awareness.
- Observe JSM alert and incident lifecycles through polling.
- Enrich alerts with Datadog severity, duration, and recurrence evidence.
- Show recurrence counts for fixed 24-hour, 7-day, and 30-day windows.
- Treat JSM as authoritative for lifecycle, incident confirmation, and incident grouping.
- Correlate JSM alerts to Datadog using a reliable source identifier or URL.
- Require one explicit disposition for every resolved operational case.
- Allow 24 working-clock hours for disposition before opening an AttentionItem.
- Pause that clock on Saturdays, Sundays, and explicitly configured India-region holidays.
- Offer AI-generated, non-binding recommendations for every resolved case.
- Recommend repositories from permitted GitHub metadata and documentation.
- Learn exact-monitor and logical service/component mappings only after manager confirmation.
- Draft Jira improvement work and create it only after manager approval.
- Track investigation and improvement follow-ups.
- Send one daily email digest of open AttentionItems.
- Retain evidence, recommendations, decisions, and history until manually deleted.
- Remain usable when AI is unavailable.

### Explicit non-goals

The MVP shall not:

- Replace JSM incident response or Datadog monitoring.
- Assign, notify, rank, or evaluate engineers.
- Model employee performance, capabilities, growth, SWOT, one-to-ones, or well-being.
- Provide employee scores, competitive leaderboards, or inferred accountability.
- Support multiple EMOS users, teams, organizations, or tenants.
- Provide a generic rule builder, workflow engine, or value-stream designer.
- Index or analyze complete repository source code.
- Autonomously select dispositions, confirm mappings, or create Jira work.
- Infer customer impact outside JSM incident confirmation.
- Use webhooks, a message broker, event sourcing, or distributed services.
- Cover customer support, CI/CD diagnosis, on-call scheduling, or rolling upgrades.
- Provide cloud deployment, SSO, or enterprise authorization.

## 4. Domain Language

### SourceEvent

An immutable fact obtained from an external system. It records the source system, source identifier, source occurrence time, ingestion time, payload provenance, and normalization status.

### Alert

An operational signal whose lifecycle is authoritative in JSM and whose technical evidence may be enriched from Datadog. An Alert is not an AttentionItem.

### Incident

A JSM-confirmed, customer-impacting operational case that may aggregate multiple Alerts. One Incident owns one disposition obligation. Its Alerts remain supporting evidence.

### OperationalCase

The common workflow subject for either:

- a confirmed Incident; or
- a standalone Alert that is not attached to an Incident.

This abstraction shares the follow-through workflow without erasing the distinction between Alerts and Incidents.

### Evidence

A provenance-preserving reference or snapshot used to understand a case or support a decision. Evidence includes lifecycle facts, recurrence counts, Datadog links, runbook content, JSM confirmation, repository clues, AI output, human rationale, and Jira state.

### Expectation

A named and versioned policy that defines what good looks like.

### Obligation

A concrete, time-bound instance of an Expectation for a subject. An Obligation is pending, satisfied, breached, superseded, or cancelled.

### AttentionItem

The manager-facing projection of a breached Obligation. Pending work is visible separately and is not an AttentionItem.

### Disposition

The manager's explicit decision for a resolved OperationalCase. The four allowed dispositions are:

- `CreateImprovement`
- `LinkExistingImprovement`
- `InvestigateFurther`
- `NoImprovementRequired`

### Rationale

A human-authored explanation that supports a decision. It is mandatory for `NoImprovementRequired` and for closing improvement work that Jira reports as cancelled, rejected, or "won't do."

### RepositoryMapping

A manager-confirmed association from an exact Datadog monitor or logical service/component to a GitHub repository. Exact-monitor mapping takes precedence over logical mapping.

### Recommendation

Non-authoritative AI output with cited evidence, uncertainty, provider and model metadata, generation time, and prompt version.

### Improvement

Jira work created or associated through an approved Disposition.

### FollowUp

A new Obligation created by `InvestigateFurther` or linked Improvement work. It has an explicit owner and/or review date.

### ActionRecord

An immutable audit record of an action performed by the manager or EMOS. The MVP does not introduce a generic mutable `Action` aggregate.

## 5. Bounded Contexts and Aggregates

The MVP is a modular monolith with three business bounded contexts and supporting adapters.

### 5.1 Operational Observation

Purpose: translate external JSM and Datadog models into EMOS operational facts.

Responsibilities:

- Poll JSM and Datadog.
- Store idempotent SourceEvents.
- Normalize vendor-specific payloads.
- Correlate JSM Alerts with Datadog monitors.
- Maintain Alert lifecycle and recurrence evidence.
- Group Alerts under a JSM-confirmed Incident.
- Expose normalized facts without leaking vendor types into other contexts.

Primary aggregate: `OperationalCase`.

Invariants:

- A standalone Alert is initially its own case.
- A confirmed Incident becomes the case for attached Alerts.
- A case has at most one effective Disposition.
- Raw vendor payloads are provenance records, not aggregate fields.
- JSM lifecycle facts take precedence over conflicting Datadog lifecycle observations.

### 5.2 Attention and Follow-Through

Purpose: compare expectations with observed facts and remember unmet obligations.

Responsibilities:

- Instantiate Obligations from named Expectations.
- Calculate and persist deadlines.
- Detect breaches idempotently.
- Project breached Obligations into AttentionItems.
- Record Dispositions and Rationales.
- Create investigation and improvement-review FollowUps.
- Satisfy Obligations when valid Evidence arrives.

Primary aggregate: `Obligation`.

Invariants:

- One Obligation produces at most one active AttentionItem.
- Viewing an AttentionItem does not satisfy it.
- Historical decisions are appended and never overwritten.
- Repeated evaluation cannot create duplicate Obligations or AttentionItems.
- An AttentionItem reflects Obligation state; it is not a second workflow source of truth.

### 5.3 Improvement Knowledge

Purpose: turn a manager decision into trustworthy, traceable improvement work.

Responsibilities:

- Rank repository candidates.
- Store confirmed RepositoryMappings.
- Generate Recommendations and Jira drafts.
- Require approval before Jira creation.
- Link existing Jira work.
- Observe Jira state.
- Create improvement-review and final-rationale Obligations.

Primary aggregates:

- `RepositoryMapping`
- `ImprovementFollowUp`

Invariants:

- AI output cannot create an authoritative mapping.
- Only manager confirmation creates or changes a RepositoryMapping.
- Jira creation requires an approved, editable draft.
- A Jira completion state can satisfy a FollowUp automatically.
- Cancellation, rejection, or "won't do" cannot satisfy a FollowUp without final Rationale.

### 5.4 Supporting ports and adapters

JSM, Datadog, GitHub, Jira, email, holiday calendar, and AI providers are replaceable adapters. Their schemas and SDK types stop at the application boundary.

## 6. Events

### Normalized source events

- `alert.detected`
- `alert.status_changed`
- `alert.resolved`
- `incident.confirmed`
- `alert.attached_to_incident`
- `jira.issue.status_changed`
- `repository.metadata_changed`

### Internal domain events

- `operational_case.resolved`
- `disposition.obligation_created`
- `obligation.breached`
- `attention_item.opened`
- `disposition.recorded`
- `repository_mapping.confirmed`
- `jira_draft.prepared`
- `jira_draft.approved`
- `improvement.linked`
- `investigation.followup_created`
- `improvement.review_due`
- `improvement.completed`
- `final_rationale.required`
- `obligation.satisfied`
- `attention_item.resolved`

Domain events enable in-process reactions and audit history. The MVP is not event sourced; aggregate state remains authoritative.

## 7. Expectations and Rules

The MVP uses explicit, versioned policies implemented as application configuration and domain code. It does not contain a generic rule language.

### 7.1 Disposition expectation

When an OperationalCase resolves, EMOS creates one Disposition Obligation due 24 working-clock hours after the source resolution time.

The working clock:

- runs continuously on configured working days;
- pauses on Saturdays and Sundays;
- pauses on explicitly configured India-region holiday dates; and
- uses the configured timezone, initially `Asia/Kolkata`.

The Obligation stores its policy version and calculated deadline. Later calendar changes do not silently rewrite existing deadlines.

The Obligation is satisfied as follows:

- `CreateImprovement`: a manager-approved Jira issue is created and linked, with a review date.
- `LinkExistingImprovement`: a valid Jira issue is linked, with a review date.
- `InvestigateFurther`: an owner and review date are recorded.
- `NoImprovementRequired`: a Rationale is recorded.

If a case reopens before a Disposition is recorded, its pending Disposition Obligation is cancelled. The next resolution creates a new Obligation and a fresh deadline. Earlier resolution and reopen facts remain Evidence.

### 7.2 Improvement follow-up expectation

When Improvement work is linked:

- EMOS suggests a review date seven calendar days after linking.
- The manager may change the date before confirmation.
- A configured Jira completion state satisfies the FollowUp automatically.
- If the work is incomplete at its review time, one AttentionItem opens.
- A Jira cancellation, rejection, or "won't do" state creates a final-rationale Obligation.
- A reopened completed Jira issue creates a new review Obligation.

### 7.3 Evaluation

Rules run:

- immediately after relevant domain events; and
- through periodic deadline and reconciliation sweeps.

Evaluation is idempotent. A repeated event or sweep cannot duplicate outcomes.

## 8. AttentionItem Lifecycle

An AttentionItem is opened only when an Obligation becomes breached.

Allowed terminal states are:

- `Resolved`: valid satisfaction Evidence arrived.
- `Superseded`: another Obligation now correctly represents the requirement.
- `Cancelled`: the underlying source fact was invalidated or explicitly withdrawn.

AttentionItems do not have generic task-management states such as "in progress" or "blocked." Those states belong to execution systems such as Jira. EMOS records whether its expectation is met.

Specific transitions:

- Recording `InvestigateFurther` satisfies the Disposition Obligation and creates a linked investigation FollowUp.
- Creating or linking Improvement work satisfies the Disposition Obligation and creates a linked improvement-review FollowUp.
- Completing the linked Jira issue satisfies the improvement FollowUp.
- Cancelled, rejected, or "won't do" Jira work requires final Rationale.
- If standalone Alerts are grouped into an Incident before their Dispositions, duplicate pending Obligations are superseded and Evidence is retained.
- If an already dispositioned Alert later joins an Incident, its decision remains Evidence and the Incident receives its own Disposition Obligation.

## 9. AI and Trust Boundary

### Deterministic application responsibilities

Only deterministic application logic may:

- Correlate known source identifiers.
- Calculate deadlines and recurrence counts.
- Create, transition, satisfy, supersede, or cancel Obligations.
- Open or resolve AttentionItems.
- Validate required Rationales and review dates.
- Apply confirmed RepositoryMappings.
- Create Jira issues after manager approval.
- Observe and interpret configured Jira workflow states.

### AI responsibilities

AI may:

- Summarize operational Evidence.
- Explain likely technical patterns.
- Recommend one of the four Dispositions.
- Rank repository candidates using permitted inputs.
- Draft an Improvement problem statement, Evidence summary, proposed direction, and acceptance intent.
- Identify uncertainty and missing Evidence.

AI output is always non-binding and clearly labeled as a Recommendation.

### Human authority

Only the manager may:

- Select a Disposition.
- Confirm or correct RepositoryMappings.
- Approve Jira creation and final ticket content.
- Accept `NoImprovementRequired`.
- Close cancelled, rejected, or "won't do" work with a final Rationale.
- Delete retained EMOS history.

### Provider and data handling

The initial preference is an approved OpenAI capability, behind a provider-neutral interface that permits later movement to Claude or another approved provider. The exact OpenAI API and model are deferred to implementation planning.

The approved enterprise AI provider may receive full Alert, runbook, and permitted repository-documentation content. Before submission, EMOS deterministically removes known secrets, credentials, authorization headers, tokens, and irrelevant personal data.

External Alerts, runbooks, Jira text, and repository documents are untrusted Evidence, not model instructions. The AI receives no shell, repository-write, Jira-write, or domain-transition capability. Responses must conform to a validated structured schema.

EMOS remains fully usable if AI is disabled, unavailable, or returns invalid output.

### Transparency and retention

Recommendations retain:

- cited Evidence references;
- provider and model metadata;
- generation time;
- prompt and schema version;
- uncertainty;
- generation state; and
- stale or superseded status.

The system retains source Evidence, prompts, Recommendations, manager decisions, corrections, and audit history locally until manually deleted. A correction does not rewrite the original Recommendation.

The MVP stores no private well-being or employee-reflection information. Repository contributors may appear as technical Evidence but never as inferred accountability or performance Evidence.

## 10. Repository Recommendation and Learning

The repository-selection sequence is:

1. Exact confirmed Datadog monitor mapping.
2. Confirmed logical service/component mapping.
3. Deterministic matches from repository name, description, topics, ownership files, READMEs, and deployment manifests.
4. AI-ranked candidates with cited clues.
5. Manual selection when confidence is insufficient.

The MVP does not inspect arbitrary source-code content.

Only manager confirmation creates reusable knowledge. A deleted or inaccessible mapped repository requires reconfirmation; EMOS does not silently select a replacement.

When exact-monitor and logical mappings conflict, the exact mapping is applied and the conflict is shown to the manager.

## 11. Integrations and Data Flow

### Inbound responsibilities

- **JSM:** Alert lifecycle, Incident confirmation, Incident grouping, resolution, and source links.
- **Datadog:** correlated monitor Evidence, duration, breach severity, and recurrence counts.
- **GitHub:** permitted repository metadata and documentation.
- **Jira:** issue lookup and lifecycle observation.

### Outbound responsibilities

- **Jira:** create only manager-approved drafts and use a stable EMOS correlation key.
- **Email:** send one daily digest.
- **AI provider:** produce structured, non-binding Recommendations.

### Local MVP flow

1. Schedulers poll JSM and Datadog every one to five minutes.
2. Ingestion deduplicates by source system and stable source identity.
3. JSM facts update Alerts and Incidents.
4. Datadog enriches cases with technical and recurrence Evidence.
5. Active Alerts appear in Active Signals without AttentionItems.
6. Resolution creates a Disposition Obligation.
7. EMOS assembles Evidence and asynchronously requests a Recommendation and repository candidates.
8. The manager reviews the case, confirms a repository, selects a Disposition, and approves any Jira work.
9. EMOS records the decision and creates the applicable FollowUp.
10. Jira polling satisfies completed work or exposes breached review and final-rationale Obligations.
11. A daily job emails open AttentionItems in dashboard priority order.

### Integration failure behavior

- Poll failures retain the last successful cursor and retry with backoff.
- Provider freshness and errors are visible.
- Normalization failures enter an inspectable ingestion-error queue.
- AI failure does not block human workflow.
- Jira creation uncertainty triggers correlation-key reconciliation before retry.
- Email failure is retried and remains diagnostically visible.
- Provider failure never changes an Obligation merely to hide an error.

## 12. Manager Experience

### Today dashboard

The dashboard presents information in this order:

1. **Attention now:** breached Obligations, ordered first by longest-overdue, then by JSM-confirmed Incident before standalone Alert, then by configured JSM severity, and finally by stable case identifier. The MVP does not calculate an opaque impact score.
2. **Pending decisions:** unresolved Obligations still within their deadline.
3. **Active signals:** active JSM Alerts for awareness only.
4. **System health:** source freshness and integration errors.

It shall not contain employee scoring, team competition, vanity charts, or an executive dashboard.

### Case review

The review workspace separates three columns or visual regions:

- **Evidence:** JSM lifecycle, Incident state, Datadog duration, severity, recurrence, runbook, and source links.
- **AI Recommendation:** proposed Disposition, repository candidates, reasons, uncertainty, and Evidence citations.
- **Human decision:** Disposition, repository confirmation, editable Jira draft, owner, review date, Rationale, and approval.

The manager's decision is never preselected. External links permit verification in source systems. Completed decisions collapse into an inspectable audit timeline.

### Digest

One daily email lists open AttentionItems using the same priority order as the dashboard and links to filtered EMOS views.

## 13. Technical Architecture

### Deployable shape

The MVP is one product repository containing:

- a Java Spring Boot REST backend;
- a React and TypeScript single-page frontend;
- PostgreSQL persistence; and
- local Docker Compose support.

The production frontend build is served by Spring Boot so the trial has one application endpoint. Development may use a separate frontend development server.

The application binds to localhost for the local trial. It has no EMOS login system. The operating system's access controls protect access. Company SSO and authorization are deployment-phase requirements.

### Backend modules

The backend uses package-by-feature modules with automated boundary verification:

- `operational-observation`
- `attention-followthrough`
- `improvement-knowledge`
- `recommendations`
- `integrations`
- `notifications`
- `platform`

Modules expose application commands, queries, and domain events. A module may not access another module's internal repositories or entities.

### Persistence

- Domain state uses relational tables and database constraints.
- Database migrations exist from the first implementation slice.
- JSON columns are limited to immutable source payloads, structured AI output, and provider metadata.
- Optimistic locking and unique constraints enforce idempotency.
- Append-only audit records accompany mutable projections.
- Integration secrets are not stored in application tables.

### Background work

A database-backed job table and in-process schedulers handle polling, enrichment, Recommendation generation, Jira reconciliation, deadline evaluation, and email delivery.

Workers claim jobs transactionally and retry safely. Periodic reconciliation repairs missed or uncertain work. The MVP does not require Kafka, RabbitMQ, or an external job service.

### Frontend organization

The frontend is organized around:

- Today dashboard;
- OperationalCase review;
- Repository confirmation;
- Jira draft approval;
- FollowUp review;
- audit and history; and
- settings and integration diagnostics.

The backend remains authoritative for domain rules and returns available actions and validation errors. The frontend does not reproduce lifecycle logic.

### Deliberately excluded infrastructure

The MVP does not introduce microservices, Kubernetes, GraphQL, event sourcing, a generic workflow engine, a rules DSL, a distributed cache, a search cluster, or a vector database.

## 14. First Vertical Slice

The first releasable slice is:

> Observe one JSM Alert through resolution, enrich it from Datadog, require a Disposition, assist repository selection, create approved Jira Improvement work, and remember the next FollowUp.

The implementation shall grow outward from this end-to-end path. It shall not build every integration or layer horizontally before demonstrating the complete flow.

Confirmed-Incident grouping and the remaining Dispositions are subsequent increments within the same MVP design.

## 15. Acceptance Criteria

### AC-1 Active awareness

Given an active JSM Alert, when polling imports it, then it appears under Active Signals with source freshness and no AttentionItem.

### AC-2 Idempotent ingestion

Given an already imported source record, when the same data is polled again, then no duplicate Alert, Evidence, Obligation, AttentionItem, or audit event is created.

### AC-3 Resolved standalone Alert

Given an Alert not attached to an Incident, when JSM marks it resolved, then one pending Disposition Obligation is created with a 24-working-hour deadline.

### AC-4 Confirmed Incident

Given several Alerts attached to one confirmed Incident, when the Incident resolves, then one Disposition Obligation exists for the Incident and all Alerts remain supporting Evidence.

### AC-5 Deadline breach

Given an unsatisfied Disposition Obligation, when its configured working-clock deadline passes, then exactly one open AttentionItem is created.

### AC-6 AI independence

Given AI generation fails, times out, or violates its response schema, then Evidence remains reviewable and all human Disposition actions remain available.

### AC-7 Repository confirmation

Given no confirmed mapping exists, when EMOS ranks repository candidates, then each candidate identifies its supporting Evidence and only manager confirmation creates a reusable mapping.

### AC-8 Disposition validation

- `CreateImprovement` requires a confirmed repository, approved Jira draft, and review date.
- `LinkExistingImprovement` requires a valid Jira issue and review date.
- `InvestigateFurther` requires an owner and review date.
- `NoImprovementRequired` requires a Rationale.

### AC-9 Safe Jira creation

Given an approved draft, when Jira creation is retried after an uncertain response, then correlation-key reconciliation prevents a duplicate Jira issue.

### AC-10 Follow-up tracking

Given linked Improvement work with a review date:

- when Jira reports completion, the FollowUp is satisfied automatically; and
- when the review time arrives while work is incomplete, exactly one AttentionItem opens.

### AC-11 Non-completion Rationale

Given Jira reports cancelled, rejected, or "won't do," then the FollowUp cannot close until the manager records final Rationale.

### AC-12 Daily digest

Given open AttentionItems exist, when the scheduled digest runs, then one email lists them in the same priority order as the dashboard.

### AC-13 Outcome measurement

EMOS records non-idle interaction time per OperationalCase and supports a four-week comparison with the 120-minute-per-day baseline. The target is approximately 30 minutes per day or less. No resolved OperationalCase may lack a corresponding Disposition Obligation or recorded Disposition. The trial also reports the percentage completed within 24 working hours and compares it with the preceding four-week period when historical reconstruction is possible.

## 16. Edge Cases

- Duplicate and out-of-order SourceEvents do not regress lifecycle state or duplicate outcomes.
- A reopened case cancels its pending Disposition Obligation; its next resolution starts a new deadline.
- A dispositioned Alert later attached to an Incident retains its decision as Evidence, while the Incident receives its own Disposition Obligation.
- A case discovered after its deadline opens its AttentionItem immediately using source occurrence time.
- Holiday edits do not silently change existing deadlines.
- Conflicting repository mappings are exposed.
- Deleted or inaccessible repositories require reconfirmation.
- Recommendations become stale when material Evidence changes.
- Human edits to a Jira draft do not alter the original Recommendation.
- Jira timeouts trigger reconciliation before retry.
- Deleted or inaccessible linked Jira work opens an AttentionItem requiring relinking or final Rationale.
- Jira status mappings are configurable per project.
- Digest failure does not change AttentionItem state.
- Manual deletion requires confirmation and records a deletion audit marker without claiming that external records were deleted.
- Empty or malformed external fields remain visible as data-quality errors; AI cannot invent them.

## 17. Verification Strategy

### Domain tests

- Use a fake clock and calendar.
- Verify Obligation transitions, Disposition validation, Incident grouping, and idempotency.
- Use property-style tests for weekends, holidays, repeated reopenings, and event ordering.

### Integration tests

- Run PostgreSQL with Testcontainers.
- Simulate JSM, Datadog, GitHub, Jira, email, and AI endpoints.
- Use sanitized, real-shaped adapter fixtures to verify normalization.

### Frontend tests

- Verify action availability, required inputs, stale Evidence, and visual separation between Evidence and Recommendation.
- Exercise the end-to-end browser flow from active Alert through Improvement completion.

### Reliability tests

- Restart during background jobs.
- Replay duplicate poll responses.
- Simulate provider timeouts and malformed responses.
- Simulate uncertain Jira creation.
- Replay captured SourceEvent streams and verify identical domain outcomes.

### AI evaluations

Use a curated incident set to evaluate:

- structured-schema compliance;
- Evidence citations;
- uncertainty;
- prompt-injection resistance;
- repository-ranking usefulness; and
- Jira-draft usefulness.

Tests shall not assert exact generated prose.

### Trial measurement

Run the local product for four weeks and compare EMOS telemetry plus weekly self-reported effort against the baseline. Each acceptance criterion maps to an automated test or explicit trial measurement.

## 18. Failure and Recovery Principles

- Source freshness is always visible.
- No provider failure silently satisfies or cancels an Obligation.
- Durable jobs survive application restart.
- Reconciliation repairs uncertain external writes.
- AI is optional and degradable.
- Raw payload normalization failures remain inspectable.
- Audit history distinguishes observed fact, AI Recommendation, manager decision, and automated action.

## 19. Future Capabilities

Future value streams require their own discovery, design, specification, and plan. They include:

- customer-support follow-through;
- CI/CD failure diagnosis;
- on-call coverage and scheduling;
- rolling-upgrade readiness;
- engineer capabilities, goals, SWOT, and one-to-ones;
- multi-manager and engineer-facing workflows;
- cloud deployment and enterprise identity;
- semantic cross-monitor grouping;
- configurable Expectations and value streams; and
- full-source-code analysis where justified.

The MVP architecture preserves ports and module boundaries that can support future work, but it does not pre-build these capabilities.

## 20. Dangerous and Unproven Ideas

The following require explicit future reconsideration and must not enter the MVP indirectly:

- employee scores or comparative leaderboards;
- using private reflection or well-being information for evaluation;
- treating commit ownership as service accountability;
- autonomous AI Dispositions;
- AI tools with direct write authority;
- hidden repository or owner inference;
- opaque operational or employee health scores;
- automatically turning every Alert into backlog work;
- broad source-code ingestion without proven need and security review; and
- moving EMOS into the active incident-response path.

## 21. Deferred Architectural Decisions

The following are intentionally deferred until implementation planning or deployment discovery:

- exact OpenAI API, model, and provider SDK;
- exact framework and dependency versions;
- build-tool choice;
- cloud provider and deployment topology;
- SSO and identity provider;
- webhook infrastructure;
- message-broker introduction criteria;
- multi-tenant partitioning;
- generic policy DSL;
- search or embedding infrastructure;
- long-term analytics storage; and
- mobile experience.

Deferral means the decision is not required to prove the MVP hypothesis. It does not permit implementation code to hard-code vendor behavior inside the domain.
