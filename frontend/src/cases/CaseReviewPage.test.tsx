import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { CaseReviewPage } from './CaseReviewPage';

describe('CaseReviewPage', () => {
  it('labels recurrence and stale evidence separately from recommendation space', () => {
    render(<CaseReviewPage detail={{
      caseId: 'case-a',
      lifecycle: { sourceId: 'alert-42', status: 'RESOLVED', runbook: 'Inspect latency', triggeredAt: '2026-10-04T10:00:00Z', resolvedAt: '2026-10-04T10:30:00Z', updatedAt: '2026-10-04T10:30:00Z' },
      evidence: { durationSeconds: 600, severity: 'P2', recurrence: { '24h': 3, '7d': 7, '30d': 12 }, freshness: { observedAt: '2026-10-04T10:30:00Z', stale: true } },
      sourceLinks: ['https://jsm/42'], availableActions: ['RECORD_DISPOSITION'], timeline: [],
    }} />);

    expect(screen.getByRole('heading', { name: 'Evidence' })).toBeInTheDocument();
    expect(screen.getByText('Stale provider data')).toBeInTheDocument();
    expect(screen.getByText('24h: 3')).toBeInTheDocument();
    expect(screen.getByText('7d: 7')).toBeInTheDocument();
    expect(screen.getByText('30d: 12')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Recommendation' })).toBeInTheDocument();
    expect(screen.getByText('No recommendation requested.')).toBeInTheDocument();
  });
});
