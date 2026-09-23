import { api } from './client';

const data = (p) => p.then((r) => r.data);

export const authApi = {
  login: (body) => data(api.post('/api/auth/login', body)),
  register: (body) => data(api.post('/api/auth/register', body)),
  logout: (refreshToken) => data(api.post('/api/auth/logout', { refreshToken })),
  me: () => data(api.get('/api/auth/me')),
};

export const jobsApi = {
  search: (params) => data(api.get('/api/jobs', { params })),
  get: (id) => data(api.get(`/api/jobs/${id}`)),
};

export const candidateApi = {
  dashboard: () => data(api.get('/api/candidate/dashboard')),
  profile: () => data(api.get('/api/candidate/profile')),
  updateProfile: (body) => data(api.put('/api/candidate/profile', body)),
  uploadResume: (file) => {
    const form = new FormData();
    form.append('file', file);
    return data(api.post('/api/candidate/resume', form));
  },
  resumeBlob: () => data(api.get('/api/candidate/resume', { responseType: 'blob' })),
  apply: (jobId, coverLetter) => data(api.post(`/api/candidate/jobs/${jobId}/apply`, { coverLetter })),
  applications: () => data(api.get('/api/candidate/applications')),
  withdraw: (id) => data(api.delete(`/api/candidate/applications/${id}`)),
  recommendations: (limit = 6) => data(api.get('/api/candidate/recommendations', { params: { limit } })),
  atsCheck: (body) => data(api.post('/api/candidate/ats-check', body)),
};

export const recruiterApi = {
  dashboard: () => data(api.get('/api/recruiter/dashboard')),
  jobs: () => data(api.get('/api/recruiter/jobs')),
  createJob: (body) => data(api.post('/api/recruiter/jobs', body)),
  updateJob: (id, body) => data(api.put(`/api/recruiter/jobs/${id}`, body)),
  setJobStatus: (id, status) => data(api.patch(`/api/recruiter/jobs/${id}/status`, { status })),
  deleteJob: (id) => data(api.delete(`/api/recruiter/jobs/${id}`)),
  board: (jobId, params) => data(api.get(`/api/recruiter/jobs/${jobId}/board`, { params })),
  moveStage: (appId, body) => data(api.patch(`/api/recruiter/applications/${appId}/stage`, body)),
  application: (appId) => data(api.get(`/api/recruiter/applications/${appId}`)),
  addNote: (appId, message) => data(api.post(`/api/recruiter/applications/${appId}/notes`, { message })),
  rate: (appId, rating) => data(api.patch(`/api/recruiter/applications/${appId}/rating`, { rating })),
  scheduleInterview: (appId, body) => data(api.post(`/api/recruiter/applications/${appId}/interviews`, body)),
  resumeBlob: (appId) => data(api.get(`/api/recruiter/applications/${appId}/resume`, { responseType: 'blob' })),
  interviews: () => data(api.get('/api/recruiter/interviews')),
};

export const notificationApi = {
  list: () => data(api.get('/api/notifications')),
  unreadCount: () => data(api.get('/api/notifications/unread-count')),
  markRead: (id) => data(api.patch(`/api/notifications/${id}/read`)),
  markAllRead: () => data(api.patch('/api/notifications/read-all')),
};

export const adminApi = {
  dashboard: () => data(api.get('/api/admin/dashboard')),
  users: (params) => data(api.get('/api/admin/users', { params })),
  setUserStatus: (id, enabled) => data(api.patch(`/api/admin/users/${id}/status`, { enabled })),
  jobs: (params) => data(api.get('/api/admin/jobs', { params })),
  deleteJob: (id) => data(api.delete(`/api/admin/jobs/${id}`)),
};

export function openBlob(blob) {
  const url = URL.createObjectURL(blob);
  window.open(url, '_blank', 'noopener');
  setTimeout(() => URL.revokeObjectURL(url), 60_000);
}
