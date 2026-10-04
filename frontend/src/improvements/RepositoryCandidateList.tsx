import { useEffect, useState } from 'react';
import { getJson, postJson } from '../app/api';

export type RepositoryCandidateView={repositoryId:string;authoritative:boolean;accessible:boolean;requiresConfirmation:boolean;reasons:string[];provenance:string};
export function RepositoryCandidateList({caseId,monitorId,candidates:given}:{caseId?:string;monitorId?:string;candidates?:RepositoryCandidateView[]}){
 const [candidates,setCandidates]=useState(given??[]); const [rationale,setRationale]=useState(''); const [confirmed,setConfirmed]=useState('');
 useEffect(()=>{if(!given&&caseId)getJson<RepositoryCandidateView[]>(`/api/cases/${caseId}/repository-candidates`).then(setCandidates).catch(()=>setCandidates([]));},[caseId,given]);
 return <section><h2>Repository candidates</h2>{confirmed&&<p>Confirmed {confirmed}</p>}{candidates.length===0?<p>No repository candidates found.</p>:<ul>{candidates.map(candidate=><li key={candidate.repositoryId}>
  <strong>{candidate.repositoryId}</strong>{candidate.authoritative&&<> — <span>Authoritative mapping</span></>}{!candidate.accessible&&<p>Repository inaccessible</p>}
  <ul>{candidate.reasons.map(reason=><li key={reason}>{reason}</li>)}</ul>
  {candidate.requiresConfirmation&&<><label>Confirmation rationale <input value={rationale} onChange={event=>setRationale(event.target.value)}/></label><button type="button" onClick={async()=>{if(!caseId||!monitorId||!rationale.trim())return;await postJson(`/api/cases/${caseId}/repository-confirmation`,{monitorId,repositoryId:candidate.repositoryId,rationale});setConfirmed(candidate.repositoryId);}}>Confirm repository</button></>}
 </li>)}</ul>}</section>;
}
