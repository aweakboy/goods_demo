<template>
  <div class="page-container">
    <h2>我的订单</h2>
    <el-tabs v-model="activeTab" @tab-change="fetchOrders">
      <el-tab-pane label="全部" name="" />
      <el-tab-pane label="待付款" name="PENDING_PAYMENT" />
      <el-tab-pane label="已付款" name="PAID" />
      <el-tab-pane label="已发货" name="SHIPPED" />
      <el-tab-pane label="已完成" name="COMPLETED" />
      <el-tab-pane label="已取消" name="CANCELLED" />
    </el-tabs>

    <div v-if="orders.length === 0" class="empty"><el-empty description="暂无订单" /></div>
    <el-card v-for="order in orders" :key="order.id" class="order-card" @click="$router.push('/orders/' + order.id)">
      <div class="order-header">
        <span>订单号：{{ order.id }}</span>
        <el-tag :type="statusType(order.status)">{{ statusLabel(order.status) }}</el-tag>
      </div>
      <div class="order-footer">
        <span>{{ order.createdAt?.slice(0,10) }}</span>
        <strong style="color:#f56c6c">合计：¥{{ order.totalAmount?.toFixed(2) }}</strong>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { orderApi } from '@/api/order'

const orders = ref([])
const activeTab = ref('')

const statusLabel = s => ({ PENDING_PAYMENT:'待付款', PAID:'已付款', SHIPPED:'已发货', COMPLETED:'已完成', CANCELLED:'已取消' }[s] || s)
const statusType = s => ({ PENDING_PAYMENT:'warning', PAID:'primary', SHIPPED:'', COMPLETED:'success', CANCELLED:'info' }[s] || '')

async function fetchOrders() {
  const res = await orderApi.list(activeTab.value ? { status: activeTab.value } : {})
  orders.value = res.data
}

onMounted(fetchOrders)
</script>

<style scoped>
.empty { padding: 40px 0; }
.order-card { margin-bottom: 12px; cursor: pointer; }
.order-card:hover { border-color: #409eff; }
.order-header, .order-footer { display: flex; justify-content: space-between; align-items: center; }
.order-footer { margin-top: 8px; color: #666; }
</style>
