import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { RepositoryCandidateList } from './RepositoryCandidateList';

describe('RepositoryCandidateList', () => {
 it('shows evidence reasons, authoritative and inaccessible states, and explicit confirmation', () => {
  render(<RepositoryCandidateList candidates={[
   {repositoryId:'payments',authoritative:true,accessible:true,requiresConfirmation:false,reasons:['Confirmed mapping for monitor monitor-17'],provenance:'MANAGER_CONFIRMATION'},
   {repositoryId:'legacy',authoritative:false,accessible:false,requiresConfirmation:true,reasons:['Metadata match: sql'],provenance:'METADATA'}
  ]}/>);
  expect(screen.getByText('Authoritative mapping')).toBeInTheDocument();
  expect(screen.getByText('Repository inaccessible')).toBeInTheDocument();
  expect(screen.getByText('Metadata match: sql')).toBeInTheDocument();
  expect(screen.getAllByRole('button',{name:'Confirm repository'})).toHaveLength(1);
  expect(screen.queryByText(/committer|employee/i)).not.toBeInTheDocument();
 });
});
