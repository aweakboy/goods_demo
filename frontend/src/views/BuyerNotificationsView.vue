<template>
  <div class="page-container">
    <div class="title-row">
      <h2>通知中心</h2>
      <el-button type="primary" :disabled="unreadCount === 0" @click="markAllRead">全部已读</el-button>
    </div>
    <p class="muted">未读通知：{{ unreadCount }}</p>
    <el-table :data="notifications" v-loading="loading" style="width:100%">
      <el-table-column label="状态" width="90">
        <template #default="{row}">
          <el-tag :type="row.read ? 'info' : 'danger'" size="small">{{ row.read ? '已读' : '未读' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="标题" prop="title" min-width="140" />
      <el-table-column label="内容" prop="content" min-width="240" />
      <el-table-column label="关联商品" min-width="150">
        <template #default="{row}">
          <router-link v-if="row.productId" :to="`/products/${row.productId}`">{{ row.productName || row.productId }}</router-link>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="时间" width="170">
        <template #default="{row}">{{ formatDate(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="{row}">
          <el-button v-if="!row.read" link type="primary" @click="markRead(row)">标记已读</el-button>
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
import { notificationApi } from '@/api/notification'

const notifications = ref([])
const loading = ref(false)
const unreadCount = ref(0)
const total = ref(0)
const currentPage = ref(1)
const pageSize = 20

function formatDate(value) {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}

async function loadUnreadCount() {
  const res = await notificationApi.unreadCount()
  unreadCount.value = res.data.unreadCount || 0
}

async function load() {
  loading.value = true
  try {
    const res = await notificationApi.list({ page: currentPage.value - 1, size: pageSize })
    notifications.value = res.data.content || []
    total.value = res.data.totalElements || 0
    await loadUnreadCount()
  } catch (err) {
    ElMessage.error(err?.message || '加载通知失败')
  } finally {
    loading.value = false
  }
}

async function markRead(row) {
  try {
    await notificationApi.markRead(row.id)
    await load()
  } catch (err) {
    ElMessage.error(err?.message || '标记已读失败')
  }
}

async function markAllRead() {
  try {
    await notificationApi.markAllRead()
    await load()
  } catch (err) {
    ElMessage.error(err?.message || '全部已读失败')
  }
}

onMounted(load)
</script>

<style scoped>
.title-row {
  align-items: center;
  display: flex;
  justify-content: space-between;
  gap: 16px;
}
.muted {
  color: var(--text-secondary);
}
</style>
