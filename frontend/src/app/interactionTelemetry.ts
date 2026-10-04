type Fetcher = typeof fetch;

const post = (fetcher: Fetcher, path: string, body?: unknown) => fetcher(path, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: body === undefined ? undefined : JSON.stringify(body),
  keepalive: true,
});

export async function startInteractionTelemetry(caseId: string | undefined, fetcher: Fetcher = fetch, heartbeatMs = 60_000) {
  const response = await post(fetcher, '/api/interaction-sessions/start', { caseId: caseId ?? null });
  if (!response.ok) throw new Error('Unable to start interaction telemetry');
  const { id } = await response.json() as { id: string };
  const timer = window.setInterval(() => {
    void post(fetcher, `/api/interaction-sessions/${id}/heartbeat`, {
      visible: document.visibilityState === 'visible',
      active: document.hasFocus(),
    });
  }, heartbeatMs);
  return async () => {
    window.clearInterval(timer);
    await post(fetcher, `/api/interaction-sessions/${id}/stop`);
  };
}
