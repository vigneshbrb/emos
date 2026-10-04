import { render, screen, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import { describe, expect, it } from 'vitest';
import { TodayPage } from './TodayPage';

describe('TodayPage', () => {
  it('separates attention, pending, and active signals without labeling active alerts as attention', () => {
    render(<MemoryRouter><TodayPage data={{
      attentionNow: [{ caseId: 'case-a', reason: 'Improvement decision missing', deadline: '2026-10-04T10:00:00Z', confirmedIncident: false, severity: 'P2', sourceUrl: 'https://jsm/a' }],
      pending: [{ caseId: 'case-p', deadline: '2026-10-06T10:00:00Z' }],
      activeSignals: [{ caseId: 'case-s', sourceId: 'alert-s', severity: 'P1', sourceUrl: 'https://jsm/s', updatedAt: '2026-10-04T11:00:00Z' }],
    }} /></MemoryRouter>);

    expect(screen.getByRole('heading', { name: 'Attention now (1)' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Pending follow-up (1)' })).toBeInTheDocument();
    const signals = screen.getByRole('region', { name: 'Active signals' });
    expect(within(signals).getByText('alert-s')).toBeInTheDocument();
    expect(within(signals).queryByText(/attention/i)).not.toBeInTheDocument();
    expect(screen.getByText(/Deadline:/)).toHaveTextContent('2026');
  });
});
