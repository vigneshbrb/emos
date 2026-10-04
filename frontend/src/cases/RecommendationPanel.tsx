import type { RecommendationView } from '../app/api';

export function RecommendationPanel({ recommendation }: { recommendation?: RecommendationView }) {
  return <section aria-label="AI recommendation"><h2>AI recommendation</h2>
    {!recommendation ? <p>AI recommendations are disabled or not requested.</p> : recommendation.status !== 'READY' ?
      <p>Recommendation status: {recommendation.status}</p> : <>
        {recommendation.stale && <strong>STALE recommendation</strong>}
        <p>{recommendation.summary}</p><p>{recommendation.proposedImprovement}</p>
        <p>Uncertainty: {recommendation.uncertainty}</p>
        <ul>{recommendation.citations?.map(id => <li key={id}><a href={`#evidence-${id}`}>{id}</a></li>)}</ul>
      </>}
    <section aria-label="Human decision"><h2>Human decision</h2><p>No decision selected.</p></section>
  </section>;
}
