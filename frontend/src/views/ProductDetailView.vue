<template>
  <div class="page-container" v-if="product">
    <el-row :gutter="40">
      <el-col :span="10">
        <el-image :src="product.imageUrl || '/placeholder.png'" fit="contain" style="width:100%;height:360px;background:#f5f5f5" />
      </el-col>
      <el-col :span="14">
        <h2>{{ product.name }}</h2>
        <p class="price">¥{{ product.price?.toFixed(2) }}</p>
        <p>库存：{{ product.stock }} 件</p>
        <p v-if="product.shopName" style="margin:4px 0">
          店铺：<router-link :to="'/shops/' + product.shopId" style="color:#409eff">{{ product.shopName }}</router-link>
        </p>
        <p style="color:#666">{{ product.description }}</p>
        <el-divider />
        <div style="display:flex;align-items:center;gap:16px">
          <el-input-number v-model="qty" :min="1" :max="product.stock" />
          <el-button type="primary" :disabled="product.stock === 0" @click="addToCart">加入购物车</el-button>
        </div>
      </el-col>
    </el-row>
  </div>
  <div v-else-if="notFound" class="page-container"><el-empty description="商品不存在或已下架" /></div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { productApi } from '@/api/product'
import { cartApi } from '@/api/cart'
import { useUserStore } from '@/stores/user'
import { useCartStore } from '@/stores/cart'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()
const product = ref(null)
const notFound = ref(false)
const qty = ref(1)

onMounted(async () => {
  try {
    const res = await productApi.detail(route.params.id)
    product.value = res.data
  } catch {
    notFound.value = true
  }
})

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
.price { font-size: 28px; color: #f56c6c; font-weight: bold; margin: 8px 0; }
</style>
