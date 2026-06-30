import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  timeout: 12000,
})

api.interceptors.request.use((config) => {
  const user = readStoredUser()
  if (user?.id) {
    config.headers['X-User-Id'] = user.id
  }
  return config
})

export function readStoredUser() {
  try {
    return JSON.parse(localStorage.getItem('ith-user'))
  } catch {
    return null
  }
}

export const authApi = {
  login: (username) => api.post('/auth/mock-login', { username }).then((res) => res.data),
}

export const userApi = {
  list: () => api.get('/users').then((res) => res.data),
}

export const taskApi = {
  list: (params) => api.get('/tasks', { params }).then((res) => res.data),
  get: (id) => api.get(`/tasks/${id}`).then((res) => res.data),
  create: (payload) => api.post('/tasks', payload).then((res) => res.data),
  update: (id, payload) => api.put(`/tasks/${id}`, payload).then((res) => res.data),
  updateStatus: (id, status) => api.patch(`/tasks/${id}/status`, { status }).then((res) => res.data),
  remove: (id) => api.delete(`/tasks/${id}`),
}

export const dashboardApi = {
  summary: () => api.get('/dashboard/summary').then((res) => res.data),
}

export const newsApi = {
  list: (keyword) => api.get('/news', { params: { keyword: keyword || undefined } }).then((res) => res.data),
  related: (keyword) => api.get('/news/related', { params: { keyword } }).then((res) => res.data),
  refresh: (keyword) =>
    api.post('/news/refresh', null, { params: { keyword: keyword || undefined } }).then((res) => res.data),
}
