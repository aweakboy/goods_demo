<template>
  <div>
    <div style="display:flex;gap:12px;margin-bottom:16px;align-items:center">
      <h2 style="margin:0">店铺管理</h2>
      <el-select v-model="filterStatus" placeholder="状态筛选" clearable style="width:120px" @change="load">
        <el-option label="营业中" value="ACTIVE" />
        <el-option label="已关闭" value="INACTIVE" />
      </el-select>
      <el-button @click="load">刷新</el-button>
    </div>
    <el-table :data="shops" style="width:100%">
      <el-table-column label="ID" prop="id" width="70" />
      <el-table-column label="店铺名称" prop="name" min-width="180" />
      <el-table-column label="卖家" prop="sellerUsername" width="130" />
      <el-table-column label="商品数" prop="productCount" width="90" />
      <el-table-column label="状态" width="100">
        <template #default="{row}">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" size="small">
            {{ row.status === 'ACTIVE' ? '营业中' : '已关闭' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="注册时间" width="120">
        <template #default="{row}">{{ row.createdAt?.slice(0,10) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{row}">
          <el-button
            link
            :type="row.status === 'ACTIVE' ? 'warning' : 'success'"
            @click="toggleStatus(row)"
          >
            {{ row.status === 'ACTIVE' ? '关闭店铺' : '恢复营业' }}
          </el-button>
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api/admin'

const shops = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = 20
const filterStatus = ref(null)

async function load() {
  try {
    const res = await adminApi.getShops({
      status: filterStatus.value || undefined,
      page: currentPage.value - 1,
      size: pageSize
    })
    shops.value = res.data.content
    total.value = res.data.totalElements
  } catch {
    ElMessage.error('加载店铺列表失败')
  }
}

async function toggleStatus(row) {
  const newStatus = row.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
  try {
    await adminApi.updateShopStatus(row.id, newStatus)
    row.status = newStatus
    ElMessage.success(newStatus === 'ACTIVE' ? '店铺已恢复营业' : '店铺已关闭')
  } catch (err) {
    ElMessage.error(err?.message || '操作失败')
  }
}

function onPageChange(page) {
  currentPage.value = page
  load()
}

onMounted(load)
</script>
