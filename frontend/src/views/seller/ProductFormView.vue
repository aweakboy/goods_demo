<template>
  <div class="page-container">
    <h2>{{ isEdit ? '编辑商品' : '上架新商品' }}</h2>
    <el-card style="max-width:600px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="商品描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="价格" prop="price">
          <el-input-number v-model="form.price" :min="0" :precision="2" :step="0.01" />
        </el-form-item>
        <el-form-item label="库存" prop="stock">
          <el-input-number v-model="form.stock" :min="0" />
        </el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择分类" clearable>
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="商品图片">
          <el-upload
            :file-list="imageFileList"
            list-type="picture-card"
            :limit="1"
            accept="image/jpeg,image/png,image/gif,image/webp"
            :http-request="handleUpload"
            :on-exceed="() => ElMessage.warning('只能上传一张图片')"
            :on-remove="handleRemove"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>
        <el-form-item v-if="isEdit" label="状态">
          <el-select v-model="form.status">
            <el-option label="上架中" value="ACTIVE" />
            <el-option label="已下架" value="INACTIVE" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="submit">{{ isEdit ? '保存' : '上架' }}</el-button>
          <el-button @click="$router.push('/seller/products')">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { productApi } from '@/api/product'

const route = useRoute()
const router = useRouter()
const formRef = ref()
const loading = ref(false)
const categories = ref([])
const isEdit = computed(() => !!route.params.id)

const form = reactive({ name: '', description: '', price: 0, stock: 0, categoryId: null, imageUrl: '', status: 'ACTIVE' })
const rules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }],
  stock: [{ required: true, message: '请输入库存', trigger: 'blur' }]
}

const imageFileList = ref([])

async function handleUpload({ file }) {
  try {
    const url = await productApi.uploadImage(file)
    form.imageUrl = url
    imageFileList.value = [{ name: file.name, url: `http://localhost:8080${url}` }]
  } catch (err) {
    ElMessage.error(err?.message || '图片上传失败')
  }
}

function handleRemove() {
  form.imageUrl = ''
  imageFileList.value = []
}

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    if (isEdit.value) {
      await productApi.update(route.params.id, form)
    } else {
      await productApi.create(form)
    }
    ElMessage.success(isEdit.value ? '保存成功' : '上架成功')
    await router.push('/seller/products')
  } catch (err) {
    ElMessage.error(err?.message || '操作失败')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  const catRes = await productApi.categories()
  categories.value = catRes.data
  if (isEdit.value) {
    const res = await productApi.detail(route.params.id)
    Object.assign(form, res.data)
    if (res.data.imageUrl) {
      const src = res.data.imageUrl.startsWith('http')
        ? res.data.imageUrl
        : `http://localhost:8080${res.data.imageUrl}`
      imageFileList.value = [{ name: '当前图片', url: src }]
    }
  }
})
</script>
