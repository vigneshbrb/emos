export type AttentionNow = { caseId: string; reason: string; deadline: string; confirmedIncident: boolean; severity: string; sourceUrl?: string };
export type Pending = { caseId: string; deadline: string };
export type ActiveSignal = { caseId: string; sourceId: string; severity: string; sourceUrl?: string; updatedAt: string };
export type TodayResponse = { attentionNow: AttentionNow[]; pending: Pending[]; activeSignals: ActiveSignal[] };

export type OperationalCaseDetail = {
  caseId: string;
  lifecycle: { sourceId: string; status: string; runbook?: string; triggeredAt?: string; resolvedAt?: string; updatedAt: string };
  evidence?: { durationSeconds?: number; severity?: string; recurrence: Record<'24h' | '7d' | '30d', number>; freshness: { observedAt: string; stale: boolean } };
  sourceLinks: string[];
  availableActions: string[];
  timeline: Array<{ eventType: string; actorType: string; occurredAt: string }>;
};

export type ApiError = { type: string; title: string; status: number; detail: string };

export async function getJson<T>(path: string): Promise<T> {
  const response = await fetch(path);
  if (!response.ok) throw (await response.json()) as ApiError;
  return response.json() as Promise<T>;
}
