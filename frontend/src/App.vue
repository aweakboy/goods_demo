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
body {
  margin: 0;
  font-family: 'Inter', 'Helvetica Neue', Arial, sans-serif;
  background: var(--app-bg);
  color: var(--text-primary);
}
#app {
  min-height: 100vh;
  background: var(--app-bg);
  color: var(--text-primary);
  transition: background-color 0.2s ease, color 0.2s ease;
}
.page-container { max-width: 1200px; margin: 0 auto; padding: 20px; }
</style>
