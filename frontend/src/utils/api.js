import axios from 'axios';

export const getApiBaseUrl = () => {
  // If we are on ngrok or a similar tunnel, use relative path to trigger Vite proxy
  if (typeof window !== 'undefined' && window.location.hostname.endsWith('ngrok-free.dev')) {
    return '';
  }
  
  if (import.meta.env.VITE_API_BASE) {
    return import.meta.env.VITE_API_BASE;
  }
  
  if (typeof window !== 'undefined' && window.location.hostname.endsWith('netlify.app')) {
    return '/.netlify/functions/api-proxy';
  }
  
  return 'http://localhost:8080';
};

const apiClient = axios.create({
  baseURL: getApiBaseUrl(),
  withCredentials: true
});

apiClient.interceptors.request.use(config => {
  const userId = localStorage.getItem('userId');
  if (userId) {
    config.headers['X-User-Id'] = userId;
  }
  return config;
}, error => {
  return Promise.reject(error);
});

export default apiClient;
