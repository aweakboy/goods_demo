<template>
  <div v-if="shipment" class="shipment-panel">
    <div class="shipment-header">
      <div>
        <div class="shipment-title">{{ shipment.carrierName || '物流信息' }}</div>
        <div class="shipment-subtitle">运单号：{{ shipment.trackingNumber || '-' }}</div>
      </div>
      <el-tag :type="statusType(shipment.status)" size="small">
        {{ shipment.statusLabel || statusLabel(shipment.status) }}
      </el-tag>
    </div>

    <div class="shipment-meta">
      <span v-if="shipment.shippedAt">发货：{{ formatDate(shipment.shippedAt) }}</span>
      <span v-if="shipment.estimatedDeliveredAt">预计送达：{{ formatDate(shipment.estimatedDeliveredAt) }}</span>
      <span v-if="shipment.deliveredAt">签收：{{ formatDate(shipment.deliveredAt) }}</span>
    </div>

    <el-alert
      v-if="shipment.legacy"
      class="shipment-alert"
      type="info"
      :closable="false"
      title="历史物流单号，暂无结构化物流轨迹"
    />
    <el-alert
      v-else-if="shipment.status === 'EXCEPTION'"
      class="shipment-alert"
      type="error"
      :closable="false"
      :title="shipment.latestEventDescription || '物流异常'"
    />

    <el-timeline v-if="events.length" class="shipment-events">
      <el-timeline-item
        v-for="event in events"
        :key="event.id || `${event.status}-${event.eventTime}`"
        :timestamp="formatDate(event.eventTime)"
        :type="event.status === 'EXCEPTION' ? 'danger' : event.status === 'DELIVERED' ? 'success' : 'primary'"
      >
        <div class="event-desc">{{ event.description }}</div>
        <div v-if="event.location" class="event-location">{{ event.location }}</div>
      </el-timeline-item>
    </el-timeline>
    <div v-else-if="!shipment.legacy" class="shipment-empty">暂无物流轨迹</div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  shipment: {
    type: Object,
    default: null
  }
})

const events = computed(() => props.shipment?.events || [])

const STATUS_LABELS = {
  SHIPPED: '已揽收',
  IN_TRANSIT: '运输中',
  OUT_FOR_DELIVERY: '派送中',
  DELIVERED: '已签收',
  EXCEPTION: '物流异常'
}

const STATUS_TYPES = {
  SHIPPED: 'primary',
  IN_TRANSIT: 'primary',
  OUT_FOR_DELIVERY: 'warning',
  DELIVERED: 'success',
  EXCEPTION: 'danger'
}

function statusLabel(status) {
  return STATUS_LABELS[status] || status || '-'
}

function statusType(status) {
  return STATUS_TYPES[status] || 'info'
}

function formatDate(value) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-'
}
</script>

<style scoped>
.shipment-panel {
  border: 1px solid var(--border-color, #e5e7eb);
  border-radius: 8px;
  padding: 12px;
  background: var(--surface-color, #fff);
}
.shipment-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}
.shipment-title {
  font-weight: 600;
  color: var(--text-primary, #303133);
}
.shipment-subtitle,
.shipment-meta,
.event-location,
.shipment-empty {
  color: var(--text-secondary, #606266);
  font-size: 13px;
}
.shipment-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 8px;
}
.shipment-alert {
  margin-top: 10px;
}
.shipment-events {
  margin-top: 14px;
  padding-left: 2px;
}
.event-desc {
  color: var(--text-primary, #303133);
}
.shipment-empty {
  margin-top: 12px;
}
</style>
