import axios from 'axios';

// Spring Boot backend base URL
const BASE_URL = 'http://localhost:8080/api';

const axiosClient = axios.create({
  baseURL: BASE_URL,
  headers: {
    'Content-Type': 'application/json'
  }
});

// Attach the JWT (if we have one) to every outgoing request
axiosClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Every backend response is wrapped as { success, message, data }.
// This interceptor unwraps it so pages just get back `data` directly,
// and turns `success: false` / network errors into a normal thrown Error
// with a readable message.
axiosClient.interceptors.response.use(
  (response) => response.data,
  (error) => {
    // Token expired/invalid on a protected route -> send back to login
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
