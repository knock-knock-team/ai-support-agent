import axios from 'axios';

const apiBaseUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

export const api = axios.create({
  baseURL: apiBaseUrl,
  headers: {
    'Content-Type': 'application/json'
  }
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error?.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export const authApi = {
  checkSystemInitialized: () => api.get('/auth/system-initialized').then(res => res.data),
  register: (payload) => api.post('/auth/register', payload).then(res => res.data),
  login: (payload) => api.post('/auth/login', payload).then(res => res.data),
  changePassword: (payload) => api.post('/auth/change-password', payload).then(res => res.data),
  getMe: () => api.get('/auth/me').then(res => res.data)
};

export const operatorApi = {
  getRequests: () => api.get('/operator/requests').then(res => res.data),
  getPendingRequests: () => api.get('/operator/requests/pending').then(res => res.data),
  createRequest: (payload) => api.post('/operator/requests', payload).then(res => res.data),
  createMockRequest: () => api.post('/operator/requests/mock').then(res => res.data),
  getRequestById: (id) => api.get(`/operator/requests/${id}`).then(res => res.data),
  approve: (id) => api.post(`/operator/requests/${id}/approve`).then(res => res.data),
  updateRequest: (id, payload) => api.put(`/operator/requests/${id}`, payload).then(res => res.data),
  sendResponse: (id) => api.post(`/operator/requests/${id}/send`).then(res => res.data),
  getStatsByStatus: () => api.get('/operator/requests/stats/status').then(res => res.data),
  getStatsByCategory: () => api.get('/operator/requests/stats/category').then(res => res.data),
  getRecentRequests: (days = 7) => api.get(`/operator/requests/recent?days=${days}`).then(res => res.data)
};

export const adminApi = {
  getUsers: () => api.get('/admin/users').then(res => res.data),
  getUserById: (id) => api.get(`/admin/users/${id}`).then(res => res.data),
  createUser: (payload) => api.post('/admin/users', payload).then(res => res.data),
  updateUser: (id, payload) => api.put(`/admin/users/${id}`, payload).then(res => res.data),
  deleteUser: (id) => api.delete(`/admin/users/${id}`),
  toggleUserActive: (id) => api.put(`/admin/users/${id}/toggle-active`).then(res => res.data)
};
