import request from './request'

export const productApi = {
  list: params => request.get('/products', { params }),
  detail: id => request.get(`/products/${id}`),
  categories: () => request.get('/categories'),
  sellerList: () => request.get('/seller/products'),
  create: data => request.post('/seller/products', data),
  update: (id, data) => request.put(`/seller/products/${id}`, data),
  uploadImage: file => {
    const form = new FormData()
    form.append('file', file)
    return request.post('/seller/products/upload-image', form, {
      headers: { 'Content-Type': 'multipart/form-data' }
    }).then(res => res.data.url)
  }
}
