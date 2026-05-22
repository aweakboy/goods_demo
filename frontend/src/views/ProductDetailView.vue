<template>
  <div class="page-container" v-if="product">
    <el-row :gutter="40">
      <el-col :span="10">
        <el-image :src="product.imageUrl || '/placeholder.png'" fit="contain" style="width:100%;height:360px;background:var(--surface-bg-soft)" />
      </el-col>
      <el-col :span="14">
        <h2>{{ product.name }}</h2>
        <p class="price">¥{{ product.price?.toFixed(2) }}</p>
        <p>库存：{{ product.stock }} 件</p>
        <p v-if="product.shopName" style="margin:4px 0">
          店铺：<router-link :to="'/shops/' + product.shopId" style="color:var(--el-color-primary)">{{ product.shopName }}</router-link>
        </p>
        <p v-if="userStore.role === 'BUYER' && priceAlert" class="alert-summary">
          降价提醒：目标价 ¥{{ money(priceAlert.targetPrice) }}，{{ alertStatusLabel(priceAlert.status) }}
        </p>
        <p style="color:var(--text-secondary)">{{ product.description }}</p>
        <el-divider />
        <div style="display:flex;align-items:center;gap:16px">
          <el-input-number v-model="qty" :min="1" :max="product.stock" />
          <el-button type="primary" :disabled="product.stock === 0" @click="addToCart">加入购物车</el-button>
          <el-button v-if="userStore.role === 'BUYER'" @click="openAlertDialog">
            {{ priceAlert?.status === 'ACTIVE' ? '更新降价提醒' : '设置降价提醒' }}
          </el-button>
        </div>
      </el-col>
    </el-row>

    <el-dialog v-model="alertDialogVisible" title="设置降价提醒" width="420px">
      <el-form label-width="90px">
        <el-form-item label="当前价格">
          ¥{{ money(product.price) }}
        </el-form-item>
        <el-form-item label="目标价">
          <el-input-number
            v-model="targetPrice"
            :min="0.01"
            :max="Number(product.price || 0)"
            :precision="2"
            :step="1"
            style="width:180px"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="alertDialogVisible=false">取消</el-button>
        <el-button
          v-if="priceAlert"
          :loading="alertSaving"
          @click="cancelAlert"
        >
          取消提醒
        </el-button>
        <el-button type="primary" :loading="alertSaving" @click="saveAlert">保存</el-button>
      </template>
    </el-dialog>
  </div>
  <div v-else-if="notFound" class="page-container"><el-empty description="商品不存在或已下架" /></div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { productApi } from '@/api/product'
import { cartApi } from '@/api/cart'
import { priceAlertApi } from '@/api/priceAlert'
import { useUserStore } from '@/stores/user'
import { useCartStore } from '@/stores/cart'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()
const product = ref(null)
const notFound = ref(false)
const qty = ref(1)
const priceAlert = ref(null)
const alertDialogVisible = ref(false)
const alertSaving = ref(false)
const targetPrice = ref(null)

onMounted(async () => {
  try {
    const res = await productApi.detail(route.params.id)
    product.value = res.data
    if (userStore.role === 'BUYER') {
      await loadAlert()
    }
  } catch {
    notFound.value = true
  }
})

function money(value) {
  return Number(value || 0).toFixed(2)
}

function alertStatusLabel(status) {
  return {
    ACTIVE: '等待降价',
    TRIGGERED: '已通知',
    CANCELLED: '已取消'
  }[status] || status
}

async function loadAlert() {
  try {
    const res = await priceAlertApi.current(route.params.id)
    priceAlert.value = res.data
  } catch {
    priceAlert.value = null
  }
}

function openAlertDialog() {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  targetPrice.value = Number(priceAlert.value?.targetPrice || product.value.price || 0)
  if (!priceAlert.value && Number(product.value.price || 0) > 1) {
    targetPrice.value = Number(product.value.price) - 1
  }
  alertDialogVisible.value = true
}

async function saveAlert() {
  alertSaving.value = true
  try {
    const res = await priceAlertApi.save(product.value.id, { targetPrice: targetPrice.value })
    priceAlert.value = res.data
    alertDialogVisible.value = false
    ElMessage.success('降价提醒已保存')
  } catch (err) {
    ElMessage.error(err?.message || '保存降价提醒失败')
  } finally {
    alertSaving.value = false
  }
}

async function cancelAlert() {
  alertSaving.value = true
  try {
    await priceAlertApi.cancel(product.value.id)
    priceAlert.value = { ...priceAlert.value, status: 'CANCELLED' }
    alertDialogVisible.value = false
    ElMessage.success('降价提醒已取消')
  } catch (err) {
    ElMessage.error(err?.message || '取消降价提醒失败')
  } finally {
    alertSaving.value = false
  }
}

async function addToCart() {
  if (!userStore.isLoggedIn) { router.push('/login'); return }
  if (userStore.role !== 'BUYER') { ElMessage.warning('卖家无法购买商品'); return }
  try {
    await cartApi.add({ productId: product.value.id, quantity: qty.value })
    const cartRes = await cartApi.list()
    cartStore.setItems(cartRes.data)
    ElMessage.success('已加入购物车')
  } catch (err) {
    ElMessage.error(err?.message || '加入购物车失败')
  }
}
</script>

<style scoped>
.price { font-size: 28px; color: var(--el-color-danger); font-weight: bold; margin: 8px 0; }
.alert-summary { color: var(--el-color-primary); margin: 4px 0; }
</style>
