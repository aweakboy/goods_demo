<template>
  <div>
    <el-card style="margin-bottom:16px">
      <el-form :inline="true" :model="filters" @submit.prevent="fetchLogs">
        <el-form-item label="模块">
          <el-select v-model="filters.module" clearable placeholder="全部" style="width:120px">
            <el-option v-for="m in modules" :key="m" :label="m" :value="m" />
          </el-select>
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="filters.username" clearable placeholder="用户名" style="width:150px" />
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="filters.dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            style="width:340px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchLogs">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <el-table :data="logs" v-loading="loading" stripe>
        <el-table-column prop="module" label="模块" width="80" />
        <el-table-column prop="action" label="动作" width="120" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="resourceId" label="资源ID" width="100" />
        <el-table-column prop="ip" label="IP" width="140" />
        <el-table-column prop="detail" label="详情" min-width="120" />
        <el-table-column prop="createdAt" label="时间" width="180">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
      <div style="margin-top:16px;text-align:right">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          @current-change="fetchLogs"
          @size-change="fetchLogs"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { adminApi } from '@/api/admin'

const modules = ['用户', '订单', '支付', '退款', '管理']
const filters = reactive({ module: '', username: '', dateRange: null })
const logs = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)

async function fetchLogs() {
  loading.value = true
  try {
    const params = {
      page: page.value - 1,
      size: pageSize.value,
    }
    if (filters.module) params.module = filters.module
    if (filters.username) params.username = filters.username
    if (filters.dateRange && filters.dateRange[0]) {
      params.startTime = filters.dateRange[0].toISOString()
      params.endTime = filters.dateRange[1].toISOString()
    }
    const res = await adminApi.getLogs(params)
    logs.value = res.data.content
    total.value = res.data.totalElements
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.module = ''
  filters.username = ''
  filters.dateRange = null
  page.value = 1
  fetchLogs()
}

function formatTime(t) {
  if (!t) return '-'
  return new Date(t).toLocaleString('zh-CN', { hour12: false })
}

onMounted(fetchLogs)
</script>
