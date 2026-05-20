<template>
  <el-config-provider>
    <AppHeader />
    <router-view />
  </el-config-provider>
</template>

<script setup>
import AppHeader from '@/components/AppHeader.vue'
</script>

<style>
body { margin: 0; font-family: 'Inter', 'Helvetica Neue', Arial, sans-serif; }
#app { min-height: 100vh; background: #F8F7F5; }
.page-container { max-width: 1200px; margin: 0 auto; padding: 20px; }
</style>
