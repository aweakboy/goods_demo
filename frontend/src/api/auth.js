import request from './request'

export const authApi = {
  register: data => request.post('/auth/register', data),
  login: data => request.post('/auth/login', data),
  refresh: refreshToken => request.post('/auth/refresh', { refreshToken })
}
