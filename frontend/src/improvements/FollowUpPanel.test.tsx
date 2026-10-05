import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { FollowUpPanel } from './FollowUpPanel';
describe('FollowUpPanel', () => {
  it('shows Jira status, freshness, review state, overdue attention and rationale requirement', () => {
    render(
      <FollowUpPanel
        followUp={{
          jiraKey: 'OPS-42',
          jiraStatus: "Won't Do",
          observedAt: '2026-10-04T10:00:00Z',
          stale: true,
          reviewDate: '2026-10-03',
          state: 'OPEN',
          overdue: true,
          finalRationaleRequired: true,
        }}
      />,
    );
    expect(screen.getByText('OPS-42')).toBeInTheDocument();
    expect(screen.getByText("Won't Do")).toBeInTheDocument();
    expect(screen.getByText('Source data is stale')).toBeInTheDocument();
    expect(screen.getByText('Review overdue')).toBeInTheDocument();
    expect(screen.getByText('Final rationale required')).toBeInTheDocument();
    expect(screen.getByText(/not available in this slice/i)).toBeInTheDocument();
  });
});
