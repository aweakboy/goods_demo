<template>
  <div class="page-container" style="max-width:600px;margin:40px auto">
    <h2>{{ shop ? '我的店铺' : '注册店铺' }}</h2>
    <el-card v-if="shop && !editing">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="店铺名称">{{ shop.name }}</el-descriptions-item>
        <el-descriptions-item label="店铺简介">{{ shop.description || '暂无' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="shop.status === 'ACTIVE' ? 'success' : 'danger'">
            {{ shop.status === 'ACTIVE' ? '营业中' : '已关闭' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ shop.createdAt?.slice(0,10) }}</el-descriptions-item>
      </el-descriptions>
      <el-button style="margin-top:16px" type="primary" @click="startEdit">编辑店铺信息</el-button>
    </el-card>

    <el-form v-else :model="form" :rules="rules" ref="formRef" label-width="90px">
      <el-form-item label="店铺名称" prop="name">
        <el-input v-model="form.name" placeholder="请输入店铺名称" />
      </el-form-item>
      <el-form-item label="店铺简介" prop="description">
        <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入店铺简介（可选）" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="submit">{{ shop ? '保存修改' : '立即注册' }}</el-button>
        <el-button v-if="shop" @click="editing = false">取消</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { shopApi } from '@/api/shop'

const shop = ref(null)
const editing = ref(false)
const loading = ref(false)
const formRef = ref()
const form = ref({ name: '', description: '' })
const rules = {
  name: [{ required: true, message: '请输入店铺名称', trigger: 'blur' }]
}

function startEdit() {
  form.value = { name: shop.value.name, description: shop.value.description || '' }
  editing.value = true
}

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    const res = shop.value
      ? await shopApi.updateShop(form.value)
      : await shopApi.registerShop(form.value)
    shop.value = res.data
    editing.value = false
    ElMessage.success(shop.value ? '店铺信息已更新' : '店铺注册成功')
  } catch (err) {
    ElMessage.error(err?.message || '操作失败')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  try {
    const res = await shopApi.getMyShop()
    shop.value = res.data
  } catch {
    // no shop yet
  }
})
</script>
