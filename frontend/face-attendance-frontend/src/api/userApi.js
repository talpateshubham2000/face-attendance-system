import axiosClient from './axiosClient';

export const getCurrentUser = async () => {
  const res = await axiosClient.get('/users/me');
  return res.data;
};

export const completeProfile = async ({ name, age, department }) => {
  const res = await axiosClient.put('/users/me', { name, age, department });
  return res.data;
};
