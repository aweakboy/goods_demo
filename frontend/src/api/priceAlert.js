import request from './request'

export const priceAlertApi = {
  list: params => request.get('/buyer/price-alerts', { params }),
  current: productId => request.get(`/buyer/price-alerts/products/${productId}`),
  save: (productId, data) => request.post(`/buyer/price-alerts/products/${productId}`, data),
  cancel: productId => request.delete(`/buyer/price-alerts/products/${productId}`)
}
