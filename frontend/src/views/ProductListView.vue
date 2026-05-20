<template>
  <div class="page-container">
    <div class="filter-bar">
      <el-select v-model="categoryId" placeholder="全部分类" clearable @change="fetchProducts">
        <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <el-input v-model="keyword" placeholder="搜索商品..." clearable style="width:220px"
        @keyup.enter="fetchProducts" @clear="fetchProducts">
        <template #append><el-button @click="fetchProducts"><el-icon><Search /></el-icon></el-button></template>
      </el-input>
      <el-input v-model="shopKeyword" placeholder="按店铺名搜索..." clearable style="width:180px"
        @keyup.enter="searchByShop" @clear="clearShopFilter" />
    </div>

    <div v-if="loading" class="loading-wrap"><el-skeleton :rows="3" animated /></div>
    <div v-else-if="products.length === 0" class="empty">暂无相关商品</div>
    <div v-else class="product-grid">
      <ProductCard v-for="p in products" :key="p.id" :product="p" />
    </div>

    <el-pagination
      v-if="total > 0"
      background layout="prev, pager, next"
      :total="total" :page-size="pageSize"
      v-model:current-page="currentPage"
      @current-change="fetchProducts"
      style="margin-top:20px;justify-content:center;display:flex"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { productApi } from '@/api/product'
import { shopApi } from '@/api/shop'
import ProductCard from '@/components/ProductCard.vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const categories = ref([])
const products = ref([])
const loading = ref(false)
const total = ref(0)
const pageSize = 20
const currentPage = ref(1)
const categoryId = ref(null)
const keyword = ref('')
const shopKeyword = ref('')
const shopId = ref(null)

async function fetchProducts() {
  loading.value = true
  try {
    const res = await productApi.list({
      categoryId: categoryId.value || undefined,
      keyword: keyword.value || undefined,
      page: currentPage.value - 1,
      size: pageSize
    })
    products.value = res.data.content
    total.value = res.data.totalElements
  } catch (err) {
    ElMessage.error(err?.message || '加载商品失败')
  } finally {
    loading.value = false
  }
}

async function searchByShop() {
  if (!shopKeyword.value) return
  try {
    const res = await shopApi.searchShops(shopKeyword.value)
    if (res.data.length === 1) {
      router.push('/shops/' + res.data[0].id)
    } else if (res.data.length > 1) {
      router.push('/shops/' + res.data[0].id)
    } else {
      ElMessage.info('未找到相关店铺')
    }
  } catch {
    ElMessage.error('搜索店铺失败')
  }
}

function clearShopFilter() {
  shopKeyword.value = ''
}

onMounted(async () => {
  const catRes = await productApi.categories()
  categories.value = catRes.data
  await fetchProducts()
})
</script>

<style scoped>
.filter-bar { display: flex; gap: 12px; margin-bottom: 20px; flex-wrap: wrap; }
.product-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 16px; }
.loading-wrap { padding: 40px 0; }
.empty { text-align: center; padding: 60px; color: #999; }
</style>
