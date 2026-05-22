import request from './request'

export const membershipApi = {
  adminList: params => request.get('/admin/membership/plans', { params }),
  adminCreate: data => request.post('/admin/membership/plans', data),
  adminUpdate: (id, data) => request.put(`/admin/membership/plans/${id}`, data),
  adminActivate: id => request.post(`/admin/membership/plans/${id}/activate`),
  adminDeactivate: id => request.post(`/admin/membership/plans/${id}/deactivate`),

  plans: () => request.get('/buyer/membership/plans'),
  status: () => request.get('/buyer/membership/status'),
  purchases: () => request.get('/buyer/membership/purchases'),
  purchase: planId => request.post(`/buyer/membership/plans/${planId}/purchase`),
  claimMonthlyBenefit: () => request.post('/buyer/membership/benefits/monthly/claim')
}
