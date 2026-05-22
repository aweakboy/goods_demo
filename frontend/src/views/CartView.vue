<template>
  <div class="page-container">
    <h2>购物车</h2>
    <div v-if="cartStore.items.length === 0" class="empty">
      <el-empty description="购物车为空">
        <el-button type="primary" @click="$router.push('/products')">去购物</el-button>
      </el-empty>
    </div>
    <template v-else>
      <el-table :data="cartStore.items" style="width:100%">
        <el-table-column label="商品" min-width="200">
          <template #default="{row}">
            <div style="display:flex;align-items:center;gap:10px">
              <el-image :src="row.product?.imageUrl || '/placeholder.png'" style="width:60px;height:60px" fit="cover" />
              <span>{{ row.product?.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="单价" width="120">
          <template #default="{row}">¥{{ row.product?.price?.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="数量" width="160">
          <template #default="{row}">
            <el-input-number :model-value="row.quantity" :min="0" :max="row.product?.stock"
              @change="val => updateQty(row, val)" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="小计" width="120">
          <template #default="{row}">¥{{ (row.product?.price * row.quantity).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{row}">
            <el-button type="danger" link @click="removeItem(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="cart-footer">
        <el-button type="danger" plain @click="clearCart">清空购物车</el-button>
        <div>
          <span style="font-size:16px">合计：<strong style="color:#f56c6c;font-size:20px">¥{{ cartStore.total.toFixed(2) }}</strong></span>
          <el-button type="primary" style="margin-left:16px" @click="$router.push('/checkout')">去结算</el-button>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cartApi } from '@/api/cart'
import { useCartStore } from '@/stores/cart'

const cartStore = useCartStore()

async function loadCart() {
  const res = await cartApi.list()
  cartStore.setItems(res.data)
}

async function updateQty(item, val) {
  try {
    await cartApi.update(item.id, val)
    await loadCart()
  } catch (err) {
    ElMessage.error(err?.message || '更新失败')
  }
}

async function removeItem(id) {
  await cartApi.remove(id)
  await loadCart()
}

async function clearCart() {
  await ElMessageBox.confirm('确认清空购物车？', '提示', { type: 'warning' })
  await cartApi.clear()
  cartStore.clear()
}

onMounted(loadCart)
</script>

<style scoped>
.empty { padding: 60px 0; }
.cart-footer { display: flex; justify-content: space-between; align-items: center; margin-top: 20px; padding-top: 16px; border-top: 1px solid var(--border-color); }
</style>
