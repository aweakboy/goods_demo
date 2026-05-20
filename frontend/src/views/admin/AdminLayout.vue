<template>
  <el-container style="height: 100vh">
    <el-aside width="200px" style="background:#001529">
      <div style="color:#fff;font-size:18px;font-weight:bold;padding:20px 16px;border-bottom:1px solid #1d2b3a">
        管理后台
      </div>
      <el-menu
        :default-active="$route.path"
        router
        background-color="#001529"
        text-color="#b0b8c1"
        active-text-color="#fff"
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
      </el-menu>
    </el-aside>
    <el-container>
      <el-header style="background:#fff;border-bottom:1px solid #e8e8e8;display:flex;align-items:center;justify-content:space-between">
        <span style="font-size:16px;color:#333">交易平台管理系统</span>
        <div style="display:flex;align-items:center;gap:12px">
          <span style="color:#666">{{ userStore.username }}</span>
          <el-button link type="danger" @click="handleLogout">退出登录</el-button>
        </div>
      </el-header>
      <el-main style="background:#f0f2f5;padding:24px">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { DataAnalysis, User, Goods, List, Document, Shop } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'

const userStore = useUserStore()
const router = useRouter()

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>
