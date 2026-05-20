<template>
  <div class="page-container" v-if="order">
    <h2>订单详情</h2>
    <el-card style="margin-bottom:16px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="订单号">{{ order.id }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusType(order.status)">{{ statusLabel(order.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="收货地址" :span="2">{{ order.address }}</el-descriptions-item>
        <el-descriptions-item label="物流单号" v-if="order.trackingNumber">{{ order.trackingNumber }}</el-descriptions-item>
        <el-descriptions-item label="下单时间">{{ order.createdAt?.slice(0,19).replace('T',' ') }}</el-descriptions-item>
        <el-descriptions-item label="合计">
          <strong style="color:#f56c6c">¥{{ order.totalAmount?.toFixed(2) }}</strong>
        </el-descriptions-item>
      </el-descriptions>
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
      <el-button v-if="order.status === 'SHIPPED'" type="success" @click="confirmReceipt">确认收货</el-button>
      <el-button v-if="order.status === 'PENDING_PAYMENT'" type="danger" plain @click="cancelOrder">取消订单</el-button>
      <el-button @click="$router.push('/orders')">返回列表</el-button>
    </div>

    <el-dialog v-model="payDialog" title="模拟支付" width="360px">
      <p>确认支付 <strong style="color:#f56c6c">¥{{ order.totalAmount?.toFixed(2) }}</strong>？</p>
      <template #footer>
        <el-button @click="payDialog=false">取消</el-button>
        <el-button type="primary" :loading="payLoading" @click="doPay">确认支付</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { orderApi } from '@/api/order'

const route = useRoute()
const order = ref(null)
const payDialog = ref(false)
const payLoading = ref(false)

const statusLabel = s => ({ PENDING_PAYMENT:'待付款', PAID:'已付款', SHIPPED:'已发货', COMPLETED:'已完成', CANCELLED:'已取消' }[s] || s)
const statusType = s => ({ PENDING_PAYMENT:'warning', PAID:'primary', SHIPPED:'', COMPLETED:'success', CANCELLED:'info' }[s] || '')

async function load() {
  const res = await orderApi.detail(route.params.id)
  order.value = res.data
}

function openPayDialog() { payDialog.value = true }

async function doPay() {
  payLoading.value = true
  try {
    const res = await orderApi.pay(order.value.id)
    order.value = res.data
    payDialog.value = false
    ElMessage.success('支付成功')
  } catch (err) {
    ElMessage.error(err?.message || '支付失败')
  } finally {
    payLoading.value = false
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

onMounted(load)
</script>

<style scoped>
.actions { display: flex; gap: 12px; }
</style>
