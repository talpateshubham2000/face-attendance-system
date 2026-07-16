import axios from 'axios';

const BASE_URL = import.meta.env.VITE_API_URL;

const axiosClient = axios.create({
  baseURL: BASE_URL,
  headers: {
    'Content-Type': 'application/json'
  }
});

axiosClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

axiosClient.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error?.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
    }

    const message =
      error?.response?.data?.message ||
      error?.message ||
      'Something went wrong. Please try again.';

    return Promise.reject(new Error(message));
  }
);

export default axiosClient;
