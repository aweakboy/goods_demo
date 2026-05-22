<template>
  <div>
    <h2 style="margin-bottom:20px">退款管理</h2>
    <el-table :data="list" v-loading="loading" border>
      <el-table-column label="订单号" prop="id" width="80" />
      <el-table-column label="金额" width="110">
        <template #default="{row}">¥{{ row.totalAmount?.toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="退款原因" prop="refundReason" min-width="180" show-overflow-tooltip />
      <el-table-column label="申请时间" width="160">
        <template #default="{row}">{{ row.createdAt?.slice(0,19).replace('T',' ') }}</template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{row}">
          <el-button type="success" size="small" @click="approve(row)">批准退款</el-button>
          <el-button type="danger" size="small" plain @click="openReject(row)">拒绝</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="rejectDialog" title="拒绝退款" width="360px">
      <el-input v-model="rejectReason" type="textarea" :rows="3" placeholder="请填写拒绝原因..." />
      <template #footer>
        <el-button @click="rejectDialog=false">取消</el-button>
        <el-button type="danger" :loading="actionLoading" @click="submitReject">确认拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api/admin'

const list = ref([])
const loading = ref(false)
const rejectDialog = ref(false)
const rejectReason = ref('')
const actionLoading = ref(false)
const currentOrder = ref(null)

async function load() {
  loading.value = true
  try {
    const res = await adminApi.refundRequests()
    list.value = res.data
  } finally {
    loading.value = false
  }
}

async function approve(row) {
  await ElMessageBox.confirm(`确认批准订单 #${row.id} 的退款申请？`, '批准退款', { type: 'warning' })
  actionLoading.value = true
  try {
    await adminApi.approveRefund(row.id)
    ElMessage.success('退款已处理')
    load()
  } catch (err) {
    ElMessage.error(err?.message || '退款处理失败')
  } finally {
    actionLoading.value = false
  }
}

function openReject(row) {
  currentOrder.value = row
  rejectReason.value = ''
  rejectDialog.value = true
}

async function submitReject() {
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请填写拒绝原因')
    return
  }
  actionLoading.value = true
  try {
    await adminApi.rejectRefund(currentOrder.value.id, rejectReason.value)
    ElMessage.success('已拒绝退款申请')
    rejectDialog.value = false
    load()
  } catch (err) {
    ElMessage.error(err?.message || '操作失败')
  } finally {
    actionLoading.value = false
  }
}

onMounted(load)
</script>
