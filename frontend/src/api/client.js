import axios from 'axios';

const TOKEN_KEY = 'tt_access';
const REFRESH_KEY = 'tt_refresh';
const USER_KEY = 'tt_user';

// Empty base URL = same origin (nginx / Vite proxy forward /api to the backend).
// Set VITE_API_URL when the frontend and backend are hosted on different domains.
export const api = axios.create({ baseURL: import.meta.env.VITE_API_URL || '' });

export const tokenStore = {
  get access() { return localStorage.getItem(TOKEN_KEY); },
  get refresh() { return localStorage.getItem(REFRESH_KEY); },
  get user() {
    try { return JSON.parse(localStorage.getItem(USER_KEY)); } catch { return null; }
  },
  save({ accessToken, refreshToken, user }) {
    localStorage.setItem(TOKEN_KEY, accessToken);
    localStorage.setItem(REFRESH_KEY, refreshToken);
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  },
  clear() {
    [TOKEN_KEY, REFRESH_KEY, USER_KEY].forEach((k) => localStorage.removeItem(k));
  },
};

api.interceptors.request.use((config) => {
  const token = tokenStore.access;
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// On a 401, transparently refresh the access token once and replay the request.
// Concurrent 401s share a single refresh call.
let refreshPromise = null;
let onSessionExpired = () => {};
export const setSessionExpiredHandler = (fn) => { onSessionExpired = fn; };

api.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config;
    const isAuthCall = original?.url?.startsWith('/api/auth/');
    if (error.response?.status === 401 && !original._retry && !isAuthCall && tokenStore.refresh) {
      original._retry = true;
      try {
        refreshPromise ??= axios
          .post(`${api.defaults.baseURL}/api/auth/refresh`, { refreshToken: tokenStore.refresh })
          .then((r) => tokenStore.save(r.data))
          .finally(() => { refreshPromise = null; });
        await refreshPromise;
        original.headers.Authorization = `Bearer ${tokenStore.access}`;
        return api(original);
      } catch (e) {
        tokenStore.clear();
        onSessionExpired();
        return Promise.reject(e);
      }
    }
    return Promise.reject(error);
  },
);

export function errorMessage(err, fallback = 'Something went wrong') {
  const data = err?.response?.data;
  if (data?.fieldErrors) {
    const [field, msg] = Object.entries(data.fieldErrors)[0];
    return `${field}: ${msg}`;
  }
  return data?.message || err?.message || fallback;
}
