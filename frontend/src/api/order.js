import request from './request'

export const orderApi = {
  create: data => request.post('/orders', data),
  list: params => request.get('/orders', { params }),
  detail: id => request.get(`/orders/${id}`),
  pay: id => request.post(`/orders/${id}/pay`),
  reconcilePayment: id => request.post(`/orders/${id}/payment/reconcile`),
  confirm: id => request.post(`/orders/${id}/confirm`),
  cancel: id => request.post(`/orders/${id}/cancel`),
  sellerList: params => request.get('/seller/orders', { params }),
  carriers: () => request.get('/seller/shipping/carriers'),
  ship: (id, data) => request.post(`/seller/orders/${id}/ship`, data),
  refundRequest: (id, reason) => request.post(`/orders/${id}/refund-request`, { reason })
}
