<template>
  <div class="page-container">
    <h2>降价提醒</h2>
    <el-table :data="alerts" v-loading="loading" style="width:100%">
      <el-table-column label="商品" min-width="180">
        <template #default="{row}">
          <router-link :to="`/products/${row.productId}`">{{ row.productName || `商品 ${row.productId}` }}</router-link>
        </template>
      </el-table-column>
      <el-table-column label="当前价" width="110">
        <template #default="{row}">¥{{ money(row.currentPrice) }}</template>
      </el-table-column>
      <el-table-column label="目标价" width="110">
        <template #default="{row}">¥{{ money(row.targetPrice) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{row}">
          <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="更新时间" width="170">
        <template #default="{row}">{{ formatDate(row.updatedAt || row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="110">
        <template #default="{row}">
          <el-button v-if="row.status === 'ACTIVE'" link type="danger" @click="cancel(row)">取消提醒</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-if="total > 0"
      background
      layout="prev, pager, next"
      :total="total"
      :page-size="pageSize"
      v-model:current-page="currentPage"
      @current-change="load"
      style="margin-top:20px;justify-content:center;display:flex"
    />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { priceAlertApi } from '@/api/priceAlert'

const alerts = ref([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = 20

function money(value) {
  return Number(value || 0).toFixed(2)
}

function formatDate(value) {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}

function statusLabel(status) {
  return {
    ACTIVE: '等待降价',
    TRIGGERED: '已通知',
    CANCELLED: '已取消'
  }[status] || status
}

function statusType(status) {
  return {
    ACTIVE: 'warning',
    TRIGGERED: 'success',
    CANCELLED: 'info'
  }[status] || ''
}

async function load() {
  loading.value = true
  try {
    const res = await priceAlertApi.list({ page: currentPage.value - 1, size: pageSize })
    alerts.value = res.data.content || []
    total.value = res.data.totalElements || 0
  } catch (err) {
    ElMessage.error(err?.message || '加载降价提醒失败')
  } finally {
    loading.value = false
  }
}

async function cancel(row) {
  try {
    await priceAlertApi.cancel(row.productId)
    ElMessage.success('降价提醒已取消')
    await load()
  } catch (err) {
    ElMessage.error(err?.message || '取消提醒失败')
  }
}

onMounted(load)
</script>
