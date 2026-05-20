import request from './request'

export const orderApi = {
  create: data => request.post('/orders', data),
  list: params => request.get('/orders', { params }),
  detail: id => request.get(`/orders/${id}`),
  pay: id => request.post(`/orders/${id}/pay`),
  confirm: id => request.post(`/orders/${id}/confirm`),
  cancel: id => request.post(`/orders/${id}/cancel`),
  sellerList: params => request.get('/seller/orders', { params }),
  ship: (id, trackingNumber) => request.post(`/seller/orders/${id}/ship`, { trackingNumber })
}
