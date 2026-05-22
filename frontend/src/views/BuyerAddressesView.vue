<template>
  <div class="page-container address-page">
    <div class="page-title-row">
      <h2>我的地址</h2>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增地址</el-button>
    </div>

    <el-empty v-if="!loading && addresses.length === 0" description="暂无常用地址">
      <el-button type="primary" :icon="Plus" @click="openCreate">新增地址</el-button>
    </el-empty>

    <div v-else class="address-grid" v-loading="loading">
      <el-card
        v-for="address in addresses"
        :key="address.id"
        class="address-card"
        :class="{ 'is-default': address.defaultAddress }"
        shadow="hover"
      >
        <div class="address-card-header">
          <div>
            <strong>{{ address.receiverName }}</strong>
            <span>{{ address.receiverPhone }}</span>
          </div>
          <el-tag v-if="address.defaultAddress" type="success" size="small">默认</el-tag>
        </div>
        <div class="address-line">
          <el-icon><Location /></el-icon>
          <span>{{ formatAddress(address) }}</span>
        </div>
        <div class="address-meta">
          <el-tag :type="address.validationStatus === 'VALID' ? 'success' : 'warning'" size="small">
            {{ address.validationStatus || 'UNVERIFIED' }}
          </el-tag>
          <span v-if="address.formattedAddress">{{ address.formattedAddress }}</span>
        </div>
        <div class="address-actions">
          <el-tooltip content="设为默认" placement="top">
            <el-button
              :icon="StarFilled"
              circle
              :disabled="address.defaultAddress"
              @click="setDefault(address)"
            />
          </el-tooltip>
          <el-tooltip content="编辑" placement="top">
            <el-button :icon="Edit" circle @click="openEdit(address)" />
          </el-tooltip>
          <el-tooltip content="删除" placement="top">
            <el-button :icon="Delete" circle type="danger" plain @click="removeAddress(address)" />
          </el-tooltip>
        </div>
      </el-card>
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑地址' : '新增地址'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="收货人" prop="receiverName">
          <el-input v-model="form.receiverName" placeholder="请输入收货人姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="receiverPhone">
          <el-input v-model="form.receiverPhone" placeholder="请输入11位手机号" />
        </el-form-item>
        <el-form-item label="省份" prop="province">
          <el-input v-model="form.province" placeholder="例如：浙江省" />
        </el-form-item>
        <el-form-item label="城市" prop="city">
          <el-input v-model="form.city" placeholder="例如：杭州市" />
        </el-form-item>
        <el-form-item label="区县" prop="district">
          <el-input v-model="form.district" placeholder="例如：西湖区" />
        </el-form-item>
        <el-form-item label="详细地址" prop="detailAddress">
          <el-input v-model="form.detailAddress" type="textarea" :rows="3" placeholder="请输入街道、门牌号等详细地址" />
        </el-form-item>
        <el-form-item label="默认地址">
          <el-switch v-model="form.defaultAddress" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitAddress">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Location, Plus, StarFilled } from '@element-plus/icons-vue'
import { addressApi } from '@/api/address'

const addresses = ref([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingId = ref(null)
const formRef = ref()

const form = reactive({
  receiverName: '',
  receiverPhone: '',
  province: '',
  city: '',
  district: '',
  detailAddress: '',
  defaultAddress: false
})

const rules = {
  receiverName: [{ required: true, message: '请输入收货人姓名', trigger: 'blur' }],
  receiverPhone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  province: [{ required: true, message: '请输入省份', trigger: 'blur' }],
  city: [{ required: true, message: '请输入城市', trigger: 'blur' }],
  district: [{ required: true, message: '请输入区县', trigger: 'blur' }],
  detailAddress: [{ required: true, message: '请输入详细地址', trigger: 'blur' }]
}

function resetForm(defaultAddress = false) {
  Object.assign(form, {
    receiverName: '',
    receiverPhone: '',
    province: '',
    city: '',
    district: '',
    detailAddress: '',
    defaultAddress
  })
  formRef.value?.clearValidate()
}

function formatAddress(address) {
  return address.fullAddress || `${address.province}${address.city}${address.district}${address.detailAddress}`
}

async function loadAddresses() {
  loading.value = true
  try {
    const res = await addressApi.list()
    addresses.value = res.data || []
  } catch (err) {
    ElMessage.error(err?.message || '地址加载失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  resetForm(addresses.value.length === 0)
  dialogVisible.value = true
}

function openEdit(address) {
  editingId.value = address.id
  Object.assign(form, {
    receiverName: address.receiverName,
    receiverPhone: address.receiverPhone,
    province: address.province,
    city: address.city,
    district: address.district,
    detailAddress: address.detailAddress,
    defaultAddress: Boolean(address.defaultAddress)
  })
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

async function submitAddress() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (editingId.value) {
      await addressApi.update(editingId.value, { ...form })
    } else {
      await addressApi.create({ ...form })
    }
    dialogVisible.value = false
    ElMessage.success('地址已保存')
    await loadAddresses()
  } catch (err) {
    ElMessage.error(err?.message || '地址保存失败')
  } finally {
    saving.value = false
  }
}

async function setDefault(address) {
  try {
    await addressApi.setDefault(address.id)
    ElMessage.success('默认地址已更新')
    await loadAddresses()
  } catch (err) {
    ElMessage.error(err?.message || '默认地址设置失败')
  }
}

async function removeAddress(address) {
  await ElMessageBox.confirm('确认删除该地址？历史订单地址不会受影响。', '删除地址', { type: 'warning' })
  try {
    await addressApi.remove(address.id)
    ElMessage.success('地址已删除')
    await loadAddresses()
  } catch (err) {
    ElMessage.error(err?.message || '地址删除失败')
  }
}

onMounted(loadAddresses)
</script>

<style scoped>
.page-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}
.address-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
  min-height: 120px;
}
.address-card {
  border-radius: 8px;
}
.address-card.is-default {
  border-color: var(--el-color-primary);
}
.address-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}
.address-card-header div {
  display: flex;
  align-items: center;
  gap: 10px;
}
.address-line {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  color: var(--text-primary);
  line-height: 1.5;
  min-height: 48px;
}
.address-line .el-icon {
  margin-top: 3px;
  color: var(--el-color-primary);
}
.address-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 26px;
  color: var(--text-secondary);
  font-size: 13px;
  margin-top: 10px;
}
.address-meta span:last-child {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.address-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
}
</style>
