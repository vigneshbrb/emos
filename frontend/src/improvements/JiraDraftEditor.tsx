import { useState } from 'react';
import { useEffect } from 'react';
import { getJson, postJson } from '../app/api';
export type JiraDraftInput={title:string;problemStatement:string;proposedDirection:string;acceptanceIntent:string;reviewDate:string;approved:boolean};
export function JiraDraftEditor({recommendation,repository,suggestedReviewDate,initial,onSubmit}:{recommendation:string;repository:string;suggestedReviewDate:string;initial?:Partial<JiraDraftInput>;onSubmit:(draft:JiraDraftInput)=>Promise<void>}){
 const [draft,setDraft]=useState<JiraDraftInput>({title:initial?.title??'',problemStatement:initial?.problemStatement??'',proposedDirection:initial?.proposedDirection??recommendation,acceptanceIntent:initial?.acceptanceIntent??'',reviewDate:initial?.reviewDate??suggestedReviewDate,approved:initial?.approved??false});
 const [errors,setErrors]=useState<Record<string,string>>({}); const [pending,setPending]=useState(false);
 const field=(key:keyof JiraDraftInput)=>(event:React.ChangeEvent<HTMLInputElement|HTMLTextAreaElement>)=>setDraft({...draft,[key]:event.target.value});
 async function submit(){const next:Record<string,string>={};if(!draft.title.trim())next.title='Title is required.';if(!draft.problemStatement.trim())next.problemStatement='Problem statement is required.';if(!draft.proposedDirection.trim())next.proposedDirection='Proposed direction is required.';if(!draft.acceptanceIntent.trim())next.acceptanceIntent='Acceptance intent is required.';if(!draft.reviewDate)next.reviewDate='Review date is required.';if(!draft.approved)next.approved='Approval is required.';setErrors(next);if(Object.keys(next).length)return;setPending(true);await onSubmit(draft);}
 return <section><h2>Jira improvement draft</h2><p>Source recommendation</p><blockquote>{recommendation}</blockquote><p>Repository: <strong>{repository}</strong></p>
  <label htmlFor="jira-title">Title</label><input id="jira-title" aria-describedby={errors.title?'jira-title-error':undefined} value={draft.title} onChange={field('title')}/>{errors.title&&<span id="jira-title-error">{errors.title}</span>}
  <label htmlFor="jira-problem">Problem statement</label><textarea id="jira-problem" aria-describedby={errors.problemStatement?'jira-problem-error':undefined} value={draft.problemStatement} onChange={field('problemStatement')}/>{errors.problemStatement&&<span id="jira-problem-error">{errors.problemStatement}</span>}
  <label htmlFor="jira-direction">Proposed direction</label><textarea id="jira-direction" aria-describedby={errors.proposedDirection?'jira-direction-error':undefined} value={draft.proposedDirection} onChange={field('proposedDirection')}/>{errors.proposedDirection&&<span id="jira-direction-error">{errors.proposedDirection}</span>}
  <label htmlFor="jira-acceptance">Acceptance intent</label><textarea id="jira-acceptance" aria-describedby={errors.acceptanceIntent?'jira-acceptance-error':undefined} value={draft.acceptanceIntent} onChange={field('acceptanceIntent')}/>{errors.acceptanceIntent&&<span id="jira-acceptance-error">{errors.acceptanceIntent}</span>}
  <label htmlFor="jira-review">Review date</label><input id="jira-review" aria-describedby={errors.reviewDate?'jira-review-error':undefined} value={draft.reviewDate} onChange={field('reviewDate')}/>{errors.reviewDate&&<span id="jira-review-error">{errors.reviewDate}</span>}
  <label><input type="checkbox" checked={draft.approved} onChange={event=>setDraft({...draft,approved:event.target.checked})}/> I approve this Jira draft</label>
  {errors.approved&&!draft.approved&&<p>{errors.approved}</p>}
  <button type="button" disabled={pending} onClick={submit}>{pending?'Creating Jira improvement…':'Create Jira improvement'}</button>
 </section>;
}

type StoredDraft=JiraDraftInput&{id:string;repositoryId:string};
export function JiraDraftWorkflow({caseId,recommendation}:{caseId:string;recommendation:string}){
 const [stored,setStored]=useState<StoredDraft>();
 useEffect(()=>{getJson<StoredDraft>(`/api/cases/${caseId}/jira-draft`).then(setStored).catch(()=>setStored(undefined));},[caseId]);
 if(!stored)return null;
 return <JiraDraftEditor recommendation={recommendation} repository={stored.repositoryId} suggestedReviewDate={stored.reviewDate} initial={stored} onSubmit={async draft=>{
  const updated=await fetch(`/api/cases/${caseId}/jira-draft`,{method:'PUT',headers:{'Content-Type':'application/json'},body:JSON.stringify(draft)});if(!updated.ok)throw new Error('Draft update failed');
  await postJson(`/api/cases/${caseId}/dispositions/create-improvement`,{draftId:stored.id,attemptId:crypto.randomUUID(),reviewDate:draft.reviewDate});
 }}/>;
}
