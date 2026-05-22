<template>
  <div class="page-container" v-if="order">
    <h2>订单详情</h2>
    <el-card style="margin-bottom:16px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="订单号">{{ order.id }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusType(order.status)">{{ statusLabel(order.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="收货人">{{ order.receiverName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ order.receiverPhone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="收货地址" :span="2">{{ orderAddress }}</el-descriptions-item>
        <el-descriptions-item label="下单时间">{{ order.createdAt?.slice(0,19).replace('T',' ') }}</el-descriptions-item>
        <el-descriptions-item label="合计">
          <strong style="color:#f56c6c">¥{{ order.totalAmount?.toFixed(2) }}</strong>
        </el-descriptions-item>
        <el-descriptions-item v-if="hasCouponSnapshot" label="商品原价">
          ¥{{ amountText(order.originalAmount) }}
        </el-descriptions-item>
        <el-descriptions-item v-if="hasCouponSnapshot" label="优惠金额">
          -¥{{ amountText(order.discountAmount) }}
        </el-descriptions-item>
        <el-descriptions-item v-if="hasMembershipSnapshot" label="会员优惠">
          -¥{{ amountText(order.membershipDiscountAmount) }}
        </el-descriptions-item>
        <el-descriptions-item v-if="hasMembershipSnapshot" label="会员套餐">
          {{ order.membershipPlanName || '-' }}（{{ discountText(order.membershipDiscountRate) }}）
        </el-descriptions-item>
        <el-descriptions-item v-if="order.couponName" label="使用优惠券" :span="2">
          {{ order.couponName }}（满 ¥{{ amountText(order.couponThresholdAmount) }} 减 ¥{{ amountText(order.couponDiscountAmount) }}）
        </el-descriptions-item>
        <el-descriptions-item v-if="order.status === 'PENDING_PAYMENT' && order.expiredAt" label="支付截止" :span="2">
          <span :style="{ color: countdown === '已超时' ? '#f56c6c' : remainingSecs < 60 ? '#e6a23c' : '#303133', fontWeight: 500 }">
            {{ order.expiredAt.slice(0,19).replace('T',' ') }}
            （剩余 <strong>{{ countdown }}</strong>）
          </span>
        </el-descriptions-item>
      </el-descriptions>
      <AddressMap
        v-if="hasReceiverMap"
        style="margin-top:16px"
        :longitude="order.receiverLongitude"
        :latitude="order.receiverLatitude"
        title="收货地址"
        :address="orderAddress"
      />
    </el-card>

    <el-card v-if="shipment" style="margin-bottom:16px">
      <h3>物流信息</h3>
      <ShipmentMap class="shipment-map-block" :map-data="shipment.mapSimulation" />
      <ShipmentTimeline :shipment="shipment" />
    </el-card>
    <el-card v-else-if="order.status === 'PAID'" style="margin-bottom:16px">
      <h3>物流信息</h3>
      <div class="muted">商家尚未发货</div>
    </el-card>

    <el-card style="margin-bottom:16px">
      <h3>商品明细</h3>
      <el-table :data="order.items">
        <el-table-column label="商品" prop="productName" min-width="180" />
        <el-table-column label="店铺" prop="shopName" width="130">
          <template #default="{row}">{{ row.shopName || '-' }}</template>
        </el-table-column>
        <el-table-column label="单价" width="110">
          <template #default="{row}">¥{{ row.price?.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="数量" prop="quantity" width="70" />
        <el-table-column label="小计" width="110">
          <template #default="{row}">¥{{ (row.price * row.quantity).toFixed(2) }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <div class="actions">
      <el-button v-if="order.status === 'PENDING_PAYMENT'" type="primary" @click="openPayDialog">立即支付</el-button>
      <el-button v-if="order.status === 'PENDING_PAYMENT'" :loading="paymentCheckLoading" @click="checkPaymentStatus(false)">确认支付状态</el-button>
      <el-button v-if="order.status === 'SHIPPED'" type="success" @click="confirmReceipt">确认收货</el-button>
      <el-button v-if="['PAID','SHIPPED','COMPLETED'].includes(order.status)" type="warning" plain @click="refundDialog=true">申请退款</el-button>
      <el-button v-if="order.status === 'PENDING_PAYMENT'" type="danger" plain @click="cancelOrder">取消订单</el-button>
      <el-button @click="$router.push('/orders')">返回列表</el-button>
    </div>

    <el-dialog v-model="refundDialog" title="申请退款" width="400px">
      <el-form>
        <el-form-item label="退款原因" label-width="80px">
          <el-input v-model="refundReason" type="textarea" :rows="4" placeholder="请描述退款原因..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="refundDialog=false">取消</el-button>
        <el-button type="warning" :loading="refundLoading" @click="submitRefund">提交申请</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="payDialog" title="支付宝支付" width="380px">
      <div style="text-align:center; padding: 8px 0 16px">
        <p style="margin:0 0 8px">支付金额</p>
        <p style="font-size:28px; font-weight:700; color:#f56c6c; margin:0 0 16px">
          ¥{{ order.totalAmount?.toFixed(2) }}
        </p>
        <el-alert v-if="order.expiredAt" :type="remainingSecs < 60 ? 'warning' : 'info'"
          :closable="false" style="text-align:left">
          <template #title>
            请在 <strong>{{ countdown }}</strong> 内完成支付，超时订单将自动取消
          </template>
        </el-alert>
      </div>
      <template #footer>
        <el-button @click="payDialog=false">稍后再付</el-button>
        <el-button type="primary" :loading="payLoading" @click="doPay">前往支付宝付款</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { orderApi } from '@/api/order'
import AddressMap from '@/components/AddressMap.vue'
import ShipmentMap from '@/components/ShipmentMap.vue'
import ShipmentTimeline from '@/components/ShipmentTimeline.vue'

const refundDialog = ref(false)
const refundReason = ref('')
const refundLoading = ref(false)

const route = useRoute()
const order = ref(null)
const payDialog = ref(false)
const payLoading = ref(false)
const paymentCheckLoading = ref(false)
const waitingForPayment = ref(false)
const now = ref(Date.now())
let timer = null

const remainingSecs = computed(() => {
  if (!order.value?.expiredAt) return Infinity
  const exp = new Date(order.value.expiredAt).getTime()
  return Math.max(0, Math.floor((exp - now.value) / 1000))
})

const countdown = computed(() => {
  const secs = remainingSecs.value
  if (secs === 0) return '已超时'
  const m = Math.floor(secs / 60).toString().padStart(2, '0')
  const s = (secs % 60).toString().padStart(2, '0')
  return `${m}:${s}`
})

const statusLabel = s => ({
  PENDING_PAYMENT:'待付款', PAID:'已付款', SHIPPED:'已发货',
  COMPLETED:'已完成', CANCELLED:'已取消',
  REFUND_REQUESTED:'退款申请中', REFUNDED:'已退款', REFUND_REJECTED:'退款被拒'
}[s] || s)
const statusType = s => ({
  PENDING_PAYMENT:'warning', PAID:'primary', SHIPPED:'',
  COMPLETED:'success', CANCELLED:'info',
  REFUND_REQUESTED:'warning', REFUNDED:'success', REFUND_REJECTED:'danger'
}[s] || '')

const orderAddress = computed(() => order.value?.receiverFullAddress || order.value?.address || '-')
const shipment = computed(() => order.value?.shipment || (order.value?.trackingNumber ? {
  trackingNumber: order.value.trackingNumber,
  status: 'SHIPPED',
  statusLabel: '已发货',
  legacy: true,
  trackingAvailable: false,
  mapSimulation: { routeAvailable: false, fallbackReason: '历史物流单号暂无地图路线' },
  events: []
} : null))
const hasReceiverMap = computed(() =>
  order.value?.receiverAddressValidationStatus === 'VALID'
  && order.value?.receiverLongitude
  && order.value?.receiverLatitude
)
const hasCouponSnapshot = computed(() =>
  order.value?.originalAmount != null
  && Number(order.value.discountAmount || 0) > 0
)
const hasMembershipSnapshot = computed(() =>
  Number(order.value?.membershipDiscountAmount || 0) > 0
)

function amountText(value) {
  return Number(value || 0).toFixed(2)
}

function discountText(value) {
  return `${(Number(value || 1) * 10).toFixed(1)}折`
}

async function load() {
  const res = await orderApi.detail(route.params.id)
  order.value = res.data
}

function openPayDialog() { payDialog.value = true }

async function doPay() {
  payLoading.value = true
  try {
    const htmlForm = await orderApi.pay(order.value.id)
    const div = document.createElement('div')
    div.innerHTML = htmlForm
    document.body.appendChild(div)
    const form = div.querySelector('form')
    form.target = '_blank'
    form.submit()
    document.body.removeChild(div)
    payDialog.value = false
    waitingForPayment.value = true
    ElMessage.info('支付页面已打开，完成后回到本页会自动确认支付状态')
  } catch (err) {
    ElMessage.error(err?.message || '发起支付失败')
  } finally {
    payLoading.value = false
  }
}

async function checkPaymentStatus(silent = true) {
  if (!order.value || order.value.status !== 'PENDING_PAYMENT') return
  paymentCheckLoading.value = true
  const previousStatus = order.value.status
  try {
    const res = await orderApi.reconcilePayment(order.value.id)
    order.value = res.data
    if (previousStatus !== 'PAID' && order.value.status === 'PAID') {
      waitingForPayment.value = false
      ElMessage.success('支付成功，订单已更新为已付款')
    } else if (!silent && order.value.status === 'PENDING_PAYMENT') {
      ElMessage.info('支付状态仍在确认中，请稍后重试')
    }
  } catch (err) {
    if (!silent) {
      ElMessage.error(err?.message || '支付状态暂未确认')
    }
    await load()
  } finally {
    paymentCheckLoading.value = false
  }
}

async function confirmReceipt() {
  await ElMessageBox.confirm('确认已收到商品？', '确认收货', { type: 'success' })
  const res = await orderApi.confirm(order.value.id)
  order.value = res.data
  ElMessage.success('确认收货成功')
}

async function cancelOrder() {
  await ElMessageBox.confirm('确认取消此订单？', '取消订单', { type: 'warning' })
  const res = await orderApi.cancel(order.value.id)
  order.value = res.data
  ElMessage.success('订单已取消')
}

async function submitRefund() {
  if (!refundReason.value.trim()) {
    ElMessage.warning('请填写退款原因')
    return
  }
  refundLoading.value = true
  try {
    const res = await orderApi.refundRequest(order.value.id, refundReason.value)
    order.value = res.data
    refundDialog.value = false
    refundReason.value = ''
    ElMessage.success('退款申请已提交，等待管理员审核')
  } catch (err) {
    ElMessage.error(err?.message || '申请退款失败')
  } finally {
    refundLoading.value = false
  }
}

onMounted(async () => {
  await load()
  window.addEventListener('focus', handlePaymentReturnRefresh)
  document.addEventListener('visibilitychange', handlePaymentReturnRefresh)
  // 每秒更新倒计时，超时后自动刷新订单状态
  timer = setInterval(async () => {
    now.value = Date.now()
    if (order.value?.status === 'PENDING_PAYMENT' && remainingSecs.value === 0) {
      clearInterval(timer)
      await load()
    }
  }, 1000)
})

function handlePaymentReturnRefresh() {
  if (document.visibilityState !== 'visible') return
  if (waitingForPayment.value && order.value?.status === 'PENDING_PAYMENT') {
    checkPaymentStatus(true)
  }
}

onUnmounted(() => {
  clearInterval(timer)
  window.removeEventListener('focus', handlePaymentReturnRefresh)
  document.removeEventListener('visibilitychange', handlePaymentReturnRefresh)
})
</script>

<style scoped>
.actions { display: flex; gap: 12px; }
.muted { color: var(--text-secondary); font-size: 14px; }
.shipment-map-block { margin-bottom: 12px; }
</style>
