import { useEffect, useState } from 'react';
import { useParams } from 'react-router';
import { getJson, type OperationalCaseDetail } from '../app/api';
import { EvidencePanel } from './EvidencePanel';
import { RecommendationPanel } from './RecommendationPanel';
import { RepositoryCandidateList } from '../improvements/RepositoryCandidateList';
import { JiraDraftWorkflow } from '../improvements/JiraDraftEditor';
import { FollowUpWorkflow } from '../improvements/FollowUpPanel';
import { startInteractionTelemetry } from '../app/interactionTelemetry';

export function CaseReviewPage({ detail }: { detail?: OperationalCaseDetail }) {
  const { caseId } = useParams(); const [loaded, setLoaded] = useState(detail); const [error, setError] = useState(false);
  useEffect(() => { if (!detail && caseId) getJson<OperationalCaseDetail>(`/api/cases/${caseId}`).then(setLoaded).catch(() => setError(true)); }, [detail, caseId]);
  useEffect(() => {
    const observedCaseId = detail?.caseId ?? caseId;
    if (!observedCaseId) return;
    let stop: undefined | (() => Promise<void>); let cancelled = false;
    void startInteractionTelemetry(observedCaseId).then(value => { if (cancelled) void value(); else stop = value; }).catch(() => undefined);
    return () => { cancelled = true; if (stop) void stop(); };
  }, [detail?.caseId, caseId]);
  if (error) return <p role="alert">Case data is unavailable.</p>;
  if (!loaded) return <p>Loading case…</p>;
  return <main><h1>Case {loaded.lifecycle.sourceId}</h1><section><h2>Lifecycle</h2><p>{loaded.lifecycle.status}</p><p>{loaded.lifecycle.runbook}</p></section>
    <EvidencePanel evidence={loaded.evidence}/><RecommendationPanel recommendation={loaded.recommendation}/><RepositoryCandidateList caseId={loaded.caseId} monitorId={loaded.evidence?.monitorId}/>{loaded.recommendation?.proposedImprovement&&<JiraDraftWorkflow caseId={loaded.caseId} recommendation={loaded.recommendation.proposedImprovement}/>}<FollowUpWorkflow caseId={loaded.caseId}/>
    <section><h2>Source links</h2><ul>{loaded.sourceLinks.map(link => <li key={link}><a href={link}>{link}</a></li>)}</ul></section>
    <section><h2>Audit trail</h2><ul>{loaded.timeline.map((entry,index)=><li key={`${entry.occurredAt}-${index}`}><strong>{entry.actorType}</strong>: {entry.eventType}</li>)}</ul></section></main>;
}
