import axios from 'axios'
import { useUserStore } from '@/stores/user'
import router from '@/router'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10000
})

request.interceptors.request.use(config => {
  const userStore = useUserStore()
  if (userStore.accessToken) {
    config.headers.Authorization = `Bearer ${userStore.accessToken}`
  }
  return config
})

let isRefreshing = false
let pendingQueue = []

request.interceptors.response.use(
  res => res.data,
  async err => {
    const original = err.config
    if (err.response?.status === 401 && !original._retry) {
      const userStore = useUserStore()
      if (!userStore.refreshToken) {
        userStore.logout()
        router.push('/login')
        return Promise.reject(err)
      }
      if (isRefreshing) {
        return new Promise(resolve => {
          pendingQueue.push(token => {
            original.headers.Authorization = `Bearer ${token}`
            resolve(request(original))
          })
        })
      }
      original._retry = true
      isRefreshing = true
      try {
        const res = await axios.post(
          `${import.meta.env.VITE_API_BASE_URL}/auth/refresh`,
          { refreshToken: userStore.refreshToken }
        )
        const newToken = res.data.data.accessToken
        userStore.updateAccessToken(newToken)
        pendingQueue.forEach(cb => cb(newToken))
        pendingQueue = []
        original.headers.Authorization = `Bearer ${newToken}`
        return request(original)
      } catch {
        userStore.logout()
        router.push('/login')
        return Promise.reject(err)
      } finally {
        isRefreshing = false
      }
    }
    return Promise.reject(err.response?.data || err)
  }
)

export default request
