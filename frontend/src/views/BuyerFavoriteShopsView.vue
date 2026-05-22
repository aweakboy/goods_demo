<template>
  <div class="page-container">
    <h2>收藏店铺</h2>
    <el-table :data="shops" v-loading="loading" style="width:100%">
      <el-table-column label="店铺" min-width="180">
        <template #default="{row}">
          <router-link v-if="row.accessible" :to="`/shops/${row.shopId}`">{{ row.shopName }}</router-link>
          <span v-else>{{ row.shopName || '店铺已不可用' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="简介" prop="shopDescription" min-width="220" />
      <el-table-column label="状态" width="100">
        <template #default="{row}">
          <el-tag :type="row.accessible ? 'success' : 'info'" size="small">
            {{ row.accessible ? '可访问' : '已关闭' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="收藏时间" width="170">
        <template #default="{row}">{{ formatDate(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="110">
        <template #default="{row}">
          <el-button link type="danger" @click="unfavorite(row)">取消收藏</el-button>
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
import { shopApi } from '@/api/shop'

const shops = ref([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = 20

function formatDate(value) {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}

async function load() {
  loading.value = true
  try {
    const res = await shopApi.favorites({ page: currentPage.value - 1, size: pageSize })
    shops.value = res.data.content || []
    total.value = res.data.totalElements || 0
  } catch (err) {
    ElMessage.error(err?.message || '加载收藏店铺失败')
  } finally {
    loading.value = false
  }
}

async function unfavorite(row) {
  try {
    await shopApi.unfavorite(row.shopId)
    ElMessage.success('已取消收藏')
    await load()
  } catch (err) {
    ElMessage.error(err?.message || '取消收藏失败')
  }
}

onMounted(load)
</script>
