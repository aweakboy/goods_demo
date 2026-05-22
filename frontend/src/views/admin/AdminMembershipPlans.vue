<template>
  <div>
    <div class="page-title-row">
      <h2>会员管理</h2>
      <div class="toolbar">
        <el-select v-model="filterStatus" clearable placeholder="状态筛选" style="width:140px" @change="load">
          <el-option label="启用" value="ACTIVE" />
          <el-option label="停用" value="INACTIVE" />
        </el-select>
        <el-button type="primary" @click="openCreate">新建套餐</el-button>
      </div>
    </div>

    <el-table :data="plans" style="width:100%" v-loading="loading">
      <el-table-column label="ID" prop="id" width="70" />
      <el-table-column label="名称" prop="name" min-width="160" />
      <el-table-column label="价格" width="110">
        <template #default="{row}">¥{{ money(row.price) }}</template>
      </el-table-column>
      <el-table-column label="有效期" width="100">
        <template #default="{row}">{{ row.durationMonths }} 个月</template>
      </el-table-column>
      <el-table-column label="折扣" width="100">
        <template #default="{row}">{{ discountText(row.discountRate) }}</template>
      </el-table-column>
      <el-table-column label="每月专属券" min-width="160">
        <template #default="{row}">
          {{ row.monthlyCouponName || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{row}">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'warning'" size="small">
            {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="190" fixed="right">
        <template #default="{row}">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="row.status !== 'ACTIVE'" link type="success" @click="activate(row)">启用</el-button>
          <el-button v-else link type="warning" @click="deactivate(row)">停用</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      style="margin-top:16px;justify-content:flex-end"
      layout="total, prev, pager, next"
      :total="total"
      :page-size="pageSize"
      :current-page="currentPage"
      @current-change="onPageChange"
    />

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑会员套餐' : '新建会员套餐'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="例如：月度会员" />
        </el-form-item>
        <el-form-item label="说明" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="套餐说明" />
        </el-form-item>
        <el-form-item label="价格" prop="price">
          <el-input-number v-model="form.price" :min="0.01" :precision="2" :step="10" style="width:180px" />
        </el-form-item>
        <el-form-item label="有效期(月)" prop="durationMonths">
          <el-input-number v-model="form.durationMonths" :min="1" :step="1" style="width:180px" />
        </el-form-item>
        <el-form-item label="折扣率" prop="discountRate">
          <el-input-number v-model="form.discountRate" :min="0.01" :max="1" :precision="4" :step="0.01" style="width:180px" />
        </el-form-item>
        <el-form-item label="每月专属券">
          <el-select v-model="form.monthlyCouponId" clearable filterable placeholder="选择会员专属券" style="width:100%">
            <el-option
              v-for="coupon in memberCoupons"
              :key="coupon.id"
              :label="couponLabel(coupon)"
              :value="coupon.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" style="width:180px">
            <el-option label="启用" value="ACTIVE" />
            <el-option label="停用" value="INACTIVE" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { membershipApi } from '@/api/membership'
import { couponApi } from '@/api/coupon'

const plans = ref([])
const memberCoupons = ref([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingId = ref(null)
const formRef = ref()
const total = ref(0)
const currentPage = ref(1)
const pageSize = 20
const filterStatus = ref(null)

const form = reactive({
  name: '',
  description: '',
  price: 30,
  durationMonths: 1,
  discountRate: 0.95,
  monthlyCouponId: null,
  status: 'ACTIVE'
})

const rules = {
  name: [{ required: true, message: '请输入套餐名称', trigger: 'blur' }],
  price: [{ required: true, message: '请输入套餐价格', trigger: 'change' }],
  durationMonths: [{ required: true, message: '请输入有效期', trigger: 'change' }],
  discountRate: [{ required: true, message: '请输入折扣率', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

function money(value) {
  return Number(value || 0).toFixed(2)
}

function discountText(value) {
  return `${(Number(value || 1) * 10).toFixed(1)}折`
}

function couponLabel(coupon) {
  return `${coupon.name} 满${money(coupon.thresholdAmount)}减${money(coupon.discountAmount)}`
}

function resetForm() {
  Object.assign(form, {
    name: '',
    description: '',
    price: 30,
    durationMonths: 1,
    discountRate: 0.95,
    monthlyCouponId: null,
    status: 'ACTIVE'
  })
  formRef.value?.clearValidate()
}

async function loadCoupons() {
  try {
    const res = await couponApi.adminList({ page: 0, size: 200 })
    memberCoupons.value = (res.data.content || []).filter(item => item.audience === 'MEMBER')
  } catch (err) {
    ElMessage.error(err?.message || '加载会员专属券失败')
  }
}

async function load() {
  loading.value = true
  try {
    const res = await membershipApi.adminList({
      status: filterStatus.value || undefined,
      page: currentPage.value - 1,
      size: pageSize
    })
    plans.value = res.data.content || []
    total.value = res.data.totalElements || 0
  } catch (err) {
    ElMessage.error(err?.message || '加载会员套餐失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  Object.assign(form, {
    name: row.name,
    description: row.description || '',
    price: Number(row.price || 0),
    durationMonths: row.durationMonths,
    discountRate: Number(row.discountRate || 1),
    monthlyCouponId: row.monthlyCouponId || null,
    status: row.status
  })
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (editingId.value) {
      await membershipApi.adminUpdate(editingId.value, { ...form })
    } else {
      await membershipApi.adminCreate({ ...form })
    }
    dialogVisible.value = false
    ElMessage.success('会员套餐已保存')
    await load()
  } catch (err) {
    ElMessage.error(err?.message || '保存会员套餐失败')
  } finally {
    saving.value = false
  }
}

async function activate(row) {
  try {
    await membershipApi.adminActivate(row.id)
    ElMessage.success('会员套餐已启用')
    await load()
  } catch (err) {
    ElMessage.error(err?.message || '启用失败')
  }
}

async function deactivate(row) {
  try {
    await membershipApi.adminDeactivate(row.id)
    ElMessage.success('会员套餐已停用')
    await load()
  } catch (err) {
    ElMessage.error(err?.message || '停用失败')
  }
}

function onPageChange(page) {
  currentPage.value = page
  load()
}

onMounted(() => {
  loadCoupons()
  load()
})
</script>

<style scoped>
.page-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}
.toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
}
</style>
