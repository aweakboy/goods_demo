import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useCartStore = defineStore('cart', () => {
  const items = ref([])

  const total = computed(() =>
    items.value.reduce((sum, item) => sum + item.product.price * item.quantity, 0)
  )

  const count = computed(() => items.value.reduce((n, i) => n + i.quantity, 0))

  function setItems(data) {
    items.value = data
  }

  function clear() {
    items.value = []
  }

  return { items, total, count, setItems, clear }
})
