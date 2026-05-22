<template>
  <div class="page-container payment-result">
    <el-icon class="result-icon" :class="resultClass">
      <CircleCheckFilled v-if="isPaid" />
      <Loading v-else-if="isPending" />
      <WarningFilled v-else />
    </el-icon>

    <h2>{{ resultTitle }}</h2>
    <p class="result-message">{{ resultMessage }}</p>
    <p v-if="errorMessage" class="result-error">{{ errorMessage }}</p>

    <div class="result-actions">
      <el-button v-if="isPending" :loading="loading" @click="confirmPayment">重新确认</el-button>
      <el-button type="primary" @click="goOrder">查看订单</el-button>
      <el-button @click="$router.push('/orders')">订单列表</el-button>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { CircleCheckFilled, Loading, WarningFilled } from '@element-plus/icons-vue'
import { orderApi } from '@/api/order'

const route = useRoute()
const router = useRouter()
const orderId = computed(() => route.query.orderId)
const order = ref(null)
const loading = ref(false)
const errorMessage = ref('')
const attempts = ref(0)
const maxAttempts = 10
let timer = null

const isPaid = computed(() => order.value?.status === 'PAID')
const isPending = computed(() => !order.value || order.value.status === 'PENDING_PAYMENT')
const resultClass = computed(() => {
  if (isPaid.value) return 'success'
  if (isPending.value) return 'pending'
  return 'warning'
})
const resultTitle = computed(() => {
  if (!orderId.value) return '缺少订单信息'
  if (isPaid.value) return '支付成功'
  if (isPending.value) return '支付确认中'
  return statusLabel(order.value.status)
})
const resultMessage = computed(() => {
  if (!orderId.value) return '未获取到订单号，请从订单列表查看支付结果'
  if (isPaid.value) return '后端已确认收款，订单状态已更新为已付款'
  if (isPending.value) return '系统正在向支付宝确认真实交易状态，请稍后'
  return '当前订单不是待付款状态，请进入订单详情查看最新结果'
})

function statusLabel(status) {
  return {
    PENDING_PAYMENT: '待付款',
    PAID: '已付款',
    SHIPPED: '已发货',
    COMPLETED: '已完成',
    CANCELLED: '已取消',
    REFUND_REQUESTED: '退款申请中',
    REFUNDED: '已退款',
    REFUND_REJECTED: '退款被拒'
  }[status] || status
}

async function confirmPayment() {
  if (!orderId.value) return
  loading.value = true
  try {
    const res = await orderApi.reconcilePayment(orderId.value)
    order.value = res.data
    errorMessage.value = ''
    if (order.value?.status !== 'PENDING_PAYMENT') {
      stopPolling()
    }
  } catch (err) {
    errorMessage.value = err?.message || '支付状态暂未确认，请稍后重试'
  } finally {
    loading.value = false
  }
}

function startPolling() {
  stopPolling()
  timer = setInterval(async () => {
    if (!isPending.value || attempts.value >= maxAttempts) {
      stopPolling()
      return
    }
    attempts.value += 1
    await confirmPayment()
  }, 3000)
}

function stopPolling() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

function goOrder() {
  if (orderId.value) {
    router.push(`/orders/${orderId.value}`)
  } else {
    router.push('/orders')
  }
}

onMounted(async () => {
  await confirmPayment()
  if (isPending.value) {
    startPolling()
  }
})

onUnmounted(stopPolling)
</script>

<style scoped>
.payment-result {
  text-align: center;
  padding-top: 60px;
}

.result-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.result-icon.success {
  color: #67c23a;
}

.result-icon.pending {
  color: #409eff;
}

.result-icon.warning {
  color: #e6a23c;
}

.result-message {
  color: #909399;
  margin-bottom: 12px;
}

.result-error {
  color: #e6a23c;
  margin-bottom: 20px;
}

.result-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}
</style>
