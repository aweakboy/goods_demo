<template>
  <el-card class="product-card" shadow="hover" @click="$router.push('/products/' + product.id)">
    <el-image :src="product.imageUrl || '/placeholder.png'" fit="cover" class="product-img" />
    <div class="product-info">
      <p class="product-name">{{ product.name }}</p>
      <p class="product-price">¥{{ product.price?.toFixed(2) }}</p>
      <p v-if="product.shopName" class="shop-name" @click.stop="$router.push('/shops/' + product.shopId)">
        {{ product.shopName }}
      </p>
      <el-tag v-if="product.stock === 0" type="danger" size="small">已售罄</el-tag>
      <el-tag v-else-if="product.stock <= 10" type="warning" size="small">库存紧张</el-tag>
      <el-tag v-else type="success" size="small">有货</el-tag>
    </div>
  </el-card>
</template>

<script setup>
defineProps({ product: Object })
</script>

<style scoped>
.product-card { cursor: pointer; }
.product-img { width: 100%; height: 180px; display: block; }
.product-info { padding: 10px 0 0; }
.product-name { margin: 0 0 6px; font-size: 14px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.product-price { margin: 0 0 4px; font-size: 16px; font-weight: bold; color: #f56c6c; }
.shop-name { margin: 0 0 6px; font-size: 12px; color: #409eff; cursor: pointer; }
.shop-name:hover { text-decoration: underline; }
</style>
