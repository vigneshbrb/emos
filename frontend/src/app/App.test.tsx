import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import { describe, expect, it } from 'vitest';

import { App } from './App';

describe('App', () => {
  it('shows the manager question and primary navigation', () => {
    render(
      <MemoryRouter>
        <App />
      </MemoryRouter>,
    );

    expect(
      screen.getByRole('heading', {
        name: 'Where does my team need attention today?',
      }),
    ).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Today' })).toBeInTheDocument();
    expect(
      screen.getByRole('link', { name: 'System health' }),
    ).toBeInTheDocument();
  });
});
