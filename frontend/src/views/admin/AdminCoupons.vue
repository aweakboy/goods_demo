<template>
  <div>
    <div class="page-title-row">
      <h2>优惠券管理</h2>
      <div class="toolbar">
        <el-select v-model="filterStatus" clearable placeholder="状态筛选" style="width:140px" @change="load">
          <el-option label="草稿" value="DRAFT" />
          <el-option label="启用" value="ACTIVE" />
          <el-option label="停用" value="INACTIVE" />
        </el-select>
        <el-button type="primary" @click="openCreate">新建优惠券</el-button>
      </div>
    </div>

    <el-table :data="coupons" style="width:100%" v-loading="loading">
      <el-table-column label="ID" prop="id" width="70" />
      <el-table-column label="名称" prop="name" min-width="160" />
      <el-table-column label="门槛" width="110">
        <template #default="{row}">¥{{ money(row.thresholdAmount) }}</template>
      </el-table-column>
      <el-table-column label="抵扣" width="110">
        <template #default="{row}">¥{{ money(row.discountAmount) }}</template>
      </el-table-column>
      <el-table-column label="领取" width="130">
        <template #default="{row}">{{ row.claimedQuantity || 0 }} / {{ row.totalQuantity }}</template>
      </el-table-column>
      <el-table-column label="每人上限" prop="perUserLimit" width="100" />
      <el-table-column label="受众" width="100">
        <template #default="{row}">
          <el-tag :type="row.audience === 'MEMBER' ? 'primary' : 'info'" size="small">
            {{ row.audience === 'MEMBER' ? '会员专属' : '普通' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="叠加" width="90">
        <template #default="{row}">
          <el-tag :type="row.stackable ? 'success' : 'info'" size="small">
            {{ row.stackable ? '允许' : '不允许' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="有效期" min-width="230">
        <template #default="{row}">
          {{ formatDate(row.validFrom) }} 至 {{ formatDate(row.validTo) }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{row}">
          <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="190" fixed="right">
        <template #default="{row}">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button
            v-if="row.status !== 'ACTIVE'"
            link
            type="success"
            @click="activate(row)"
          >
            启用
          </el-button>
          <el-button
            v-else
            link
            type="warning"
            @click="deactivate(row)"
          >
            停用
          </el-button>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑优惠券' : '新建优惠券'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="例如：满100减10" />
        </el-form-item>
        <el-form-item label="说明" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="活动说明" />
        </el-form-item>
        <el-form-item label="满减门槛" prop="thresholdAmount">
          <el-input-number v-model="form.thresholdAmount" :min="0" :precision="2" :step="10" style="width:180px" />
        </el-form-item>
        <el-form-item label="抵扣金额" prop="discountAmount">
          <el-input-number v-model="form.discountAmount" :min="0.01" :precision="2" :step="5" style="width:180px" />
        </el-form-item>
        <el-form-item label="发放总量" prop="totalQuantity">
          <el-input-number v-model="form.totalQuantity" :min="1" :step="10" style="width:180px" />
        </el-form-item>
        <el-form-item label="每人上限" prop="perUserLimit">
          <el-input-number v-model="form.perUserLimit" :min="1" :step="1" style="width:180px" />
        </el-form-item>
        <el-form-item label="有效期" required>
          <div class="date-row">
            <el-form-item prop="validFrom">
              <el-date-picker
                v-model="form.validFrom"
                type="datetime"
                value-format="YYYY-MM-DDTHH:mm:ss"
                placeholder="开始时间"
              />
            </el-form-item>
            <el-form-item prop="validTo">
              <el-date-picker
                v-model="form.validTo"
                type="datetime"
                value-format="YYYY-MM-DDTHH:mm:ss"
                placeholder="结束时间"
              />
            </el-form-item>
          </div>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" style="width:180px">
            <el-option label="草稿" value="DRAFT" />
            <el-option label="启用" value="ACTIVE" />
            <el-option label="停用" value="INACTIVE" />
          </el-select>
        </el-form-item>
        <el-form-item label="受众" prop="audience">
          <el-select v-model="form.audience" style="width:180px">
            <el-option label="普通优惠券" value="PUBLIC" />
            <el-option label="会员专属券" value="MEMBER" />
          </el-select>
        </el-form-item>
        <el-form-item label="允许叠加">
          <el-switch v-model="form.stackable" active-text="允许" inactive-text="不允许" />
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
import { couponApi } from '@/api/coupon'

const coupons = ref([])
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
  thresholdAmount: 100,
  discountAmount: 10,
  totalQuantity: 100,
  perUserLimit: 1,
  validFrom: '',
  validTo: '',
  status: 'ACTIVE',
  audience: 'PUBLIC',
  stackable: false
})

const rules = {
  name: [{ required: true, message: '请输入优惠券名称', trigger: 'blur' }],
  thresholdAmount: [{ required: true, message: '请输入满减门槛', trigger: 'change' }],
  discountAmount: [{ required: true, message: '请输入抵扣金额', trigger: 'change' }],
  totalQuantity: [{ required: true, message: '请输入发放总量', trigger: 'change' }],
  perUserLimit: [{ required: true, message: '请输入每人领取上限', trigger: 'change' }],
  validFrom: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  validTo: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
  audience: [{ required: true, message: '请选择受众', trigger: 'change' }]
}

function money(value) {
  return Number(value || 0).toFixed(2)
}

function formatDate(value) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-'
}

function statusLabel(status) {
  return { DRAFT: '草稿', ACTIVE: '启用', INACTIVE: '停用' }[status] || status
}

function statusType(status) {
  return { DRAFT: 'info', ACTIVE: 'success', INACTIVE: 'warning' }[status] || ''
}

function resetForm() {
  Object.assign(form, {
    name: '',
    description: '',
    thresholdAmount: 100,
    discountAmount: 10,
    totalQuantity: 100,
    perUserLimit: 1,
    validFrom: '',
    validTo: '',
    status: 'ACTIVE',
    audience: 'PUBLIC',
    stackable: false
  })
  formRef.value?.clearValidate()
}

async function load() {
  loading.value = true
  try {
    const res = await couponApi.adminList({
      status: filterStatus.value || undefined,
      page: currentPage.value - 1,
      size: pageSize
    })
    coupons.value = res.data.content || []
    total.value = res.data.totalElements || 0
  } catch (err) {
    ElMessage.error(err?.message || '加载优惠券失败')
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
    thresholdAmount: Number(row.thresholdAmount || 0),
    discountAmount: Number(row.discountAmount || 0),
    totalQuantity: row.totalQuantity,
    perUserLimit: row.perUserLimit,
    validFrom: row.validFrom?.slice(0, 19),
    validTo: row.validTo?.slice(0, 19),
    status: row.status,
    audience: row.audience || 'PUBLIC',
    stackable: Boolean(row.stackable)
  })
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

async function save() {
  await formRef.value.validate()
  if (form.discountAmount > form.thresholdAmount) {
    ElMessage.warning('抵扣金额不能大于满减门槛')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await couponApi.adminUpdate(editingId.value, { ...form })
    } else {
      await couponApi.adminCreate({ ...form })
    }
    dialogVisible.value = false
    ElMessage.success('优惠券已保存')
    await load()
  } catch (err) {
    ElMessage.error(err?.message || '保存优惠券失败')
  } finally {
    saving.value = false
  }
}

async function activate(row) {
  try {
    await couponApi.adminActivate(row.id)
    ElMessage.success('优惠券已启用')
    await load()
  } catch (err) {
    ElMessage.error(err?.message || '启用失败')
  }
}

async function deactivate(row) {
  try {
    await couponApi.adminDeactivate(row.id)
    ElMessage.success('优惠券已停用')
    await load()
  } catch (err) {
    ElMessage.error(err?.message || '停用失败')
  }
}

function onPageChange(page) {
  currentPage.value = page
  load()
}

onMounted(load)
</script>

<style scoped>
.page-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}
.toolbar,
.date-row {
  display: flex;
  gap: 12px;
  align-items: center;
}
.date-row {
  width: 100%;
}
</style>
