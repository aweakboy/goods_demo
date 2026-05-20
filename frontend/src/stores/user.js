import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  const accessToken = ref(localStorage.getItem('accessToken') || '')
  const refreshToken = ref(localStorage.getItem('refreshToken') || '')
  const userId = ref(Number(localStorage.getItem('userId')) || null)
  const username = ref(localStorage.getItem('username') || '')
  const role = ref(localStorage.getItem('role') || '')

  const isLoggedIn = computed(() => !!accessToken.value)

  function setUser(data) {
    accessToken.value = data.accessToken
    refreshToken.value = data.refreshToken
    userId.value = data.userId
    username.value = data.username
    role.value = data.role
    localStorage.setItem('accessToken', data.accessToken)
    localStorage.setItem('refreshToken', data.refreshToken)
    localStorage.setItem('userId', data.userId)
    localStorage.setItem('username', data.username)
    localStorage.setItem('role', data.role)
  }

  function updateAccessToken(token) {
    accessToken.value = token
    localStorage.setItem('accessToken', token)
  }

  function logout() {
    accessToken.value = ''
    refreshToken.value = ''
    userId.value = null
    username.value = ''
    role.value = ''
    localStorage.clear()
  }

  return { accessToken, refreshToken, userId, username, role, isLoggedIn, setUser, updateAccessToken, logout }
})
