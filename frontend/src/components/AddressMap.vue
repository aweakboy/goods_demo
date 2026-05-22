<template>
  <div class="address-map">
    <div v-if="!hasCoordinate" class="map-fallback">{{ fallbackText }}</div>
    <div v-else-if="error" class="map-fallback">{{ error }}</div>
    <div v-else ref="mapEl" class="map-canvas"></div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { loadAmap } from '@/composables/useAmap'

const props = defineProps({
  longitude: [Number, String],
  latitude: [Number, String],
  title: { type: String, default: '地址定位' },
  address: { type: String, default: '' },
  fallbackText: { type: String, default: '暂无可用地图定位' }
})

const mapEl = ref(null)
const error = ref('')
let map = null
let marker = null

const lng = computed(() => Number(props.longitude))
const lat = computed(() => Number(props.latitude))
const hasCoordinate = computed(() => Number.isFinite(lng.value) && Number.isFinite(lat.value))

async function renderMap() {
  if (!hasCoordinate.value || !mapEl.value) return
  try {
    error.value = ''
    const AMap = await loadAmap()
    const center = [lng.value, lat.value]
    if (!map) {
      map = new AMap.Map(mapEl.value, {
        zoom: 15,
        center
      })
      marker = new AMap.Marker({
        position: center,
        title: props.title
      })
      map.add(marker)
    } else {
      map.setCenter(center)
      marker.setPosition(center)
      marker.setTitle(props.title)
    }
  } catch (err) {
    error.value = err?.message || '地图加载失败'
  }
}

watch(() => [props.longitude, props.latitude], renderMap)
onMounted(renderMap)
onUnmounted(() => {
  if (map) {
    map.destroy()
    map = null
    marker = null
  }
})
</script>

<style scoped>
.address-map {
  width: 100%;
}

.map-canvas,
.map-fallback {
  width: 100%;
  height: 220px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  overflow: hidden;
}

.map-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
  background: var(--bg-secondary);
}
</style>
