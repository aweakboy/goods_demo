<template>
  <div class="shipment-map">
    <div v-if="!mapData" class="map-fallback">暂无物流地图数据</div>
    <div v-else-if="!mapData.routeAvailable" class="map-fallback">
      {{ mapData.fallbackReason || '暂无可用物流地图路线' }}
    </div>
    <div v-else-if="error" class="map-fallback">{{ error }}</div>
    <template v-else>
      <div class="route-summary">
        <span>{{ routeSourceLabel }}</span>
        <span v-if="formattedDistance">距离：{{ formattedDistance }}</span>
        <span v-if="formattedDuration">预计：{{ formattedDuration }}</span>
        <span v-if="formattedPlannedAt">规划：{{ formattedPlannedAt }}</span>
        <span v-if="mapData.planningFailureReason" class="route-warning">{{ mapData.planningFailureReason }}</span>
      </div>
      <div ref="mapEl" class="map-canvas"></div>
    </template>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { loadAmap } from '@/composables/useAmap'

const props = defineProps({
  mapData: {
    type: Object,
    default: null
  }
})

const mapEl = ref(null)
const error = ref('')
let map = null
let markers = []
let lines = []

const points = computed(() => {
  const data = props.mapData
  if (!data?.routeAvailable) return null
  return {
    origin: toLngLat(data.origin),
    destination: toLngLat(data.destination),
    current: toLngLat(data.currentPosition)
  }
})
const routePath = computed(() => {
  const path = normalizePath(props.mapData?.routePath)
  if (path.length >= 2) return path
  const origin = points.value?.origin
  const destination = points.value?.destination
  return origin && destination ? [origin, destination] : []
})
const completedPath = computed(() => {
  const path = normalizePath(props.mapData?.completedPath)
  if (path.length >= 2) return path
  const origin = points.value?.origin
  const current = points.value?.current
  return origin && current ? [origin, current] : []
})
const routeSourceLabel = computed(() => props.mapData?.routeSource === 'PLANNED' ? '真实路线' : '模拟路线')
const formattedDistance = computed(() => formatDistance(props.mapData?.distanceMeters))
const formattedDuration = computed(() => formatDuration(props.mapData?.durationSeconds))
const formattedPlannedAt = computed(() => formatDateTime(props.mapData?.plannedAt))

async function renderMap() {
  if (!props.mapData?.routeAvailable || !points.value || !mapEl.value) return
  try {
    error.value = ''
    const AMap = await loadAmap()
    const { origin, destination, current } = points.value
    if (!origin || !destination || !current) {
      error.value = '物流坐标不完整'
      return
    }

    if (!map) {
      map = new AMap.Map(mapEl.value, {
        zoom: 11,
        center: current
      })
    } else {
      clearOverlays()
      map.setCenter(current)
    }

    const routeLine = new AMap.Polyline({
      path: routePath.value,
      strokeColor: '#909399',
      strokeOpacity: props.mapData.routeSource === 'PLANNED' ? 0.75 : 0.55,
      strokeWeight: 6,
      strokeStyle: props.mapData.routeSource === 'PLANNED' ? 'solid' : 'dashed'
    })
    const progressLine = new AMap.Polyline({
      path: completedPath.value,
      strokeColor: '#409eff',
      strokeOpacity: 0.95,
      strokeWeight: 7
    })

    const originMarker = new AMap.Marker({ position: origin, title: props.mapData.origin?.title || '发货地' })
    const destinationMarker = new AMap.Marker({ position: destination, title: props.mapData.destination?.title || '收货地' })
    const currentMarker = new AMap.Marker({
      position: current,
      title: props.mapData.currentPosition?.title || props.mapData.statusLabel || '当前位置',
      offset: new AMap.Pixel(-13, -30)
    })

    lines = [routeLine, progressLine]
    markers = [originMarker, destinationMarker, currentMarker]
    map.add([...lines, ...markers])
    map.setFitView([...lines, ...markers], false, [24, 24, 24, 24])
  } catch (err) {
    error.value = err?.message || '物流地图加载失败'
  }
}

function clearOverlays() {
  if (!map) return
  const overlays = [...lines, ...markers]
  if (overlays.length) {
    map.remove(overlays)
  }
  lines = []
  markers = []
}

function normalizePath(path) {
  if (!Array.isArray(path)) return []
  return path.map(toLngLat).filter(Boolean)
}

function toLngLat(point) {
  if (!point) return null
  const lng = Number(point.longitude)
  const lat = Number(point.latitude)
  if (!Number.isFinite(lng) || !Number.isFinite(lat)) return null
  return [lng, lat]
}

function formatDistance(value) {
  const meters = Number(value)
  if (!Number.isFinite(meters) || meters <= 0) return ''
  if (meters < 1000) return `${Math.round(meters)} 米`
  return `${(meters / 1000).toFixed(1)} 公里`
}

function formatDuration(value) {
  const seconds = Number(value)
  if (!Number.isFinite(seconds) || seconds <= 0) return ''
  const minutes = Math.round(seconds / 60)
  if (minutes < 60) return `${minutes} 分钟`
  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60
  return rest ? `${hours} 小时 ${rest} 分钟` : `${hours} 小时`
}

function formatDateTime(value) {
  return value ? value.replace('T', ' ').slice(0, 16) : ''
}

watch(() => props.mapData, async () => {
  if (!props.mapData?.routeAvailable) {
    clearOverlays()
    error.value = ''
    return
  }
  await nextTick()
  renderMap()
}, { deep: true })
onMounted(() => nextTick(renderMap))
onUnmounted(() => {
  clearOverlays()
  if (map) {
    map.destroy()
    map = null
  }
})
</script>

<style scoped>
.shipment-map {
  width: 100%;
}

.map-canvas,
.map-fallback {
  width: 100%;
  height: 260px;
  border: 1px solid var(--border-color, #e5e7eb);
  border-radius: 8px;
  overflow: hidden;
}

.map-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 16px;
  color: var(--text-secondary, #606266);
  background: var(--bg-secondary, #f5f7fa);
  text-align: center;
}

.route-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  margin-bottom: 8px;
  color: var(--text-secondary, #606266);
  font-size: 13px;
  line-height: 1.4;
}

.route-warning {
  color: #e6a23c;
}
</style>
