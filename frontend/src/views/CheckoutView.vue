<template>
  <div class="page-container">
    <h2>确认订单</h2>
    <el-card style="margin-bottom:20px">
      <h3>商品清单</h3>
      <el-table :data="cartStore.items" style="width:100%">
        <el-table-column label="商品" prop="product.name" min-width="200" />
        <el-table-column label="单价" width="120">
          <template #default="{row}">¥{{ row.product?.price?.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="数量" prop="quantity" width="80" />
        <el-table-column label="小计" width="120">
          <template #default="{row}">¥{{ (row.product?.price * row.quantity).toFixed(2) }}</template>
        </el-table-column>
      </el-table>
      <div style="text-align:right;margin-top:12px;font-size:16px">
        合计：<strong style="color:#f56c6c;font-size:20px">¥{{ cartStore.total.toFixed(2) }}</strong>
      </div>
    </el-card>

    <el-card style="margin-bottom:20px">
      <h3>优惠券</h3>
      <div class="coupon-select-row">
        <el-select
          v-model="selectedBuyerCouponId"
          clearable
          filterable
          placeholder="选择可用优惠券"
          @change="handleCouponChange"
        >
          <el-option
            v-for="coupon in usableCoupons"
            :key="coupon.id"
            :label="couponOptionLabel(coupon)"
            :value="coupon.id"
            :disabled="!couponEligible(coupon)"
          >
            <div class="coupon-option">
              <span>{{ couponOptionLabel(coupon) }}</span>
              <el-tag v-if="!couponEligible(coupon)" size="small" type="warning">未达门槛</el-tag>
            </div>
          </el-option>
        </el-select>
        <el-button @click="$router.push('/coupons')">去领券</el-button>
      </div>
      <div class="amount-preview">
        <span>商品金额：¥{{ amountText(originalAmount) }}</span>
        <span>优惠券：-¥{{ amountText(selectedDiscount) }}</span>
        <span v-if="membershipActive">会员优惠：-¥{{ amountText(membershipDiscount) }}</span>
        <strong>应付：¥{{ amountText(payableAmount) }}</strong>
      </div>
      <el-alert
        v-if="selectedCoupon && !selectedCouponEligible"
        type="warning"
        :closable="false"
        title="当前订单金额未达到该优惠券使用门槛"
        style="margin-top:10px"
      />
    </el-card>

    <el-card>
      <h3>收货信息</h3>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
        <el-form-item v-if="addresses.length > 0" label="常用地址">
          <div class="address-select-row">
            <el-select
              v-model="selectedAddressId"
              clearable
              filterable
              placeholder="选择常用地址"
              @change="handleAddressChange"
            >
              <el-option
                v-for="address in addresses"
                :key="address.id"
                :label="addressOptionLabel(address)"
                :value="address.id"
              >
                <div class="address-option">
                  <span>{{ addressOptionLabel(address) }}</span>
                  <el-tag v-if="address.defaultAddress" size="small" type="success">默认</el-tag>
                </div>
              </el-option>
            </el-select>
            <el-button @click="useManualAddress">手动填写</el-button>
          </div>
        </el-form-item>
        <el-form-item label="收货人" prop="receiverName">
          <el-input v-model="form.receiverName" placeholder="请输入收货人姓名" @input="clearSelectedAddress" />
        </el-form-item>
        <el-form-item label="手机号" prop="receiverPhone">
          <el-input v-model="form.receiverPhone" placeholder="请输入11位手机号" @input="clearSelectedAddress" />
        </el-form-item>
        <el-form-item label="省份" prop="receiverProvince">
          <el-input v-model="form.receiverProvince" placeholder="例如：浙江省" @input="clearSelectedAddress" />
        </el-form-item>
        <el-form-item label="城市" prop="receiverCity">
          <el-input v-model="form.receiverCity" placeholder="例如：杭州市" @input="clearSelectedAddress" />
        </el-form-item>
        <el-form-item label="区县" prop="receiverDistrict">
          <el-input v-model="form.receiverDistrict" placeholder="例如：西湖区" @input="clearSelectedAddress" />
        </el-form-item>
        <el-form-item label="详细地址" prop="receiverDetailAddress">
          <el-input
            v-model="form.receiverDetailAddress"
            type="textarea"
            :rows="3"
            placeholder="请输入街道、门牌号等详细地址"
            @input="clearSelectedAddress"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="submitOrder">提交订单</el-button>
          <el-button :loading="saveAddressLoading" @click="saveCurrentAddress">保存为常用地址</el-button>
          <el-button @click="$router.push('/cart')">返回购物车</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { orderApi } from '@/api/order'
