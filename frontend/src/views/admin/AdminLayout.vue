<template>
  <el-container class="admin-layout">
    <el-aside width="200px" class="admin-aside">
      <div class="admin-title">
        管理后台
      </div>
      <el-menu
        :default-active="$route.path"
        router
        background-color="var(--admin-sidebar-bg)"
        text-color="var(--admin-sidebar-text)"
        active-text-color="var(--admin-sidebar-active)"
      >
        <el-menu-item index="/admin/overview">
          <el-icon><DataAnalysis /></el-icon>数据总览
        </el-menu-item>
        <el-menu-item index="/admin/users">
          <el-icon><User /></el-icon>用户管理
        </el-menu-item>
        <el-menu-item index="/admin/products">
          <el-icon><Goods /></el-icon>商品审核
        </el-menu-item>
        <el-menu-item index="/admin/categories">
          <el-icon><List /></el-icon>分类管理
        </el-menu-item>
        <el-menu-item index="/admin/shops">
          <el-icon><Shop /></el-icon>店铺管理
        </el-menu-item>
        <el-menu-item index="/admin/orders">
          <el-icon><Document /></el-icon>订单总览
        </el-menu-item>
        <el-menu-item index="/admin/coupons">
          <el-icon><Tickets /></el-icon>优惠券管理
        </el-menu-item>
        <el-menu-item index="/admin/membership">
          <el-icon><Medal /></el-icon>会员管理
        </el-menu-item>
        <el-menu-item index="/admin/refunds">
          <el-icon><RefreshLeft /></el-icon>退款管理
        </el-menu-item>
        <el-menu-item index="/admin/logs">
          <el-icon><Memo /></el-icon>操作日志
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="admin-header">
        <span class="admin-heading">交易平台管理系统</span>
        <div class="admin-actions">
          <el-tooltip :content="isDark ? '切换到浅色模式' : '切换到深色模式'" placement="bottom">
            <el-button class="admin-theme-toggle" circle text aria-label="Toggle theme" @click="toggleTheme">
              <el-icon>
                <Sunny v-if="isDark" />
                <Moon v-else />
              </el-icon>
            </el-button>
          </el-tooltip>
          <span class="admin-username">{{ userStore.username }}</span>
          <el-button link type="danger" @click="handleLogout">退出登录</el-button>
        </div>
      </el-header>
      <el-main class="admin-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { DataAnalysis, User, Goods, List, Document, Shop, Tickets, Medal, RefreshLeft, Memo, Moon, Sunny } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'
import { useTheme } from '@/composables/useTheme'

const userStore = useUserStore()
const router = useRouter()
const { isDark, toggleTheme } = useTheme()

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.admin-layout {
  height: 100vh;
}

.admin-aside {
  background: var(--admin-sidebar-bg);
}

.admin-title {
  color: var(--admin-sidebar-active);
  font-size: 18px;
  font-weight: 700;
  padding: 20px 16px;
  border-bottom: 1px solid var(--admin-sidebar-border);
}

.admin-header {
  background: var(--surface-bg);
  border-bottom: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.admin-heading {
  font-size: 16px;
  color: var(--text-primary);
}

.admin-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.admin-username {
  color: var(--text-secondary);
}

.admin-theme-toggle {
  color: var(--text-secondary);
  font-size: 16px;
}

.admin-theme-toggle:hover,
.admin-theme-toggle:focus {
  color: var(--brand-primary);
  background: var(--surface-bg-soft);
}

.admin-main {
  background: var(--app-bg);
  padding: 24px;
}
</style>
