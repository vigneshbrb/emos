import type { OperationalCaseDetail } from '../app/api';

export function EvidencePanel({ evidence }: { evidence?: OperationalCaseDetail['evidence'] }) {
  return (
    <section aria-label="Evidence">
      <h2>Evidence</h2>
      {!evidence ? (
        <p>No Datadog evidence available.</p>
      ) : (
        <>
          {evidence.freshness.stale && <strong>Stale provider data</strong>}
          <p>Severity: {evidence.severity ?? 'Unknown'}</p>
          <p>Duration: {evidence.durationSeconds ?? 'Unknown'} seconds</p>
          <ul>
            <li>24h: {evidence.recurrence['24h']}</li>
            <li>7d: {evidence.recurrence['7d']}</li>
            <li>30d: {evidence.recurrence['30d']}</li>
          </ul>
        </>
      )}
    </section>
  );
}