import { addressApi } from '@/api/address'
import { couponApi } from '@/api/coupon'
import { membershipApi } from '@/api/membership'
import { useCartStore } from '@/stores/cart'

const router = useRouter()
const cartStore = useCartStore()
const formRef = ref()
const loading = ref(false)
const saveAddressLoading = ref(false)
const addresses = ref([])
const selectedAddressId = ref(null)
const usableCoupons = ref([])
const selectedBuyerCouponId = ref(null)
const membershipStatus = ref({})
const form = reactive({
  receiverName: '',
  receiverPhone: '',
  receiverProvince: '',
  receiverCity: '',
  receiverDistrict: '',
  receiverDetailAddress: ''
})
const rules = {
  receiverName: [{ required: true, message: '请输入收货人姓名', trigger: 'blur' }],
  receiverPhone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  receiverProvince: [{ required: true, message: '请输入省份', trigger: 'blur' }],
  receiverCity: [{ required: true, message: '请输入城市', trigger: 'blur' }],
  receiverDistrict: [{ required: true, message: '请输入区县', trigger: 'blur' }],
  receiverDetailAddress: [{ required: true, message: '请输入详细地址', trigger: 'blur' }]
}

const originalAmount = computed(() => Number(cartStore.total || 0))
const selectedCoupon = computed(() => usableCoupons.value.find(item => item.id === selectedBuyerCouponId.value))
const selectedCouponEligible = computed(() => !selectedCoupon.value || couponEligible(selectedCoupon.value))
const selectedDiscount = computed(() => {
  if (!selectedCoupon.value || !selectedCouponEligible.value) return 0
  return Math.min(Number(selectedCoupon.value.discountAmount || 0), originalAmount.value)
})
const membershipActive = computed(() => membershipStatus.value?.member && Number(membershipStatus.value.discountRate || 1) < 1)
const amountAfterCoupon = computed(() => Math.max(0, originalAmount.value - selectedDiscount.value))
const membershipDiscount = computed(() => {
  if (!membershipActive.value) return 0
  return amountAfterCoupon.value * (1 - Number(membershipStatus.value.discountRate || 1))
})
const payableAmount = computed(() => Math.max(0, amountAfterCoupon.value - membershipDiscount.value))

function amountText(value) {
  return Number(value || 0).toFixed(2)
}

function couponEligible(coupon) {
  return originalAmount.value >= Number(coupon.thresholdAmount || 0)
}

function couponOptionLabel(coupon) {
  return `${coupon.couponName} 满${amountText(coupon.thresholdAmount)}减${amountText(coupon.discountAmount)}`
}

function addressOptionLabel(address) {
  return `${address.receiverName} ${address.receiverPhone} ${formatAddress(address)}`
}

function formatAddress(address) {
  return address.fullAddress || `${address.province}${address.city}${address.district}${address.detailAddress}`
}

function fillForm(address) {
  Object.assign(form, {
    receiverName: address.receiverName,
    receiverPhone: address.receiverPhone,
    receiverProvince: address.province,
    receiverCity: address.city,
    receiverDistrict: address.district,
    receiverDetailAddress: address.detailAddress
  })
}

