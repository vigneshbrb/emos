import { Link } from 'react-router';
import type { AttentionNow } from '../app/api';

export function AttentionList({ items }: { items: AttentionNow[] }) {
  return (
    <section aria-label="Attention now">
      <h2>Attention now ({items.length})</h2>
      {items.length === 0 ? (
        <p>Nothing needs attention.</p>
      ) : (
        <ul>
          {items.map((item) => (
            <li key={item.caseId}>
              <Link to={`/cases/${item.caseId}`}>{item.reason}</Link> <span>{item.severity}</span>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
