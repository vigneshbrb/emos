import { Link } from 'react-router';
import type { ActiveSignal } from '../app/api';

export function ActiveSignalsList({ items }: { items: ActiveSignal[] }) {
  return <section aria-label="Active signals"><h2>Active signals ({items.length})</h2>
    {items.length === 0 ? <p>No active alerts.</p> : <ul>{items.map(item =>
      <li key={item.caseId}><Link to={`/cases/${item.caseId}`}>{item.sourceId}</Link> <span>{item.severity}</span></li>)}</ul>}
  </section>;
}
