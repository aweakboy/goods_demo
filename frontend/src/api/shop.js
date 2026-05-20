import request from './request'

export const shopApi = {
  // Seller
  getMyShop: () => request.get('/seller/shop'),
  registerShop: data => request.post('/seller/shop', data),
  updateShop: data => request.put('/seller/shop', data),

  // Public
  getStorefront: (id, params) => request.get(`/shops/${id}`, { params }),
  searchShops: name => request.get('/shops', { params: { name } }),
}
