import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { JiraDraftEditor } from './JiraDraftEditor';

describe('JiraDraftEditor', () => {
  it('keeps recommendation visible while requiring editable approved fields', async () => {
    const submit = vi.fn(() => new Promise<void>(() => {}));
    render(
      <JiraDraftEditor
        recommendation="Profile recurring query latency"
        repository="payments"
        suggestedReviewDate="2026-10-11"
        onSubmit={submit}
      />,
    );
    expect(
      screen.getByText('Profile recurring query latency', { selector: 'blockquote' }),
    ).toBeInTheDocument();
    expect(screen.getByText('payments')).toBeInTheDocument();
    for (const name of [
      'Title',
      'Problem statement',
      'Proposed direction',
      'Acceptance intent',
      'Review date',
    ])
      expect(screen.getByRole('textbox', { name })).toBeEnabled();
    fireEvent.click(screen.getByRole('button', { name: 'Create Jira improvement' }));
    expect(await screen.findByText('Approval is required.')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('checkbox', { name: 'I approve this Jira draft' }));
    fireEvent.change(screen.getByRole('textbox', { name: 'Title' }), {
      target: { value: 'Improve latency' },
    });
    fireEvent.change(screen.getByRole('textbox', { name: 'Problem statement' }), {
      target: { value: 'Recurring latency' },
    });
    fireEvent.change(screen.getByRole('textbox', { name: 'Acceptance intent' }), {
      target: { value: 'Latency remains stable' },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Create Jira improvement' }));
    expect(submit).toHaveBeenCalledTimes(1);
    expect(screen.getByRole('button', { name: 'Creating Jira improvement…' })).toBeDisabled();
  });
});
