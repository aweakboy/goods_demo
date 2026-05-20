import request from './request'

export const productApi = {
  list: params => request.get('/products', { params }),
  detail: id => request.get(`/products/${id}`),
  categories: () => request.get('/categories'),
  sellerList: () => request.get('/seller/products'),
  create: data => request.post('/seller/products', data),
  update: (id, data) => request.put(`/seller/products/${id}`, data)
}
