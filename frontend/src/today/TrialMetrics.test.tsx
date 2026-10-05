import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { TrialMetrics } from './TrialMetrics';
describe('TrialMetrics', () => {
  it('shows manager workflow outcomes without employee metrics', () => {
    render(
      <TrialMetrics
        metrics={{
          baselineMinutesPerDay: 120,
          targetMinutesPerDay: 30,
          actualAverageMinutesPerDay: 28,
          resolvedCases: 20,
          resolvedCasesWithDisposition: 18,
          dispositionCoveragePercent: 90,
          withinWorkingHoursPercent: 80,
        }}
      />,
    );
    for (const text of [
      '120 minutes/day',
      '30 minutes/day',
      '28 minutes/day',
      '18 of 20',
      '90%',
      '80%',
    ])
      expect(screen.getByText(text)).toBeInTheDocument();
    expect(screen.queryByText(/employee/i)).not.toBeInTheDocument();
  });
});
