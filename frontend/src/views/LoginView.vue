<template>
  <div class="auth-container">
    <el-card class="auth-card">
      <div class="brand-top">优选商城</div>
      <h2 class="auth-title">欢迎回来</h2>
      <el-form :model="form" :rules="rules" ref="formRef" label-position="top">
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" size="large" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password size="large" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleLogin" style="width:100%" size="large">登录</el-button>
        </el-form-item>
      </el-form>
      <p class="auth-link">没有账号？<router-link to="/register">立即注册</router-link></p>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)
const form = reactive({ email: '', password: '' })
const rules = {
  email: [{ required: true, type: 'email', message: '请输入有效邮箱', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  await formRef.value.validate()
  loading.value = true
  try {
    const res = await authApi.login(form)
    userStore.setUser(res.data)
    ElMessage.success('登录成功')
    const role = res.data.role
    router.push(role === 'ADMIN' ? '/admin/overview' : role === 'SELLER' ? '/seller/products' : '/')
  } catch (err) {
    ElMessage.error(err?.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 60px);
  background: var(--brand-gradient);
}
.auth-card {
  width: 420px;
  border-radius: 12px !important;
  box-shadow: 0 20px 60px rgba(0,0,0,0.25) !important;
  overflow: hidden;
}
.brand-top {
  text-align: center;
  font-size: 22px;
  font-weight: 700;
  color: var(--brand-primary);
  margin-bottom: 4px;
  letter-spacing: 1px;
}
.auth-title {
  text-align: center;
  margin: 0 0 24px;
  font-size: 18px;
  color: #333;
  font-weight: 500;
}
.auth-link { text-align: center; margin: 0; color: #666; font-size: 14px; }
</style>
