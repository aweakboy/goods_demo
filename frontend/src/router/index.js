import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  { path: '/', redirect: '/products' },
  { path: '/products', component: () => import('@/views/ProductListView.vue') },
  { path: '/products/:id', component: () => import('@/views/ProductDetailView.vue') },
  { path: '/cart', component: () => import('@/views/CartView.vue'), meta: { requiresAuth: true, role: 'BUYER' } },
  { path: '/checkout', component: () => import('@/views/CheckoutView.vue'), meta: { requiresAuth: true, role: 'BUYER' } },
  { path: '/orders', component: () => import('@/views/OrderListView.vue'), meta: { requiresAuth: true } },
  { path: '/orders/:id', component: () => import('@/views/OrderDetailView.vue'), meta: { requiresAuth: true } },
  { path: '/login', component: () => import('@/views/LoginView.vue') },
  { path: '/register', component: () => import('@/views/RegisterView.vue') },
  { path: '/seller/shop', component: () => import('@/views/seller/SellerShopView.vue'), meta: { requiresAuth: true, role: 'SELLER' } },
  { path: '/seller/products', component: () => import('@/views/seller/SellerProductsView.vue'), meta: { requiresAuth: true, role: 'SELLER' } },
  { path: '/seller/products/new', component: () => import('@/views/seller/ProductFormView.vue'), meta: { requiresAuth: true, role: 'SELLER' } },
  { path: '/seller/products/:id/edit', component: () => import('@/views/seller/ProductFormView.vue'), meta: { requiresAuth: true, role: 'SELLER' } },
  { path: '/seller/orders', component: () => import('@/views/seller/SellerOrdersView.vue'), meta: { requiresAuth: true, role: 'SELLER' } },
  { path: '/shops/:id', component: () => import('@/views/ShopStorefrontView.vue') },
  {
    path: '/admin',
    component: () => import('@/views/admin/AdminLayout.vue'),
    meta: { requiresAuth: true, role: 'ADMIN' },
    children: [
      { path: '', redirect: '/admin/overview' },
      { path: 'overview', component: () => import('@/views/admin/AdminOverview.vue') },
      { path: 'users', component: () => import('@/views/admin/AdminUsers.vue') },
      { path: 'products', component: () => import('@/views/admin/AdminProducts.vue') },
      { path: 'categories', component: () => import('@/views/admin/AdminCategories.vue') },
      { path: 'shops', component: () => import('@/views/admin/AdminShops.vue') },
      { path: 'orders', component: () => import('@/views/admin/AdminOrders.vue') },
    ]
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const userStore = useUserStore()
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    return '/login'
  }
  if (to.meta.role && userStore.role !== to.meta.role) {
    return '/'
  }
})

export default router
