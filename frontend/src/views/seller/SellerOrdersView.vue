<template>
  <div class="page-container">
    <h2>订单管理</h2>
    <el-tabs v-model="activeTab" @tab-change="fetchOrders">
      <el-tab-pane label="全部" name="" />
      <el-tab-pane label="待付款" name="PENDING_PAYMENT" />
      <el-tab-pane label="已付款" name="PAID" />
      <el-tab-pane label="已发货" name="SHIPPED" />
      <el-tab-pane label="已完成" name="COMPLETED" />
    </el-tabs>

    <div v-if="orders.length === 0" class="empty"><el-empty description="暂无订单" /></div>
    <el-card v-for="order in orders" :key="order.id" class="order-card">
      <div class="order-header">
        <span>订单号：{{ order.id }}</span>
        <el-tag :type="statusType(order.status)">{{ statusLabel(order.status) }}</el-tag>
      </div>
      <p style="color:var(--text-secondary);margin:8px 0">收货地址：{{ formatOrderAddress(order) }}</p>
      <AddressMap
        v-if="hasOrderMap(order)"
        class="order-map"
        :longitude="order.receiverLongitude"
        :latitude="order.receiverLatitude"
        title="收货地址"
        :address="formatOrderAddress(order)"
      />
      <ShipmentTimeline
        v-if="shipmentOf(order)"
        class="shipment-block"
        :shipment="shipmentOf(order)"
      />
      <ShipmentMap
        v-if="shipmentOf(order)"
        class="shipment-map-block"
        :map-data="shipmentOf(order).mapSimulation"
      />
      <div class="order-footer">
        <span>{{ order.createdAt?.slice(0,10) }}</span>
        <div style="display:flex;align-items:center;gap:12px">
          <strong style="color:#f56c6c">¥{{ order.totalAmount?.toFixed(2) }}</strong>
          <el-button v-if="order.status === 'PAID'" type="primary" size="small" @click="openShipDialog(order)">发货</el-button>
        </div>
      </div>
    </el-card>

    <el-dialog v-model="shipDialog" title="填写物流信息" width="400px">
      <el-form :model="shipForm" label-width="90px">
        <el-form-item label="物流公司">
          <el-select
            v-model="shipForm.carrierCode"
            placeholder="请选择物流公司"
            style="width:100%"
            @change="onCarrierChange"
          >
            <el-option
              v-for="carrier in carrierOptions"
              :key="carrier.code"
              :label="carrier.name"
              :value="carrier.code"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="物流单号">
          <el-input v-model="shipForm.trackingNumber" placeholder="请输入物流单号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shipDialog=false">取消</el-button>
        <el-button type="primary" :loading="shipLoading" @click="doShip">确认发货</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { orderApi } from '@/api/order'
import AddressMap from '@/components/AddressMap.vue'
import ShipmentMap from '@/components/ShipmentMap.vue'
import ShipmentTimeline from '@/components/ShipmentTimeline.vue'

const orders = ref([])
const activeTab = ref('')
const shipDialog = ref(false)
const shipLoading = ref(false)
const carrierOptions = ref([])
const shipForm = ref({ orderId: null, carrierCode: '', carrierName: '', trackingNumber: '' })

const statusLabel = s => ({
  PENDING_PAYMENT:'待付款', PAID:'已付款', SHIPPED:'已发货', COMPLETED:'已完成', CANCELLED:'已取消',
  REFUND_REQUESTED:'退款申请中', REFUNDED:'已退款', REFUND_REJECTED:'退款被拒'
}[s] || s)
const statusType = s => ({
  PENDING_PAYMENT:'warning', PAID:'primary', SHIPPED:'', COMPLETED:'success', CANCELLED:'info',
  REFUND_REQUESTED:'warning', REFUNDED:'success', REFUND_REJECTED:'danger'
}[s] || '')
const formatOrderAddress = order => order.receiverFullAddress || order.address || '-'
const hasOrderMap = order => order.receiverAddressValidationStatus === 'VALID' && order.receiverLongitude && order.receiverLatitude
const shipmentOf = order => order.shipment || (order.trackingNumber ? {
  trackingNumber: order.trackingNumber,
  status: 'SHIPPED',
  statusLabel: '已发货',
  legacy: true,
  trackingAvailable: false,
  mapSimulation: { routeAvailable: false, fallbackReason: '历史物流单号暂无地图路线' },
  events: []
} : null)

async function fetchOrders() {
  const res = await orderApi.sellerList(activeTab.value ? { status: activeTab.value } : {})
  orders.value = res.data
}

async function fetchCarriers() {
  const res = await orderApi.carriers()
  carrierOptions.value = res.data
}

function openShipDialog(order) {
  shipForm.value = { orderId: order.id, carrierCode: '', carrierName: '', trackingNumber: '' }
  shipDialog.value = true
}

function onCarrierChange(code) {
  const carrier = carrierOptions.value.find(item => item.code === code)
  shipForm.value.carrierName = carrier?.name || ''
}

async function doShip() {
  if (!shipForm.value.carrierCode) {
    ElMessage.warning('请选择物流公司')
    return
  }
  if (!shipForm.value.trackingNumber.trim()) {
    ElMessage.warning('请填写物流单号')
    return
  }
  shipLoading.value = true
  try {
    await orderApi.ship(shipForm.value.orderId, {
      carrierCode: shipForm.value.carrierCode,
      carrierName: shipForm.value.carrierName,
      trackingNumber: shipForm.value.trackingNumber.trim()
    })
    shipDialog.value = false
    ElMessage.success('发货成功')
    await fetchOrders()
  } catch (err) {
    ElMessage.error(err?.message || '发货失败')
  } finally {
    shipLoading.value = false
  }
}

onMounted(async () => {
  await Promise.all([fetchOrders(), fetchCarriers()])
})
</script>

<style scoped>
.empty { padding: 40px 0; }
.order-card { margin-bottom: 12px; }
.order-header, .order-footer { display: flex; justify-content: space-between; align-items: center; }
.order-footer { margin-top: 8px; color: var(--text-secondary); }
.order-map { margin: 8px 0 12px; }
.shipment-block { margin: 8px 0 12px; }
.shipment-map-block { margin: 8px 0 12px; }
</style>
