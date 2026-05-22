import { ref } from 'vue'

const THEME_STORAGE_KEY = 'preferred-theme'
const DARK_THEME = 'dark'
const LIGHT_THEME = 'light'

const isDark = ref(false)

function applyDocumentTheme(dark) {
  if (typeof document === 'undefined') {
    return
  }
  document.documentElement.classList.toggle('dark', dark)
}

export function initTheme() {
  if (typeof window === 'undefined') {
    return
  }

  isDark.value = window.localStorage.getItem(THEME_STORAGE_KEY) === DARK_THEME
  applyDocumentTheme(isDark.value)
}

export function setTheme(dark) {
  isDark.value = dark
  applyDocumentTheme(dark)

  if (typeof window !== 'undefined') {
    window.localStorage.setItem(THEME_STORAGE_KEY, dark ? DARK_THEME : LIGHT_THEME)
  }
}

export function toggleTheme() {
  setTheme(!isDark.value)
}

export function useTheme() {
  return {
    isDark,
    initTheme,
    setTheme,
    toggleTheme
  }
}
