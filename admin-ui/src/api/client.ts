import axios, { type AxiosError, type AxiosInstance } from 'axios';

const ACCESS_KEY = 'hf_admin_access';
const REFRESH_KEY = 'hf_admin_refresh';

export const tokens = {
  get access() { return localStorage.getItem(ACCESS_KEY); },
  get refresh() { return localStorage.getItem(REFRESH_KEY); },
  set(access: string, refresh: string) {
    localStorage.setItem(ACCESS_KEY, access);
    localStorage.setItem(REFRESH_KEY, refresh);
  },
  clear() {
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
  },
  isLoggedIn(): boolean { return !!localStorage.getItem(ACCESS_KEY); },
};

export const api: AxiosInstance = axios.create({
  baseURL: import.meta.env.PROD ? 'https://api.healthforge.endgear.de' : '',
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use((config) => {
  const t = tokens.access;
  if (t) config.headers.Authorization = `Bearer ${t}`;
  return config;
});

let refreshing: Promise<string> | null = null;

api.interceptors.response.use(
  (r) => r,
  async (err: AxiosError) => {
    const original = err.config as (typeof err.config & { _retry?: boolean }) | undefined;
    if (err.response?.status === 401 && original && !original._retry && tokens.refresh) {
      original._retry = true;
      try {
        if (!refreshing) {
          refreshing = axios
            .post(`${api.defaults.baseURL}/v1/auth/refresh`, { refreshToken: tokens.refresh })
            .then((res) => {
              tokens.set(res.data.accessToken, res.data.refreshToken);
              return res.data.accessToken as string;
            })
            .finally(() => { refreshing = null; });
        }
        const newAccess = await refreshing;
        if (original.headers) original.headers.Authorization = `Bearer ${newAccess}`;
        return api.request(original);
      } catch {
        tokens.clear();
        window.location.href = '/login';
      }
    }
    return Promise.reject(err);
  },
);

// ============ Auth API ============

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  expiresInSeconds: number;
  user: { id: string; email: string; displayName: string; role: 'USER' | 'ADMIN' };
}

export async function login(email: string, password: string): Promise<LoginResponse> {
  const { data } = await api.post<LoginResponse>('/v1/auth/login', { email, password });
  tokens.set(data.accessToken, data.refreshToken);
  return data;
}

export async function logout(): Promise<void> {
  if (tokens.refresh) {
    try { await api.post('/v1/auth/logout', { refreshToken: tokens.refresh }); } catch { /* noop */ }
  }
  tokens.clear();
}

// ============ Invite API ============

export interface Invite {
  id: string;
  code: string;
  note: string | null;
  createdBy: string | null;
  usedBy: string | null;
  usedAt: string | null;
  expiresAt: string;
  createdAt: string;
}

export async function listInvites(): Promise<Invite[]> {
  const { data } = await api.get<Invite[]>('/admin/v1/invites');
  return data;
}

export async function createInvite(note: string | null, validDays: number): Promise<Invite> {
  const { data } = await api.post<Invite>('/admin/v1/invites', { note, validDays });
  return data;
}