async function loadAddresses(preferredId = null) {
  try {
    const res = await addressApi.list()
    addresses.value = res.data || []
    const target = addresses.value.find(item => item.id === preferredId)
      || addresses.value.find(item => item.defaultAddress)
    if (target) {
      selectedAddressId.value = target.id
      fillForm(target)
    }
  } catch (err) {
    ElMessage.error(err?.message || '常用地址加载失败')
  }
}

async function loadCoupons() {
  try {
    const res = await couponApi.usable()
    usableCoupons.value = res.data || []
    if (selectedBuyerCouponId.value && !usableCoupons.value.some(item => item.id === selectedBuyerCouponId.value)) {
      selectedBuyerCouponId.value = null
    }
  } catch (err) {
    ElMessage.error(err?.message || '优惠券加载失败')
  }
}

async function loadMembershipStatus() {
  try {
    const res = await membershipApi.status()
    membershipStatus.value = res.data || {}
  } catch {
    membershipStatus.value = {}
  }
}

function handleCouponChange(id) {
  const coupon = usableCoupons.value.find(item => item.id === id)
  if (coupon && !couponEligible(coupon)) {
    ElMessage.warning('当前订单金额未达到该优惠券使用门槛')
  }
}

function handleAddressChange(id) {
  const address = addresses.value.find(item => item.id === id)
  if (address) {
    fillForm(address)
  }
}

function clearSelectedAddress() {
  selectedAddressId.value = null
}

function useManualAddress() {
  selectedAddressId.value = null
}

function toAddressRequest(defaultAddress = false) {
  return {
    receiverName: form.receiverName,
    receiverPhone: form.receiverPhone,
    province: form.receiverProvince,
    city: form.receiverCity,
    district: form.receiverDistrict,
    detailAddress: form.receiverDetailAddress,
    defaultAddress
  }
}

async function saveCurrentAddress() {
  await formRef.value.validate()
  saveAddressLoading.value = true
  try {
    const res = await addressApi.create(toAddressRequest(addresses.value.length === 0))
    ElMessage.success('地址已保存')
    await loadAddresses(res.data?.id)
  } catch (err) {
    ElMessage.error(err?.message || '地址保存失败')
  } finally {
    saveAddressLoading.value = false
  }
}

async function submitOrder() {
  await formRef.value.validate()
  if (selectedCoupon.value && !selectedCouponEligible.value) {
    ElMessage.warning('当前订单金额未达到优惠券使用门槛')
    return
  }
  loading.value = true
  try {
    const payload = { ...form }
    if (selectedAddressId.value) {
      payload.addressId = selectedAddressId.value
    }
    if (selectedBuyerCouponId.value) {
      payload.buyerCouponId = selectedBuyerCouponId.value
    }
    const res = await orderApi.create(payload)
    cartStore.clear()
    ElMessage.success('订单创建成功')
    router.push(`/orders/${res.data.id}`)
  } catch (err) {
    ElMessage.error(err?.message || '下单失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadAddresses()
  loadCoupons()
  loadMembershipStatus()
})
</script>

<style scoped>
.address-select-row {
  display: flex;
  gap: 10px;
  width: 100%;
}
.address-select-row .el-select {
  flex: 1;
}
.address-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  max-width: 100%;
}
.address-option span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.coupon-select-row {
  display: flex;
  gap: 10px;
  width: 100%;
}
.coupon-select-row .el-select {
  flex: 1;
}
.coupon-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  max-width: 100%;
}
.coupon-option span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.amount-preview {
  display: flex;
  justify-content: flex-end;
  gap: 20px;
  margin-top: 12px;
  font-size: 15px;
}
.amount-preview strong {
  color: #f56c6c;
  font-size: 18px;
}
@media (max-width: 640px) {
  .address-select-row,
  .coupon-select-row,
  .amount-preview {
    flex-direction: column;
  }
}
</style>
