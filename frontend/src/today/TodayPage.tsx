import { useEffect, useState } from 'react';
import { getJson, type TodayResponse } from '../app/api';
import { AttentionList } from './AttentionList';
import { PendingList } from './PendingList';
import { ActiveSignalsList } from './ActiveSignalsList';

export function TodayPage({ data }: { data?: TodayResponse }) {
  const [loaded, setLoaded] = useState<TodayResponse | undefined>(data);
  const [error, setError] = useState(false);
  useEffect(() => { if (!data) getJson<TodayResponse>('/api/today').then(setLoaded).catch(() => setError(true)); }, [data]);
  if (error) return <p role="alert">Provider data is unavailable. Try again.</p>;
  if (!loaded) return <p>Loading today’s attention…</p>;
  return <main><AttentionList items={loaded.attentionNow}/><PendingList items={loaded.pending}/><ActiveSignalsList items={loaded.activeSignals}/></main>;
}
