<template>
  <div class="page-container">
    <h2>确认订单</h2>
    <el-card style="margin-bottom:20px">
      <h3>商品清单</h3>
      <el-table :data="cartStore.items" style="width:100%">
        <el-table-column label="商品" prop="product.name" min-width="200" />
        <el-table-column label="单价" width="120">
          <template #default="{row}">¥{{ row.product?.price?.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="数量" prop="quantity" width="80" />
        <el-table-column label="小计" width="120">
          <template #default="{row}">¥{{ (row.product?.price * row.quantity).toFixed(2) }}</template>
        </el-table-column>
      </el-table>
      <div style="text-align:right;margin-top:12px;font-size:16px">
        合计：<strong style="color:#f56c6c;font-size:20px">¥{{ cartStore.total.toFixed(2) }}</strong>
      </div>
    </el-card>

    <el-card>
      <h3>收货信息</h3>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="收货地址" prop="address">
          <el-input v-model="form.address" type="textarea" :rows="3" placeholder="请输入详细收货地址" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="submitOrder">提交订单</el-button>
          <el-button @click="$router.push('/cart')">返回购物车</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { orderApi } from '@/api/order'
import { useCartStore } from '@/stores/cart'

const router = useRouter()
const cartStore = useCartStore()
const formRef = ref()
const loading = ref(false)
const form = reactive({ address: '' })
const rules = { address: [{ required: true, message: '请输入收货地址', trigger: 'blur' }] }

async function submitOrder() {
  await formRef.value.validate()
  loading.value = true
  try {
    const res = await orderApi.create({ address: form.address })
    cartStore.clear()
    ElMessage.success('订单创建成功')
    router.push(`/orders/${res.data.id}`)
  } catch (err) {
    ElMessage.error(err?.message || '下单失败')
  } finally {
    loading.value = false
  }
}
</script>
