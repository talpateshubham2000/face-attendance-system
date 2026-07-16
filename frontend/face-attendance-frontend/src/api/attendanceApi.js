import axiosClient from './axiosClient';

// The single call that recognizes the face AND marks attendance
export const markAttendance = async (image) => {
  const res = await axiosClient.post('/attendance/mark', { image });
  return res.data; // AttendanceResponse
};

export const getAttendanceByUser = async (userId) => {
  const res = await axiosClient.get(`/attendance/user/${userId}`);
  return res.data;
};
