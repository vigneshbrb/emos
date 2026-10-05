import { Link } from 'react-router';
import type { Pending } from '../app/api';

export function PendingList({ items }: { items: Pending[] }) {
  return (
    <section aria-label="Pending follow-up">
      <h2>Pending follow-up ({items.length})</h2>
      {items.length === 0 ? (
        <p>No pending follow-up.</p>
      ) : (
        <ul>
          {items.map((item) => (
            <li key={item.caseId}>
              <Link to={`/cases/${item.caseId}`}>Case {item.caseId}</Link>{' '}
              <span>Deadline: {new Date(item.deadline).toLocaleString()}</span>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
