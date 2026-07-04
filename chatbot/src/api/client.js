import axios from 'axios';

const API_URL = import.meta.env.VITE_API_URL || '';

const client = axios.create({
  baseURL: API_URL,
});

client.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

client.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export const authApi = {
  register: (name, email, password) =>
    client.post('/api/auth/register', { name, email, password }),
  login: (email, password) =>
    client.post('/api/auth/login', { email, password }),
};

export const chatApi = {
  listSessions: () => client.get('/api/chat/sessions'),
  createSession: (title) => client.post('/api/chat/sessions', { title }),
  getMessages: (sessionId) => client.get(`/api/chat/sessions/${sessionId}/messages`),
  sendMessage: (sessionId, message, attachments = []) => {
    const formData = new FormData();
    if (sessionId) {
      formData.append('sessionId', sessionId);
    }
    formData.append('message', message);
    attachments.forEach((attachment) => {
      formData.append('attachments', attachment);
    });

    return client.post('/api/chat/send', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
};

export default client;
