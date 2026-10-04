import { afterEach, describe, expect, it, vi } from 'vitest';
import { startInteractionTelemetry } from './interactionTelemetry';

describe('interaction telemetry', () => {
  afterEach(() => vi.restoreAllMocks());

  it('starts a case session, heartbeats visibility and focus, and stops it', async () => {
    vi.useFakeTimers();
    const fetcher = vi.fn()
      .mockResolvedValueOnce({ ok: true, json: async () => ({ id: 'session-1' }) })
      .mockResolvedValue({ ok: true });
    const focused = vi.spyOn(document, 'hasFocus').mockReturnValue(true);

    const stop = await startInteractionTelemetry('case-1', fetcher as typeof fetch, 60_000);
    expect(fetcher).toHaveBeenCalledWith('/api/interaction-sessions/start', expect.objectContaining({ body: JSON.stringify({ caseId: 'case-1' }) }));

    await vi.advanceTimersByTimeAsync(60_000);
    expect(fetcher).toHaveBeenCalledWith('/api/interaction-sessions/session-1/heartbeat', expect.objectContaining({ body: JSON.stringify({ visible: true, active: true }) }));

    focused.mockReturnValue(false);
    await vi.advanceTimersByTimeAsync(60_000);
    expect(fetcher).toHaveBeenLastCalledWith('/api/interaction-sessions/session-1/heartbeat', expect.objectContaining({ body: JSON.stringify({ visible: true, active: false }) }));

    await stop();
    expect(fetcher).toHaveBeenLastCalledWith('/api/interaction-sessions/session-1/stop', expect.objectContaining({ method: 'POST' }));
    vi.useRealTimers();
  });
});
