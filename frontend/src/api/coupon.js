import request from './request'

export const couponApi = {
  adminList: params => request.get('/admin/coupons', { params }),
  adminCreate: data => request.post('/admin/coupons', data),
  adminUpdate: (id, data) => request.put(`/admin/coupons/${id}`, data),
  adminActivate: id => request.post(`/admin/coupons/${id}/activate`),
  adminDeactivate: id => request.post(`/admin/coupons/${id}/deactivate`),

  claimable: () => request.get('/buyer/coupons/claimable'),
  claim: couponId => request.post(`/buyer/coupons/${couponId}/claim`),
  mine: params => request.get('/buyer/coupons/mine', { params }),
  usable: () => request.get('/buyer/coupons/usable')
}
