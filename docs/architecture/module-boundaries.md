# Module boundaries

EMOS is a modular monolith with event-driven behavior inside one deployable Spring Boot application.

| Module                   | Owns                                                                                  | Must not own                                     |
| ------------------------ | ------------------------------------------------------------------------------------- | ------------------------------------------------ |
| operational observation  | normalized JSM alert lifecycle and immutable Datadog evidence                         | attention policy or Jira workflow                |
| attention follow-through | expectations, disposition obligations, working deadlines, attention projection        | provider payload models                          |
| recommendations          | evidence-bound optional AI output and provenance                                      | decisions or employee judgments                  |
| improvement knowledge    | permitted repository catalog, candidates, manager-confirmed mapping                   | arbitrary source-code access                     |
| improvement delivery     | editable draft, explicit approval, idempotent Jira creation/reconciliation, follow-up | attention-rule evaluation                        |
| notifications            | one-manager digest and delivery idempotency                                           | workflow state                                   |
| platform                 | jobs, audit, diagnostics, privacy-limited interaction telemetry                       | operational domain identities or business policy |
| web                      | HTTP composition and DTOs                                                             | persistence or provider logic                    |

Provider adapters translate external schemas at the boundary. JSM, Datadog, GitHub, Jira, SMTP, and AI data models never become the domain model. Internal application events connect modules without requiring a broker; durable jobs cover polling, retries, and recovery. A service split is deferred until an independently scaled or isolated workload is demonstrated.

Audit actors remain explicit: `SOURCE` facts, `AI` recommendations, `MANAGER` decisions, and `AUTOMATION` actions. AI cannot select a disposition, confirm a repository, approve a Jira draft, or block the deterministic workflow.

Private reflection, well-being, employee scoring, competitive leaderboards, confirmed-incident grouping, generic rule authoring, and distributed infrastructure are outside this slice.
