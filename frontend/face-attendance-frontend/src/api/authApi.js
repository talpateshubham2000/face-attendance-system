import axiosClient from './axiosClient';

export const signup = async (email, password) => {
  const res = await axiosClient.post('/auth/signup', { email, password });
  return res.data; // AuthResponse { token, id, name, email, profileCompleted, faceRegistered }
};

export const login = async (email, password) => {
  const res = await axiosClient.post('/auth/login', { email, password });
  return res.data;
};

export const forgotPassword = async (email) => {
  const res = await axiosClient.post('/auth/forgot-password', { email });
  return res.data; // demo mode: the reset token itself, returned directly
};

export const resetPassword = async (token, newPassword) => {
  await axiosClient.post('/auth/reset-password', { token, newPassword });
};
