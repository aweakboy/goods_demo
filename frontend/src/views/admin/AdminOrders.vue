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

    <el-dialog v-model="detailVisible" title="订单详情" width="600px">
      <div v-if="detail">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="订单ID">{{ detail.id }}</el-descriptions-item>
          <el-descriptions-item label="买家">{{ detail.buyerUsername }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ statusLabel(detail.status) }}</el-descriptions-item>
          <el-descriptions-item label="金额">¥{{ detail.totalAmount?.toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="收货地址" :span="2">{{ detail.address }}</el-descriptions-item>
          <el-descriptions-item label="物流单号" :span="2">{{ detail.trackingNumber || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间" :span="2">{{ formatDate(detail.createdAt) }}</el-descriptions-item>
        </el-descriptions>
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api/admin'

const orders = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = 20
const filterStatus = ref(null)
const detailVisible = ref(false)
const detail = ref(null)

const STATUS_LABELS = { PENDING_PAYMENT: '待付款', PAID: '已付款', SHIPPED: '已发货', COMPLETED: '已完成', CANCELLED: '已取消' }
const STATUS_TYPES = { PENDING_PAYMENT: 'warning', PAID: 'primary', SHIPPED: 'success', COMPLETED: '', CANCELLED: 'info' }

function statusLabel(s) { return STATUS_LABELS[s] || s }
function statusType(s) { return STATUS_TYPES[s] || '' }
function formatDate(dt) { return dt ? dt.replace('T', ' ').slice(0, 16) : '' }

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

function onPageChange(page) {
  currentPage.value = page
  load()
}

onMounted(load)
</script>
