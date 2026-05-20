<template>
  <div>
    <div style="display:flex;gap:12px;margin-bottom:16px;align-items:center">
      <h2 style="margin:0">用户管理</h2>
      <el-select v-model="filterRole" placeholder="角色筛选" clearable style="width:120px" @change="load">
        <el-option label="买家" value="BUYER" />
        <el-option label="卖家" value="SELLER" />
      </el-select>
      <el-select v-model="filterStatus" placeholder="状态筛选" clearable style="width:120px" @change="load">
        <el-option label="正常" value="ACTIVE" />
        <el-option label="已禁用" value="DISABLED" />
      </el-select>
    </div>
    <el-table :data="users" style="width:100%">
      <el-table-column label="ID" prop="id" width="70" />
      <el-table-column label="用户名" prop="username" min-width="120" />
      <el-table-column label="邮箱" prop="email" min-width="200" />
      <el-table-column label="角色" width="90">
        <template #default="{row}">
          <el-tag :type="{ SELLER: 'warning', ADMIN: 'danger', BUYER: 'primary' }[row.role]" size="small">
            {{ { SELLER: '卖家', ADMIN: '管理员', BUYER: '买家' }[row.role] || row.role }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{row}">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" size="small">
            {{ row.status === 'ACTIVE' ? '正常' : '已禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="注册时间" min-width="160">
        <template #default="{row}">{{ formatDate(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{row}">
          <el-button
            link
            :type="row.status === 'ACTIVE' ? 'danger' : 'success'"
            @click="toggleStatus(row)"
          >
            {{ row.status === 'ACTIVE' ? '禁用' : '启用' }}
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api/admin'

const users = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = 20
const filterRole = ref(null)
const filterStatus = ref(null)

function formatDate(dt) {
  return dt ? dt.replace('T', ' ').slice(0, 16) : ''
}

async function load() {
  try {
    const res = await adminApi.getUsers({
      role: filterRole.value || undefined,
      status: filterStatus.value || undefined,
      page: currentPage.value - 1,
      size: pageSize
    })
    users.value = res.data.content
    total.value = res.data.totalElements
  } catch {
    ElMessage.error('加载用户列表失败')
  }
}

async function toggleStatus(row) {
  const newStatus = row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  const action = newStatus === 'DISABLED' ? '禁用' : '启用'
  await ElMessageBox.confirm(`确认${action}用户「${row.username}」？`, '提示', { type: 'warning' })
  try {
    const res = await adminApi.updateUserStatus(row.id, newStatus)
    row.status = res.data.status
    ElMessage.success(`已${action}`)
  } catch (err) {
    ElMessage.error(err?.message || `${action}失败`)
  }
}

function onPageChange(page) {
  currentPage.value = page
  load()
}

onMounted(load)
</script>
