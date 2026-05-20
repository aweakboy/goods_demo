<template>
  <div>
    <div style="display:flex;gap:12px;margin-bottom:16px;align-items:center">
      <h2 style="margin:0">商品审核</h2>
      <el-select v-model="filterStatus" placeholder="状态筛选" clearable style="width:120px" @change="load">
        <el-option label="上架中" value="ACTIVE" />
        <el-option label="已下架" value="INACTIVE" />
      </el-select>
      <el-input v-model="filterSeller" placeholder="卖家用户名" clearable style="width:140px" @clear="load" @keyup.enter="load" />
      <el-input v-model="filterShop" placeholder="店铺名称" clearable style="width:140px" @clear="load" @keyup.enter="load" />
      <el-button @click="load">搜索</el-button>
    </div>
    <el-table :data="products" style="width:100%">
      <el-table-column label="ID" prop="id" width="70" />
      <el-table-column label="商品名称" prop="name" min-width="180" />
      <el-table-column label="价格" width="100">
        <template #default="{row}">¥{{ row.price?.toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="库存" prop="stock" width="80" />
      <el-table-column label="卖家" prop="sellerUsername" width="110" />
      <el-table-column label="店铺" prop="shopName" width="120">
        <template #default="{row}">{{ row.shopName || '-' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{row}">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
            {{ row.status === 'ACTIVE' ? '上架中' : '已下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{row}">
          <el-button
            link
            :type="row.status === 'ACTIVE' ? 'warning' : 'success'"
            @click="toggleStatus(row)"
          >
            {{ row.status === 'ACTIVE' ? '强制下架' : '恢复上架' }}
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

const products = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = 20
const filterStatus = ref(null)
const filterSeller = ref('')
const filterShop = ref('')

async function load() {
  try {
    const res = await adminApi.getProducts({
      status: filterStatus.value || undefined,
      sellerName: filterSeller.value || undefined,
      shopName: filterShop.value || undefined,
      page: currentPage.value - 1,
      size: pageSize
    })
    products.value = res.data.content
    total.value = res.data.totalElements
  } catch {
    ElMessage.error('加载商品列表失败')
  }
}

async function toggleStatus(row) {
  const newStatus = row.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
  try {
    await adminApi.updateProductStatus(row.id, newStatus)
    row.status = newStatus
    ElMessage.success(newStatus === 'ACTIVE' ? '已恢复上架' : '已下架')
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
