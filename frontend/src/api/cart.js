import request from './request'

export const cartApi = {
  list: () => request.get('/cart/items'),
  add: data => request.post('/cart/items', data),
  update: (id, quantity) => request.put(`/cart/items/${id}`, { quantity }),
  remove: id => request.delete(`/cart/items/${id}`),
  clear: () => request.delete('/cart')
}
