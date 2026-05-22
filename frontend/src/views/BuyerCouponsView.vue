<template>
  <div class="page-container coupon-page">
    <div class="page-title-row">
      <h2>我的优惠券</h2>
      <el-button :icon="Refresh" @click="loadAll">刷新</el-button>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="可领取" name="claimable">
        <el-empty v-if="!loadingClaimable && claimableCoupons.length === 0" description="暂无可领取优惠券" />
        <div v-else class="coupon-grid" v-loading="loadingClaimable">
          <el-card v-for="coupon in claimableCoupons" :key="coupon.id" class="coupon-card" shadow="hover">
            <div class="coupon-main">
              <div>
                <div class="coupon-name">{{ coupon.name }}</div>
                <div class="coupon-rule">满 ¥{{ money(coupon.thresholdAmount) }} 减 ¥{{ money(coupon.discountAmount) }}</div>
              </div>
              <el-tag :type="coupon.claimLimitReached ? 'info' : 'success'" size="small">
                {{ coupon.claimLimitReached ? '已达上限' : `剩余 ${coupon.remainingQuantity}` }}
              </el-tag>
            </div>
            <div class="coupon-tags">
              <el-tag size="small" :type="coupon.stackable ? 'success' : 'info'">
                {{ coupon.stackable ? '可叠加' : '不可叠加' }}
              </el-tag>
            </div>
            <div class="coupon-desc">{{ coupon.description || '平台通用满减券' }}</div>
            <div class="coupon-meta">{{ formatDate(coupon.validFrom) }} 至 {{ formatDate(coupon.validTo) }}</div>
            <div class="coupon-actions">
              <el-button
                type="primary"
                :disabled="coupon.claimLimitReached"
                :loading="claimingId === coupon.id"
                @click="claim(coupon)"
              >
                领取
              </el-button>
            </div>
          </el-card>
        </div>
      </el-tab-pane>

      <el-tab-pane label="我的券包" name="mine">
        <div class="coupon-filter">
          <el-radio-group v-model="mineStatus" @change="loadMine">
            <el-radio-button label="">全部</el-radio-button>
            <el-radio-button label="UNUSED">未使用</el-radio-button>
            <el-radio-button label="USED">已使用</el-radio-button>
            <el-radio-button label="EXPIRED">已过期</el-radio-button>
          </el-radio-group>
        </div>
        <el-empty v-if="!loadingMine && myCoupons.length === 0" description="暂无优惠券" />
        <div v-else class="coupon-grid" v-loading="loadingMine">
          <el-card v-for="item in myCoupons" :key="item.id" class="coupon-card" shadow="hover">
            <div class="coupon-main">
              <div>
                <div class="coupon-name">{{ item.couponName }}</div>
                <div class="coupon-rule">满 ¥{{ money(item.thresholdAmount) }} 减 ¥{{ money(item.discountAmount) }}</div>
              </div>
              <div class="coupon-status-tags">
                <el-tag :type="buyerCouponType(item.status)" size="small">{{ buyerCouponLabel(item.status) }}</el-tag>
                <el-tag :type="audienceType(item.audience)" size="small">{{ audienceLabel(item.audience) }}</el-tag>
              </div>
            </div>
            <div class="coupon-tags">
              <el-tag size="small" :type="item.stackable ? 'success' : 'info'">
                {{ item.stackable ? '可叠加' : '不可叠加' }}
              </el-tag>
            </div>
            <div class="coupon-desc">{{ item.description || '平台通用满减券' }}</div>
            <div class="coupon-meta">{{ formatDate(item.validFrom) }} 至 {{ formatDate(item.validTo) }}</div>
            <div v-if="item.usedOrderId" class="coupon-meta">使用订单：{{ item.usedOrderId }}</div>
          </el-card>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { couponApi } from '@/api/coupon'

const activeTab = ref('claimable')
const claimableCoupons = ref([])
const myCoupons = ref([])
const loadingClaimable = ref(false)
const loadingMine = ref(false)
const claimingId = ref(null)
const mineStatus = ref('')

function money(value) {
  return Number(value || 0).toFixed(2)
}

function formatDate(value) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-'
}

function buyerCouponLabel(status) {
  return { UNUSED: '未使用', USED: '已使用', EXPIRED: '已过期' }[status] || status
}

function buyerCouponType(status) {
  return { UNUSED: 'success', USED: 'info', EXPIRED: 'warning' }[status] || ''
}

function audienceLabel(audience) {
  return audience === 'MEMBER' ? '会员专属' : '普通'
}

function audienceType(audience) {
  return audience === 'MEMBER' ? 'primary' : 'info'
}

async function loadClaimable() {
  loadingClaimable.value = true
  try {
    const res = await couponApi.claimable()
    claimableCoupons.value = res.data || []
  } catch (err) {
    ElMessage.error(err?.message || '加载可领取优惠券失败')
  } finally {
    loadingClaimable.value = false
  }
}

async function loadMine() {
  loadingMine.value = true
  try {
    const res = await couponApi.mine({ status: mineStatus.value || undefined })
    myCoupons.value = res.data || []
  } catch (err) {
    ElMessage.error(err?.message || '加载我的优惠券失败')
  } finally {
    loadingMine.value = false
  }
}

async function claim(coupon) {
  claimingId.value = coupon.id
  try {
    await couponApi.claim(coupon.id)
    ElMessage.success('领取成功')
    await loadClaimable()
    await loadMine()
    activeTab.value = 'mine'
  } catch (err) {
    ElMessage.error(err?.message || '领取失败')
  } finally {
    claimingId.value = null
  }
}

async function loadAll() {
  await Promise.all([loadClaimable(), loadMine()])
}

onMounted(loadAll)
</script>

<style scoped>
.page-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}
.coupon-filter {
  margin-bottom: 16px;
}
.coupon-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
  min-height: 120px;
}
.coupon-card {
  border-radius: 8px;
}
.coupon-main {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}
.coupon-status-tags,
.coupon-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.coupon-tags {
  margin-top: 10px;
}
.coupon-name {
  font-weight: 700;
  color: var(--text-primary);
}
.coupon-rule {
  margin-top: 6px;
  color: #f56c6c;
  font-size: 18px;
  font-weight: 700;
}
.coupon-desc,
.coupon-meta {
  color: var(--text-secondary);
  font-size: 13px;
  margin-top: 10px;
}
.coupon-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}
</style>
