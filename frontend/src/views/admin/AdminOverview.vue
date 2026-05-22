<template>
  <div>
    <h2 style="margin:0 0 20px">数据总览</h2>
    <el-row :gutter="16">
      <el-col :span="6" v-for="card in cards" :key="card.label">
        <el-card shadow="never" style="text-align:center">
          <div style="font-size:13px;color:var(--text-muted);margin-bottom:8px">{{ card.label }}</div>
          <div style="font-size:28px;font-weight:bold;color:var(--text-primary)">{{ card.value }}</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api/admin'

const cards = ref([
  { label: '注册用户总数', value: '-' },
  { label: '上架商品数', value: '-' },
  { label: '今日新增订单', value: '-' },
  { label: '平台总成交额', value: '-' },
])

onMounted(async () => {
  try {
    const res = await adminApi.getOverview()
    const d = res.data
    cards.value[0].value = d.totalUsers
    cards.value[1].value = d.activeProducts
    cards.value[2].value = d.todayOrders
    cards.value[3].value = '¥' + Number(d.totalRevenue).toFixed(2)
  } catch {
    ElMessage.error('加载数据失败')
  }
})
</script>
