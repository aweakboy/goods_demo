import request from './request'

export const adminApi = {
  getOverview: () => request.get('/admin/overview'),

  getUsers: (params) => request.get('/admin/users', { params }),
  updateUserStatus: (id, status) => request.put(`/admin/users/${id}/status`, { status }),

  getProducts: (params) => request.get('/admin/products', { params }),
  updateProductStatus: (id, status) => request.put(`/admin/products/${id}/status`, { status }),

  getCategories: () => request.get('/admin/categories'),
  createCategory: (name) => request.post('/admin/categories', { name }),
  updateCategory: (id, name) => request.put(`/admin/categories/${id}`, { name }),
  updateCategoryStatus: (id, status) => request.put(`/admin/categories/${id}/status`, { status }),

  getShops: (params) => request.get('/admin/shops', { params }),
  updateShopStatus: (id, status) => request.put(`/admin/shops/${id}/status`, { status }),

  getOrders: (params) => request.get('/admin/orders', { params }),
  getOrderDetail: (id) => request.get(`/admin/orders/${id}`),
}
