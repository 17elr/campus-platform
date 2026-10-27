<script setup>
defineProps({ item: Object, busy: Boolean, acting: Number });
defineEmits(["inspect", "moderate"]);
</script>

<template>
  <div class="moderation-actions">
    <button class="inspect-action" @click="$emit('inspect', item)">
      查看详情
    </button>
    <button
      v-if="['PENDING', 'APPROVED', 'HIDDEN'].includes(item.status)"
      :class="[
        'moderate-action',
        { 'hide-action': item.status === 'APPROVED' },
      ]"
      :disabled="busy || acting != null"
      @click="$emit('moderate', item)"
    >
      {{
        acting === item.id
          ? "处理中…"
          : item.status === "APPROVED"
            ? "下架"
            : item.status === "PENDING"
              ? "审核通过"
              : "恢复公开"
      }}
    </button>
    <small v-else>保留历史记录</small>
  </div>
</template>

<style scoped>
.moderation-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.moderation-actions button {
  padding: 8px 11px;
  font: inherit;
  font-size: 12px;
  border: 1px solid #bbced2;
  border-radius: 7px;
  background: #fff;
  color: #315969;
  white-space: nowrap;
}
.moderation-actions .moderate-action {
  background: #204e65;
  color: #fff;
  border-color: #204e65;
}
.moderation-actions .hide-action {
  color: #914833;
  background: #fff3eb;
  border-color: #e7c4b1;
}
.moderation-actions button:hover {
  filter: brightness(0.95);
}
.moderation-actions button:focus-visible {
  outline: 2px solid #315969;
  outline-offset: 3px;
}
.moderation-actions button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.moderation-actions small {
  font-size: 11px;
  color: #637477;
}
</style>
