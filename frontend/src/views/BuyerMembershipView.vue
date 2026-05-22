<template>
  <div class="page-container">
    <h2>会员中心</h2>

    <el-card style="margin-bottom:20px">
      <div class="membership-status">
        <div>
          <h3>{{ statusTitle }}</h3>
          <p v-if="status.member" class="muted">
            {{ status.planName }} · {{ discountText(status.discountRate) }} · 到期时间 {{ formatDate(status.expiresAt) }}
          </p>
          <p v-else-if="status.status === 'EXPIRED'" class="muted">
            会员已过期，可重新购买套餐恢复权益。
          </p>
          <p v-else class="muted">购买会员后可享受会员折扣和每月专属优惠券。</p>
        </div>
        <el-button
          v-if="status.member"
          type="primary"
          :disabled="status.currentMonthBenefitClaimed || !status.monthlyCouponId"
          :loading="claiming"
          @click="claimBenefit"
        >
          {{ benefitButtonText }}
        </el-button>
      </div>
    </el-card>

    <el-card style="margin-bottom:20px">
      <h3>可购买套餐</h3>
      <el-table :data="plans" v-loading="loadingPlans" style="width:100%">
        <el-table-column label="套餐" prop="name" min-width="150" />
        <el-table-column label="价格" width="110">
          <template #default="{row}">¥{{ money(row.price) }}</template>
        </el-table-column>
        <el-table-column label="有效期" width="100">
          <template #default="{row}">{{ row.durationMonths }} 个月</template>
        </el-table-column>
        <el-table-column label="会员折扣" width="110">
          <template #default="{row}">{{ discountText(row.discountRate) }}</template>
        </el-table-column>
        <el-table-column label="每月专属券" min-width="160">
          <template #default="{row}">{{ row.monthlyCouponName || '暂无' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{row}">
            <el-button type="primary" link :loading="buyingId === row.id" @click="buy(row)">购买</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card>
      <h3>购买记录</h3>
      <el-table :data="purchases" v-loading="loadingPurchases" style="width:100%">
        <el-table-column label="记录号" prop="id" width="90" />
        <el-table-column label="套餐" prop="planName" min-width="150" />
        <el-table-column label="金额" width="110">
          <template #default="{row}">¥{{ money(row.amount) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{row}">
            <el-tag :type="purchaseStatusType(row.status)" size="small">{{ purchaseStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{row}">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { membershipApi } from '@/api/membership'

const status = ref({})
const plans = ref([])
const purchases = ref([])
const loadingPlans = ref(false)
const loadingPurchases = ref(false)
const claiming = ref(false)
const buyingId = ref(null)

const statusTitle = computed(() => {
  if (status.value.member) return '会员有效'
  if (status.value.status === 'EXPIRED') return '会员已过期'
  return '暂未开通会员'
})

const benefitButtonText = computed(() => {
  if (!status.value.monthlyCouponId) return '暂无月度权益'
  return status.value.currentMonthBenefitClaimed ? '本月已领取' : '领取本月专属券'
})

function money(value) {
  return Number(value || 0).toFixed(2)
}

function discountText(value) {
  return `${(Number(value || 1) * 10).toFixed(1)}折`
}

function formatDate(value) {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}

function purchaseStatusLabel(status) {
  return {
    PENDING_PAYMENT: '待支付',
    PAID: '已支付',
    CANCELLED: '已取消',
    EXPIRED: '已过期'
  }[status] || status
}

function purchaseStatusType(status) {
  return {
    PENDING_PAYMENT: 'warning',
    PAID: 'success',
    CANCELLED: 'info',
    EXPIRED: 'info'
  }[status] || ''
}

async function loadStatus() {
  try {
    const res = await membershipApi.status()
    status.value = res.data || {}
  } catch (err) {
    ElMessage.error(err?.message || '加载会员状态失败')
  }
}

async function loadPlans() {
  loadingPlans.value = true
  try {
    const res = await membershipApi.plans()
    plans.value = res.data || []
  } catch (err) {
    ElMessage.error(err?.message || '加载会员套餐失败')
  } finally {
    loadingPlans.value = false
  }
}

async function loadPurchases() {
  loadingPurchases.value = true
  try {
    const res = await membershipApi.purchases()
    purchases.value = res.data || []
  } catch (err) {
    ElMessage.error(err?.message || '加载购买记录失败')
  } finally {
    loadingPurchases.value = false
  }
}

async function claimBenefit() {
  claiming.value = true
  try {
    await membershipApi.claimMonthlyBenefit()
    ElMessage.success('会员专属券已领取')
    await loadStatus()
  } catch (err) {
    ElMessage.error(err?.message || '领取会员权益失败')
  } finally {
    claiming.value = false
  }
}

async function buy(row) {
  buyingId.value = row.id
  try {
    const htmlForm = await membershipApi.purchase(row.id)
    const div = document.createElement('div')
    div.innerHTML = htmlForm
    document.body.appendChild(div)
    const form = div.querySelector('form')
    form.target = '_blank'
    form.submit()
    document.body.removeChild(div)
    ElMessage.info('支付页面已打开，支付成功后刷新会员中心查看状态')
    await loadPurchases()
  } catch (err) {
    ElMessage.error(err?.message || '发起会员购买失败')
  } finally {
    buyingId.value = null
  }
}

onMounted(() => {
  loadStatus()
  loadPlans()
  loadPurchases()
})
</script>

<style scoped>
.membership-status {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}
.membership-status h3 {
  margin: 0 0 8px;
}
.muted {
  color: var(--text-secondary);
  margin: 0;
}
@media (max-width: 640px) {
  .membership-status {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
