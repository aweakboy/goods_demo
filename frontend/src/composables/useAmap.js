let amapPromise = null

export function getAmapConfig() {
  return {
    key: import.meta.env.VITE_AMAP_JS_API_KEY || '',
    securityJsCode: import.meta.env.VITE_AMAP_SECURITY_JS_CODE || ''
  }
}

export function loadAmap() {
  if (window.AMap) {
    return Promise.resolve(window.AMap)
  }
  if (amapPromise) {
    return amapPromise
  }

  const { key, securityJsCode } = getAmapConfig()
  if (!key) {
    return Promise.reject(new Error('未配置地图 Key'))
  }
  if (securityJsCode) {
    window._AMapSecurityConfig = { securityJsCode }
  }

  amapPromise = new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${encodeURIComponent(key)}`
    script.async = true
    script.onload = () => resolve(window.AMap)
    script.onerror = () => reject(new Error('地图加载失败'))
    document.head.appendChild(script)
  })

  return amapPromise
}
