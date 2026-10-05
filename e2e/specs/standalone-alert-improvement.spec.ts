import { expect, test } from '@playwright/test';
import jsm from '../fixtures/jsm-alert.json';
import datadog from '../fixtures/datadog-monitor.json';
import recommendation from '../fixtures/ai-recommendation.json';
import repositories from '../fixtures/github-catalog.json';

test('standalone alert progresses from active signal to reconciled Jira improvement follow-up', async ({
  page,
}) => {
  let resolved = false;
  let breached = false;
  let createAttempts = 0;
  let jiraDone = false;
  const caseDetail = () => ({
    caseId: jsm.caseId,
    lifecycle: {
      sourceId: jsm.sourceId,
      status: resolved ? 'RESOLVED' : 'ACTIVE',
      runbook: jsm.runbook,
      triggeredAt: '2026-10-04T10:00:00Z',
      resolvedAt: resolved ? '2026-10-04T10:15:00Z' : null,
      updatedAt: '2026-10-04T10:20:00Z',
    },
    evidence: resolved
      ? { ...datadog, freshness: { observedAt: datadog.observedAt, stale: false } }
      : undefined,
    sourceLinks: [jsm.sourceUrl],
    availableActions: resolved ? ['RECORD_DISPOSITION'] : [],
    recommendation: resolved ? recommendation : undefined,
    timeline: resolved
      ? [
          {
            eventType: 'jsm.alert.resolved',
            actorType: 'SOURCE',
            occurredAt: '2026-10-04T10:15:00Z',
          },
          {
            eventType: 'recommendation.generated',
            actorType: 'AI',
            occurredAt: '2026-10-04T10:30:00Z',
          },
          {
            eventType: 'repository.confirmed',
            actorType: 'MANAGER',
            occurredAt: '2026-10-05T10:30:00Z',
          },
          {
            eventType: 'jira.improvement.created',
            actorType: 'AUTOMATION',
            occurredAt: '2026-10-05T10:35:00Z',
          },
        ]
      : [],
  });
  await page.route('**/api/interaction-sessions/**', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: route.request().url().endsWith('/start') ? '{"id":"session-1"}' : '{}',
    }),
  );
  await page.route('**/api/today', (route) =>
    route.fulfill({
      json: {
        attentionNow: breached
          ? [
              {
                caseId: jsm.caseId,
                reason: 'Improvement decision missing',
                deadline: '2026-10-05T10:15:00Z',
                confirmedIncident: false,
                severity: 'P2',
                sourceUrl: jsm.sourceUrl,
              },
            ]
          : [],
        pending:
          resolved && !breached ? [{ caseId: jsm.caseId, deadline: '2026-10-05T10:15:00Z' }] : [],
        activeSignals: resolved
          ? []
          : [
              {
                caseId: jsm.caseId,
                sourceId: jsm.sourceId,
                severity: 'P2',
                sourceUrl: jsm.sourceUrl,
                updatedAt: '2026-10-04T10:10:00Z',
              },
            ],
      },
    }),
  );
  await page.route(`**/api/cases/${jsm.caseId}`, (route) => route.fulfill({ json: caseDetail() }));
  await page.route(`**/api/cases/${jsm.caseId}/repository-candidates`, (route) =>
    route.fulfill({ json: repositories }),
  );
  await page.route(`**/api/cases/${jsm.caseId}/repository-confirmation`, (route) =>
    route.fulfill({ json: {} }),
  );
  await page.route(`**/api/cases/${jsm.caseId}/jira-draft`, async (route) => {
    if (route.request().method() === 'PUT') return route.fulfill({ json: {} });
    return route.fulfill({
      json: {
        id: '22222222-2222-4222-8222-222222222222',
        repositoryId: 'platform/request-service',
        title: '',
        problemStatement: '',
        proposedDirection: recommendation.proposedImprovement,
        acceptanceIntent: '',
        reviewDate: '2026-10-12',
        approved: false,
      },
    });
  });
  await page.route(`**/api/cases/${jsm.caseId}/dispositions/create-improvement`, (route) => {
    createAttempts += 1;
    if (createAttempts === 1) return route.abort('timedout');
    return route.fulfill({ json: { status: 'RECONCILED', jiraKey: 'PERF-123' } });
  });
  await page.route(`**/api/cases/${jsm.caseId}/follow-up`, (route) =>
    route.fulfill({
      json: {
        jiraKey: 'PERF-123',
        jiraStatus: jiraDone ? 'Done' : 'In Progress',
        observedAt: '2026-10-06T10:00:00Z',
        stale: false,
        reviewDate: '2026-10-12',
        state: jiraDone ? 'CLOSED' : 'OPEN',
        overdue: false,
        finalRationaleRequired: false,
      },
    }),
  );

  await page.goto('/');
  await expect(page.getByRole('heading', { name: 'Active Signals' })).toBeVisible();
  await expect(page.getByText(jsm.sourceId)).toBeVisible();
  resolved = true;
  breached = true;
  await page.reload();
  await expect(page.getByText('Improvement decision missing')).toBeVisible();
  await page.getByRole('link', { name: 'Improvement decision missing' }).click();
  await expect(page.getByText(recommendation.summary)).toBeVisible();
  await expect(page.getByText('No decision selected.')).toBeVisible();
  await page.getByLabel('Confirmation rationale').fill('This service owns the request path.');
  await page.getByRole('button', { name: 'Confirm repository' }).click();
  await expect(page.getByText('Confirmed platform/request-service')).toBeVisible();
  await page.getByLabel('Title').fill('Reduce repeated request latency');
  await page
    .getByLabel('Problem statement')
    .fill('A11 breached request latency three times in 24 hours.');
  await page.getByLabel('Acceptance intent').fill('No equivalent breach for seven days.');
  await page.getByLabel('I approve this Jira draft').check();
  await page.getByRole('button', { name: 'Create Jira improvement' }).click();
  await expect(page.getByRole('alert')).toContainText('uncertain');
  await page.getByRole('button', { name: 'Create Jira improvement' }).click();
  await expect(page.getByText('Jira improvement linked.')).toBeVisible();
  await page.reload();
  await expect(page.getByText('Jira:')).toBeVisible();
  await expect(page.getByText('PERF-123')).toBeVisible();
  jiraDone = true;
  await page.reload();
  await expect(page.getByText('Completion: CLOSED')).toBeVisible();
  for (const actor of ['SOURCE', 'AI', 'MANAGER', 'AUTOMATION'])
    await expect(page.getByText(actor, { exact: true })).toBeVisible();
  expect(createAttempts).toBe(2);
});
