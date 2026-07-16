import { beforeEach, describe, expect, it, vi } from 'vitest';
import { request } from './client';
import { getSession, saveSession } from './session';

// A minimal stand-in for a fetch Response (only what the client reads).
function jsonRes(status: number, body: unknown): Response {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: async () => body,
    text: async () => JSON.stringify(body),
  } as unknown as Response;
}

const session = {
  token: 'access1',
  refreshToken: 'refresh1',
  email: 'p@x.com',
  role: 'PATIENT' as const,
};

beforeEach(() => {
  localStorage.clear();
  vi.restoreAllMocks();
});

describe('request()', () => {
  it('attaches the Bearer token and returns parsed JSON', async () => {
    saveSession(session);
    const fetchMock = vi.fn().mockResolvedValue(jsonRes(200, [{ id: 1 }]));
    vi.stubGlobal('fetch', fetchMock);

    const result = await request<{ id: number }[]>('/api/services');

    expect(result).toEqual([{ id: 1 }]);
    const init = fetchMock.mock.calls[0][1];
    expect(init.headers.Authorization).toBe('Bearer access1');
  });

  it('throws an ApiError carrying the server message on non-2xx', async () => {
    saveSession(session);
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(jsonRes(409, { message: 'Slot is fully booked' })),
    );

    await expect(request('/api/appointments', { method: 'POST' })).rejects.toMatchObject({
      status: 409,
      message: 'Slot is fully booked',
    });
  });

  it('refreshes once on 401, retries, and persists the new session', async () => {
    saveSession(session);
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonRes(401, { message: 'expired' })) // original request
      .mockResolvedValueOnce(
        jsonRes(200, {
          token: 'access2',
          refreshToken: 'refresh2',
          email: 'p@x.com',
          role: 'PATIENT',
        }),
      ) // refresh
      .mockResolvedValueOnce(jsonRes(200, { ok: true })); // retried request
    vi.stubGlobal('fetch', fetchMock);

    const result = await request<{ ok: boolean }>('/api/appointments/mine');

    expect(result).toEqual({ ok: true });
    expect(fetchMock).toHaveBeenCalledTimes(3);
    expect(fetchMock.mock.calls[1][0]).toContain('/api/auth/refresh');
    expect(fetchMock.mock.calls[2][1].headers.Authorization).toBe('Bearer access2');
    expect(getSession()?.token).toBe('access2');
  });
});
