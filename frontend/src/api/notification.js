import request from './request'

export const notificationApi = {
  list: params => request.get('/buyer/notifications', { params }),
  unreadCount: () => request.get('/buyer/notifications/unread-count'),
  markRead: id => request.post(`/buyer/notifications/${id}/read`),
  markAllRead: () => request.post('/buyer/notifications/read-all')
}
