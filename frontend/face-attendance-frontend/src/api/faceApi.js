import axiosClient from './axiosClient';

// Called once, after all 6 liveness-verified photos are captured
export const registerFace = async (images) => {
  const res = await axiosClient.post('/face/register', { images });
  return res.data;
};

// Called continuously by the live kiosk screen to drive the green/red frame
export const checkFace = async (image) => {
  const res = await axiosClient.post('/face/check', { image });
  return res.data; // { status: 'GOOD' | 'ADJUST', message }
};

// Called continuously during guided face registration - checks a SPECIFIC
// live action (blink / turn / smile / hold still)
export const checkLiveness = async (image, challenge) => {
  const res = await axiosClient.post('/face/liveness-check', { image, challenge });
  return res.data; // { status, challengePassed, message }
};
