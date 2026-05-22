import request from './request'

export const addressApi = {
  list: () => request.get('/buyer/addresses'),
  create: data => request.post('/buyer/addresses', data),
  update: (id, data) => request.put(`/buyer/addresses/${id}`, data),
  remove: id => request.delete(`/buyer/addresses/${id}`),
  setDefault: id => request.post(`/buyer/addresses/${id}/default`)
}
