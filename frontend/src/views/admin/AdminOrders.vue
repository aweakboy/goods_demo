<template>
  <div>
    <div style="display:flex;gap:12px;margin-bottom:16px;align-items:center">
      <h2 style="margin:0">订单总览</h2>
      <el-select v-model="filterStatus" placeholder="状态筛选" clearable style="width:150px" @change="load">
        <el-option label="待付款" value="PENDING_PAYMENT" />
        <el-option label="已付款" value="PAID" />
        <el-option label="已发货" value="SHIPPED" />
        <el-option label="已完成" value="COMPLETED" />
        <el-option label="已取消" value="CANCELLED" />
      </el-select>
    </div>
    <el-table :data="orders" style="width:100%">
      <el-table-column label="订单ID" prop="id" width="90" />
      <el-table-column label="买家" prop="buyerUsername" width="120" />
      <el-table-column label="商品概览" prop="productSummary" min-width="200" />
      <el-table-column label="金额" width="110">
        <template #default="{row}">¥{{ row.totalAmount?.toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="会员优惠" width="110">
        <template #default="{row}">¥{{ money(row.membershipDiscountAmount) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{row}">
          <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" min-width="150">
        <template #default="{row}">{{ formatDate(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="80">
        <template #default="{row}">
          <el-button link type="primary" @click="showDetail(row.id)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      style="margin-top:16px;justify-content:flex-end"
      layout="total, prev, pager, next"
      :total="total"
      :page-size="pageSize"
      :current-page="currentPage"
      @current-change="onPageChange"
    />

    <el-dialog v-model="detailVisible" title="订单详情" width="760px">
      <div v-if="detail">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="订单ID">{{ detail.id }}</el-descriptions-item>
          <el-descriptions-item label="买家">{{ detail.buyerUsername }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ statusLabel(detail.status) }}</el-descriptions-item>
          <el-descriptions-item label="金额">¥{{ detail.totalAmount?.toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="会员优惠">¥{{ money(detail.membershipDiscountAmount) }}</el-descriptions-item>
          <el-descriptions-item label="会员套餐">
            {{ detail.membershipPlanName ? `${detail.membershipPlanName}（${discountText(detail.membershipDiscountRate)}）` : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="收货人">{{ detail.receiverName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ detail.receiverPhone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="收货地址" :span="2">{{ detail.receiverFullAddress || detail.address || '-' }}</el-descriptions-item>
          <el-descriptions-item label="物流公司">{{ detailShipment?.carrierName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="物流单号">{{ detailShipment?.trackingNumber || detail.trackingNumber || '-' }}</el-descriptions-item>
          <el-descriptions-item label="物流状态" :span="2">{{ detailShipment?.statusLabel || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间" :span="2">{{ formatDate(detail.createdAt) }}</el-descriptions-item>
        </el-descriptions>
        <AddressMap
          v-if="hasDetailMap"
          style="margin-top:12px"
          :longitude="detail.receiverLongitude"
          :latitude="detail.receiverLatitude"
          title="收货地址"
          :address="detail.receiverFullAddress || detail.address || '-'"
        />
        <div class="detail-section">
          <div class="section-header">
            <div class="section-title">物流轨迹</div>
            <div v-if="detailShipment?.id" class="section-actions">
              <el-button
                size="small"
                type="primary"
                :loading="simulateLoading"
                :disabled="!canAdvanceShipment"
                @click="advanceShipment"
              >
                推进物流
              </el-button>
              <el-button
                size="small"
                :loading="routeRefreshLoading"
                :disabled="!detailShipment?.id"
                @click="refreshShipmentRoute"
              >
                刷新路线
              </el-button>
              <el-button
                size="small"
                type="danger"
                plain
                :disabled="!canMarkException"
                @click="exceptionDialog=true"
              >
                标记异常
              </el-button>
            </div>
          </div>
          <ShipmentMap v-if="detailShipment" class="shipment-map-block" :map-data="detailShipment.mapSimulation" />
          <ShipmentTimeline v-if="detailShipment" :shipment="detailShipment" />
          <div v-else class="muted">暂无物流信息</div>
        </div>
        <div style="margin-top:12px;font-weight:bold;margin-bottom:8px">商品列表</div>
        <el-table :data="detail.items" size="small">
          <el-table-column label="商品名称" prop="productName" />
          <el-table-column label="单价" width="100">
            <template #default="{row}">¥{{ row.price?.toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="数量" prop="quantity" width="70" />
        </el-table>
      </div>
    </el-dialog>

    <el-dialog v-model="exceptionDialog" title="标记物流异常" width="420px">
      <el-input
        v-model="exceptionReason"
        type="textarea"
        :rows="4"
        placeholder="请输入异常原因"
      />
      <template #footer>
        <el-button @click="exceptionDialog=false">取消</el-button>
        <el-button type="danger" :loading="simulateLoading" @click="markException">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api/admin'
import AddressMap from '@/components/AddressMap.vue'
import ShipmentMap from '@/components/ShipmentMap.vue'
import ShipmentTimeline from '@/components/ShipmentTimeline.vue'

const orders = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = 20
const filterStatus = ref(null)
const detailVisible = ref(false)
const detail = ref(null)
const simulateLoading = ref(false)
const routeRefreshLoading = ref(false)
const exceptionDialog = ref(false)
const exceptionReason = ref('')

const STATUS_LABELS = {
  PENDING_PAYMENT: '待付款', PAID: '已付款', SHIPPED: '已发货', COMPLETED: '已完成', CANCELLED: '已取消',
  REFUND_REQUESTED: '退款申请中', REFUNDED: '已退款', REFUND_REJECTED: '退款被拒'
}
const STATUS_TYPES = {
  PENDING_PAYMENT: 'warning', PAID: 'primary', SHIPPED: 'success', COMPLETED: '', CANCELLED: 'info',
  REFUND_REQUESTED: 'warning', REFUNDED: 'success', REFUND_REJECTED: 'danger'
}
const hasDetailMap = computed(() =>
  detail.value?.receiverAddressValidationStatus === 'VALID'
  && detail.value?.receiverLongitude
  && detail.value?.receiverLatitude
)
const detailShipment = computed(() => detail.value?.shipment || (detail.value?.trackingNumber ? {
  trackingNumber: detail.value.trackingNumber,
  status: 'SHIPPED',
  statusLabel: '已发货',
  legacy: true,
  trackingAvailable: false,
  mapSimulation: { routeAvailable: false, fallbackReason: '历史物流单号暂无地图路线' },
  events: []
} : null))
const canAdvanceShipment = computed(() =>
  detailShipment.value?.id
  && !['DELIVERED', 'EXCEPTION'].includes(detailShipment.value.status)
)
const canMarkException = computed(() =>
  detailShipment.value?.id
  && !['DELIVERED', 'EXCEPTION'].includes(detailShipment.value.status)
)

function statusLabel(s) { return STATUS_LABELS[s] || s }
function statusType(s) { return STATUS_TYPES[s] || '' }
function formatDate(dt) { return dt ? dt.replace('T', ' ').slice(0, 16) : '' }
function money(value) { return Number(value || 0).toFixed(2) }
function discountText(value) { return `${(Number(value || 1) * 10).toFixed(1)}折` }

async function load() {
  try {
    const res = await adminApi.getOrders({
      status: filterStatus.value || undefined,
      page: currentPage.value - 1,
      size: pageSize
    })
    orders.value = res.data.content
    total.value = res.data.totalElements
  } catch {
    ElMessage.error('加载订单列表失败')
  }
}

async function showDetail(id) {
  try {
    const res = await adminApi.getOrderDetail(id)
    detail.value = res.data
    detailVisible.value = true
  } catch {
    ElMessage.error('加载订单详情失败')
  }
}

async function advanceShipment() {
  if (!detailShipment.value?.id) return
  simulateLoading.value = true
  try {
    const res = await adminApi.advanceShipment(detailShipment.value.id)
    detail.value.shipment = res.data
    ElMessage.success('物流状态已推进')
  } catch (err) {
    ElMessage.error(err?.message || '推进物流失败')
  } finally {
    simulateLoading.value = false
  }
}

async function markException() {
  if (!detailShipment.value?.id) return
  if (!exceptionReason.value.trim()) {
    ElMessage.warning('请填写异常原因')
    return
  }
  simulateLoading.value = true
  try {
    const res = await adminApi.markShipmentException(detailShipment.value.id, exceptionReason.value.trim())
    detail.value.shipment = res.data
    exceptionDialog.value = false
    exceptionReason.value = ''
    ElMessage.success('已标记物流异常')
  } catch (err) {
    ElMessage.error(err?.message || '标记异常失败')
  } finally {
    simulateLoading.value = false
  }
}

async function refreshShipmentRoute() {
  if (!detailShipment.value?.id) return
  routeRefreshLoading.value = true
  try {
    const res = await adminApi.refreshShipmentRoute(detailShipment.value.id)
    detail.value.shipment = res.data
    ElMessage.success('路线已刷新')
  } catch (err) {
    ElMessage.error(err?.message || '刷新路线失败')
  } finally {
    routeRefreshLoading.value = false
  }
}

function onPageChange(page) {
  currentPage.value = page
  load()
}

onMounted(load)
</script>

<style scoped>
.detail-section {
  margin-top: 12px;
}
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}
.section-title {
  font-weight: 700;
}
.section-actions {
  display: flex;
  gap: 8px;
}
.muted {
  color: var(--text-secondary);
  font-size: 14px;
}
.shipment-map-block {
  margin-bottom: 12px;
}
</style>
