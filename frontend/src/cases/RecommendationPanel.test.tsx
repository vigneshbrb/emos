import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { RecommendationPanel } from './RecommendationPanel';

describe('RecommendationPanel', () => {
  it('separates AI advice from an unselected human decision and shows citations and uncertainty', () => {
    render(<RecommendationPanel recommendation={{ status: 'READY', recommendedDisposition: 'CREATE_IMPROVEMENT',
      summary: 'Recurring latency', proposedImprovement: 'Profile the query', repositorySearchTerms: ['latency'],
      citations: ['occ-1'], uncertainty: 'medium', generatedAt: '2026-10-04T12:00:00Z', model: 'model',
      promptVersion: 'v1', stale: false }} />);
    expect(screen.getByRole('heading', { name: 'AI recommendation' })).toBeInTheDocument();
    expect(screen.getByText('Uncertainty: medium')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'occ-1' })).toHaveAttribute('href', '#evidence-occ-1');
    expect(screen.getByRole('heading', { name: 'Human decision' })).toBeInTheDocument();
    expect(screen.getByText('No decision selected.')).toBeInTheDocument();
  });

  it.each(['PENDING', 'FAILED', 'STALE'] as const)('makes %s status visible', status => {
    render(<RecommendationPanel recommendation={{ status, stale: status === 'STALE' }} />);
    expect(screen.getByText(new RegExp(status, 'i'))).toBeInTheDocument();
  });
});
