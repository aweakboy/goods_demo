<template>
  <el-header class="header">
    <div class="header-inner">
      <router-link to="/" class="logo">优选商城</router-link>
      <div class="nav-links">
        <router-link v-if="userStore.role !== 'SELLER'" to="/products">商品</router-link>
        <template v-if="userStore.isLoggedIn">
          <router-link v-if="userStore.role === 'BUYER'" to="/cart">
            <el-badge :value="cartStore.count || null">购物车</el-badge>
          </router-link>
          <router-link v-if="userStore.role === 'BUYER'" to="/orders">我的订单</router-link>
          <router-link v-if="userStore.role === 'BUYER'" to="/addresses">我的地址</router-link>
          <router-link v-if="userStore.role === 'BUYER'" to="/coupons">我的优惠券</router-link>
          <router-link v-if="userStore.role === 'BUYER'" to="/membership">会员中心</router-link>
          <router-link v-if="userStore.role === 'BUYER'" to="/favorite-shops">收藏店铺</router-link>
          <router-link v-if="userStore.role === 'BUYER'" to="/price-alerts">降价提醒</router-link>
          <router-link v-if="userStore.role === 'BUYER'" to="/notifications">
            <el-badge :value="unreadCount || null">通知</el-badge>
          </router-link>
          <router-link v-if="userStore.role === 'SELLER'" to="/seller/shop">我的店铺</router-link>
          <router-link v-if="userStore.role === 'SELLER'" to="/seller/products">商品管理</router-link>
          <router-link v-if="userStore.role === 'SELLER'" to="/seller/orders">订单管理</router-link>
          <router-link v-if="userStore.role === 'ADMIN'" to="/admin/overview">管理后台</router-link>
          <el-dropdown @command="handleCommand">
            <span class="user-name">{{ userStore.username }} <el-icon><ArrowDown /></el-icon></span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <router-link to="/login">登录</router-link>
          <router-link to="/register">注册</router-link>
        </template>
        <el-tooltip :content="isDark ? '切换到浅色模式' : '切换到深色模式'" placement="bottom">
          <el-button class="theme-toggle" circle text aria-label="Toggle theme" @click="toggleTheme">
            <el-icon>
              <Sunny v-if="isDark" />
              <Moon v-else />
            </el-icon>
          </el-button>
        </el-tooltip>
      </div>
    </div>
  </el-header>
</template>

<script setup>
import { useUserStore } from '@/stores/user'
import { useCartStore } from '@/stores/cart'
import { useRouter } from 'vue-router'
import { onMounted, ref, watch } from 'vue'
import { Moon, Sunny } from '@element-plus/icons-vue'
import { useTheme } from '@/composables/useTheme'
import { notificationApi } from '@/api/notification'

const userStore = useUserStore()
const cartStore = useCartStore()
const router = useRouter()
const { isDark, toggleTheme } = useTheme()
const unreadCount = ref(0)

async function loadUnreadCount() {
  if (userStore.role !== 'BUYER') {
    unreadCount.value = 0
    return
  }
  try {
    const res = await notificationApi.unreadCount()
    unreadCount.value = res.data.unreadCount || 0
  } catch {
    unreadCount.value = 0
  }
}

function handleCommand(cmd) {
  if (cmd === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}

onMounted(loadUnreadCount)
watch(() => userStore.role, loadUnreadCount)
</script>

<style scoped>
.header {
  background: var(--header-bg);
  box-shadow: 0 2px 12px rgba(0,0,0,0.2);
  position: sticky;
  top: 0;
  z-index: 100;
}
.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.logo {
  font-size: 20px;
  font-weight: 700;
  color: var(--header-text);
  text-decoration: none;
  letter-spacing: 0.5px;
}
.logo:hover { color: var(--header-text); }
.nav-links {
  display: flex;
  align-items: center;
  gap: 20px;
}
.nav-links a {
  text-decoration: none;
  color: var(--header-text-muted);
  font-size: 14px;
  padding-bottom: 2px;
  border-bottom: 2px solid transparent;
  transition: color 0.15s, border-color 0.15s;
}
.nav-links a:hover { color: var(--header-text); }
.nav-links a.router-link-active {
  color: var(--header-text);
  border-bottom-color: var(--brand-primary);
}
.user-name {
  cursor: pointer;
  color: var(--header-text-muted);
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
}
.user-name:hover { color: var(--header-text); }
.theme-toggle {
  color: var(--header-text-muted);
  font-size: 16px;
}
.theme-toggle:hover,
.theme-toggle:focus {
  color: var(--header-text);
  background: rgba(255, 255, 255, 0.14);
}
</style>
