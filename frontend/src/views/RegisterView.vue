<template>
  <div class="auth-container">
    <el-card class="auth-card">
      <div class="brand-top">优选商城</div>
      <h2 class="auth-title">创建账号</h2>
      <el-form :model="form" :rules="rules" ref="formRef" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="2-50个字符" size="large" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" size="large" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="至少8位，含字母和数字" show-password size="large" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-radio-group v-model="form.role">
            <el-radio value="BUYER">买家</el-radio>
            <el-radio value="SELLER">卖家</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleRegister" style="width:100%" size="large">注册</el-button>
        </el-form-item>
      </el-form>
      <p class="auth-link">已有账号？<router-link to="/login">立即登录</router-link></p>
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
const form = reactive({ username: '', email: '', password: '', role: 'BUYER' })
const rules = {
  username: [{ required: true, min: 2, max: 50, message: '用户名2-50个字符', trigger: 'blur' }],
  email: [{ required: true, type: 'email', message: '请输入有效邮箱', trigger: 'blur' }],
  password: [{ required: true, pattern: /^(?=.*[A-Za-z])(?=.*\d).{8,}$/, message: '至少8位含字母和数字', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

async function handleRegister() {
  await formRef.value.validate()
  loading.value = true
  try {
    const res = await authApi.register(form)
    userStore.setUser(res.data)
    ElMessage.success('注册成功')
    router.push('/')
  } catch (err) {
    ElMessage.error(err?.message || '注册失败')
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
  padding: 40px 0;
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
