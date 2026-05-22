<template>
  <div class="page-container" v-if="shop">
    <el-card style="margin-bottom:24px">
      <div class="shop-heading">
        <div>
          <h2 style="margin:0 0 8px">{{ shop.name }}</h2>
          <p class="favorite-count">{{ shop.favoriteCount || 0 }} 人收藏</p>
        </div>
        <el-button
          v-if="userStore.role === 'BUYER'"
          :type="shop.favorited ? 'default' : 'primary'"
          :loading="favoriteLoading"
          @click="toggleFavorite"
        >
          {{ shop.favorited ? '取消收藏' : '收藏店铺' }}
        </el-button>
      </div>
      <p style="color:var(--text-secondary);margin:0">{{ shop.description || '暂无简介' }}</p>
      <p v-if="shop.fullAddress" style="color:var(--text-secondary);margin:8px 0 0">店铺地址：{{ shop.fullAddress }}</p>
      <AddressMap
        v-if="shop.addressValidationStatus === 'VALID'"
        style="margin-top:16px"
        :longitude="shop.longitude"
        :latitude="shop.latitude"
        :title="shop.name"
        :address="shop.fullAddress"
      />
    </el-card>

    <div v-if="loading" class="loading-wrap"><el-skeleton :rows="3" animated /></div>
    <div v-else-if="products.length === 0" class="empty">该店铺暂无商品</div>
    <div v-else class="product-grid">
      <ProductCard v-for="p in products" :key="p.id" :product="p" />
    </div>

    <el-pagination
      v-if="total > 0"
      background layout="prev, pager, next"
      :total="total" :page-size="pageSize"
      v-model:current-page="currentPage"
      @current-change="loadProducts"
      style="margin-top:20px;justify-content:center;display:flex"
    />
  </div>
  <div v-else-if="notFound" class="page-container"><el-empty description="店铺不存在或已关闭" /></div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { shopApi } from '@/api/shop'
import { useUserStore } from '@/stores/user'
import ProductCard from '@/components/ProductCard.vue'
import AddressMap from '@/components/AddressMap.vue'

const route = useRoute()
const userStore = useUserStore()
const shop = ref(null)
const products = ref([])
const total = ref(0)
const loading = ref(false)
const favoriteLoading = ref(false)
const notFound = ref(false)
const currentPage = ref(1)
const pageSize = 20

async function loadProducts() {
  loading.value = true
  try {
    const res = await shopApi.getStorefront(route.params.id, { page: currentPage.value - 1, size: pageSize })
    shop.value = res.data
    products.value = res.data.products?.content || []
    total.value = res.data.products?.totalElements || 0
  } catch (err) {
    if (err?.code === 404 || err?.status === 404) {
      notFound.value = true
    } else {
      ElMessage.error(err?.message || '加载失败')
    }
  } finally {
    loading.value = false
  }
}

async function toggleFavorite() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  favoriteLoading.value = true
  try {
    const res = shop.value.favorited
      ? await shopApi.unfavorite(shop.value.id)
      : await shopApi.favorite(shop.value.id)
    shop.value.favorited = res.data.favorited
    shop.value.favoriteCount = res.data.favoriteCount
    ElMessage.success(shop.value.favorited ? '已收藏店铺' : '已取消收藏')
  } catch (err) {
    ElMessage.error(err?.message || '操作失败')
  } finally {
    favoriteLoading.value = false
  }
}

onMounted(loadProducts)
</script>

<style scoped>
.shop-heading {
  align-items: flex-start;
  display: flex;
  gap: 16px;
  justify-content: space-between;
}
.favorite-count {
  color: var(--text-secondary);
  margin: 0;
}
.product-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 16px; }
.loading-wrap { padding: 40px 0; }
.empty { text-align: center; padding: 60px; color: var(--text-muted); }
@media (max-width: 640px) {
  .shop-heading {
    flex-direction: column;
  }
}
</style>
