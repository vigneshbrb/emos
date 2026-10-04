import { useEffect, useState } from 'react';
import { useParams } from 'react-router';
import { getJson, type OperationalCaseDetail } from '../app/api';
import { EvidencePanel } from './EvidencePanel';

export function CaseReviewPage({ detail }: { detail?: OperationalCaseDetail }) {
  const { caseId } = useParams(); const [loaded, setLoaded] = useState(detail); const [error, setError] = useState(false);
  useEffect(() => { if (!detail && caseId) getJson<OperationalCaseDetail>(`/api/cases/${caseId}`).then(setLoaded).catch(() => setError(true)); }, [detail, caseId]);
  if (error) return <p role="alert">Case data is unavailable.</p>;
  if (!loaded) return <p>Loading case…</p>;
  return <main><h1>Case {loaded.lifecycle.sourceId}</h1><section><h2>Lifecycle</h2><p>{loaded.lifecycle.status}</p><p>{loaded.lifecycle.runbook}</p></section>
    <EvidencePanel evidence={loaded.evidence}/><section aria-label="Recommendation"><h2>Recommendation</h2><p>No recommendation requested.</p></section>
    <section><h2>Source links</h2><ul>{loaded.sourceLinks.map(link => <li key={link}><a href={link}>{link}</a></li>)}</ul></section></main>;
}
