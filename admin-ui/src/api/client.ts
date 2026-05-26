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

// ============ Reports API ============

export type ReportStatus = 'OPEN' | 'RESOLVED' | 'DISMISSED';

export interface ReportAdmin {
  id: string;
  recipeId: string;
  recipeTitle: string | null;
  recipeStatus: string | null;
  reporterId: string;
  reporterEmail: string | null;
  reason: string;
  status: ReportStatus;
  resolvedBy: string | null;
  resolvedAt: string | null;
  createdAt: string;
}

export async function listReports(onlyOpen = true): Promise<ReportAdmin[]> {
  const { data } = await api.get<ReportAdmin[]>('/admin/v1/reports', { params: { onlyOpen } });
  return data;
}

export async function resolveReport(id: string): Promise<void> {
  await api.post(`/admin/v1/reports/${id}/resolve`);
}

export async function dismissReport(id: string): Promise<void> {
  await api.post(`/admin/v1/reports/${id}/dismiss`);
}

export async function deleteRecipe(id: string): Promise<void> {
  await api.delete(`/admin/v1/recipes/${id}`);
}

// ============ Users API ============

export type UserStatus = 'PENDING_VERIFICATION' | 'ACTIVE' | 'BANNED' | 'DELETED';
export type UserRole = 'USER' | 'ADMIN';

export interface AdminUser {
  id: string;
  email: string;
  displayName: string;
  role: UserRole;
  status: UserStatus;
  emailVerifiedAt: string | null;
  lastLoginAt: string | null;
  createdAt: string;
}

export async function listUsers(): Promise<AdminUser[]> {
  const { data } = await api.get<AdminUser[]>('/admin/v1/users');
  return data;
}

export async function banUser(id: string): Promise<void> {
  await api.post(`/admin/v1/users/${id}/ban`);
}

export async function unbanUser(id: string): Promise<void> {
  await api.post(`/admin/v1/users/${id}/unban`);
}

export async function deleteUser(id: string): Promise<void> {
  await api.delete(`/admin/v1/users/${id}`);
}

// ============ Supplement Suggestions API (REQ-SUPP-004) ============

export type SupplementSuggestionStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface SupplementSuggestionAdmin {
  id: string;
  proposer_id: string;
  proposer_email: string | null;
  name_de: string;
  brand: string | null;
  unit_label: string;
  default_dose: number;
  kcal_per_dose: number | null;
  protein_per_dose: number | null;
  carbs_per_dose: number | null;
  fat_per_dose: number | null;
  micronutrients_json: string | null;
  notes: string | null;
  status: SupplementSuggestionStatus;
  reviewer_id: string | null;
  reviewed_at: string | null;
  review_note: string | null;
  public_id: string | null;
  created_at: string;
}

export async function listSupplementSuggestions(onlyPending = true): Promise<SupplementSuggestionAdmin[]> {
  const { data } = await api.get<SupplementSuggestionAdmin[]>(
    '/admin/v1/supplements/suggestions',
    { params: { onlyPending } },
  );
  return data;
}

export async function approveSupplementSuggestion(id: string): Promise<{ public_id: string }> {
  const { data } = await api.post<{ public_id: string }>(`/admin/v1/supplements/suggestions/${id}/approve`);
  return data;
}

export async function rejectSupplementSuggestion(id: string, note?: string): Promise<void> {
  await api.post(`/admin/v1/supplements/suggestions/${id}/reject`, { note: note ?? null });
}

// ============ Ingredient Queue & Field-PR API (P4.S1 — REQ-INGR-USER-001, REQ-FIELDPR-001..003) ============

export type IngredientReviewStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface IngredientQueueEntry {
  id: string;
  name_de: string;
  brand: string | null;
  barcode: string | null;
  submitted_by: string | null;
  submitter_email: string | null;
  status: IngredientReviewStatus;
  created_at: string;
}

export async function listIngredientQueue(): Promise<IngredientQueueEntry[]> {
  const { data } = await api.get<IngredientQueueEntry[]>('/admin/v1/ingredients/queue');
  return data;
}

export async function approveIngredient(id: string): Promise<void> {
  await api.post(`/admin/v1/ingredients/${id}/approve`);
}

export async function rejectIngredient(id: string, note?: string): Promise<void> {
  await api.post(`/admin/v1/ingredients/${id}/reject`, { note: note ?? null });
}

export interface FieldPrAdmin {
  id: string;
  ingredient_id: string;
  ingredient_name: string;
  proposer_id: string;
  proposer_email: string | null;
  field_name: string;
  old_value: string | null;
  new_value: string;
  rationale: string | null;
  status: IngredientReviewStatus;
  created_at: string;
  reviewed_at: string | null;
  review_note: string | null;
}

export async function listFieldPrs(onlyPending = true): Promise<FieldPrAdmin[]> {
  const { data } = await api.get<FieldPrAdmin[]>('/admin/v1/ingredients/field-prs', { params: { onlyPending } });
  return data;
}

export async function approveFieldPr(id: string): Promise<void> {
  await api.post(`/admin/v1/ingredients/field-prs/${id}/approve`);
}

export async function rejectFieldPr(id: string, note?: string): Promise<void> {
  await api.post(`/admin/v1/ingredients/field-prs/${id}/reject`, { note: note ?? null });
}

// ============ Admin Dashboard / Statistics / Audit API (P4.S5 — REQ-ADMIN-FULL-001 / REQ-AUDIT-001) ============

export interface AdminDashboard {
  user_count: number;
  recipe_count: number;
  ingredient_count: number;
  supplement_count: number;
  pending_ingredients: number;
  pending_field_prs: number;
  pending_supplements: number;
  open_recipe_reports: number;
}

export async function getAdminDashboard(): Promise<AdminDashboard> {
  const { data } = await api.get<AdminDashboard>('/admin/v1/stats/dashboard');
  return data;
}

export interface AdminStatistics {
  users: number;
  recipes: number;
  ingredients: number;
  supplements: number;
  approved_ingredients: number;
  rejected_ingredients: number;
  approved_supplements: number;
  rejected_supplements: number;
}

export async function getAdminStatistics(): Promise<AdminStatistics> {
  const { data } = await api.get<AdminStatistics>('/admin/v1/stats/statistics');
  return data;
}

export interface AuditLogEntry {
  id: number;
  occurred_at: string;
  actor_user_id: string | null;
  actor_kind: 'USER' | 'ADMIN' | 'SYSTEM';
  action: string;
  target_type: string | null;
  target_id: string | null;
  ip_address: string | null;
  detail: string | null;
}

export interface AuditQuery {
  actor?: string;
  action?: string;
  from?: string;
  to?: string;
  limit?: number;
}

export async function listAuditLog(query: AuditQuery = {}): Promise<AuditLogEntry[]> {
  const params: Record<string, string | number> = {};
  if (query.actor) params.actor = query.actor;
  if (query.action) params.action = query.action;
  if (query.from) params.from = query.from;
  if (query.to) params.to = query.to;
  if (query.limit) params.limit = query.limit;
  const { data } = await api.get<AuditLogEntry[]>('/admin/v1/audit', { params });
  return data;
}
