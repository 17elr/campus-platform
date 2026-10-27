<script setup>
import { computed } from "vue";
const props = defineProps({
  page: Number,
  size: Number,
  total: Number,
  busy: Boolean,
});
const emit = defineEmits(["update:page", "update:size"]);
const pages = computed(() => Math.max(1, Math.ceil(props.total / props.size)));
</script>
<template>
  <div class="list-pager">
    <button
      :disabled="busy || page <= 1"
      @click="emit('update:page', page - 1)"
    >
      上一页</button
    ><span>第 {{ page }} / {{ pages }} 页 · 共 {{ total }} 条</span
    ><button
      :disabled="busy || page >= pages"
      @click="emit('update:page', page + 1)"
    >
      下一页</button
    ><label
      >每页
      <select
        :value="size"
        :disabled="busy"
        @change="emit('update:size', Number($event.target.value))"
      >
        <option v-for="n in [10, 12, 20, 50]" :key="n" :value="n">
          {{ n }}
        </option>
      </select>
      条</label
    >
  </div>
</template>
<style scoped>
.list-pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  flex-wrap: wrap;
  margin: 24px 0;
}
.list-pager button,
.list-pager select {
  padding: 9px;
  border: 1px solid #d6e8e1;
  border-radius: 6px;
  background: #e4eef0;
  color: #24645f;
}
.list-pager button:disabled {
  opacity: 0.45;
}
</style>
