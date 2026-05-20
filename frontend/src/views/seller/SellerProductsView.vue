<template>
  <div class="page-container">
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <h2>商品管理</h2>
      <el-button type="primary" @click="$router.push('/seller/products/new')">上架新商品</el-button>
    </div>
    <el-table :data="products" style="width:100%">
      <el-table-column label="商品名称" prop="name" min-width="180" />
      <el-table-column label="价格" width="100">
        <template #default="{row}">¥{{ row.price?.toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="库存" width="100">
        <template #default="{row}">
          <span :style="row.stock <= 10 ? 'color:#e6a23c;font-weight:bold' : ''">{{ row.stock }}</span>
          <el-tag v-if="row.stock <= 10" type="warning" size="small" style="margin-left:4px">低库存</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{row}">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
            {{ row.status === 'ACTIVE' ? '上架中' : '已下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{row}">
          <el-button link type="primary" @click="$router.push(`/seller/products/${row.id}/edit`)">编辑</el-button>
          <el-button link :type="row.status === 'ACTIVE' ? 'warning' : 'success'" @click="toggleStatus(row)">
            {{ row.status === 'ACTIVE' ? '下架' : '上架' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted, onActivated } from 'vue'
import { ElMessage } from 'element-plus'
import { productApi } from '@/api/product'

const products = ref([])

async function load() {
  try {
    const res = await productApi.sellerList()
    products.value = res.data
  } catch (err) {
    ElMessage.error(err?.message || '加载商品列表失败')
  }
}

async function toggleStatus(row) {
  const newStatus = row.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
  await productApi.update(row.id, {
    name: row.name, description: row.description, price: row.price,
    stock: row.stock, categoryId: row.categoryId, imageUrl: row.imageUrl,
    status: newStatus
  })
  row.status = newStatus
  ElMessage.success(newStatus === 'ACTIVE' ? '已上架' : '已下架')
}

onMounted(load)
onActivated(load)
</script>
