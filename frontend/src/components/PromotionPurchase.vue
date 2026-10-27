<script setup>
import { ref, onMounted } from "vue";
const props = defineProps({ item: Object, request: Function });
const emit = defineEmits(["changed"]);
const plans = ref([]),
  chosen = ref("TOP_24H"),
  order = ref(null),
  busy = ref(false),
  message = ref("");
onMounted(async () => {
  try {
    plans.value = await props.request("/promotions/plans", undefined, "get");
  } catch (e) {
    message.value = e.message;
  }
});
async function create() {
  busy.value = true;
  message.value = "";
  try {
    order.value = await props.request("/promotions/orders", {
      itemId: props.item.id,
      promotionType: chosen.value,
    });
    message.value = "订单已创建，可模拟支付或在个人中心继续付款。";
  } catch (e) {
    message.value = e.message;
  } finally {
    busy.value = false;
  }
}
async function pay() {
  busy.value = true;
  try {
    await props.request(`/promotions/orders/${order.value.id}/pay`);
    order.value = null;
    message.value = "模拟支付成功，推广已生效。";
    emit("changed");
  } catch (e) {
    message.value = e.message;
  } finally {
    busy.value = false;
  }
}
</script>
<template>
  <section class="promotion-purchase">
    <h3>推广我的商品</h3>
    <p>
      24
      小时置顶在大厅优先展示；首页推荐优先进入首页；分类置顶在对应物品类别中优先展示。
    </p>
    <p>当前为模拟支付，不会产生真实扣款。同类型推广到期后可再次购买。</p>
    <label
      >推广套餐
      <select v-model="chosen" :disabled="busy || !!order">
        <option v-for="p in plans" :key="p.type" :value="p.type">
          {{ p.label }} · ¥{{ Number(p.amount).toFixed(2) }}
        </option>
      </select></label
    ><button
      v-if="!order"
      class="primary"
      :disabled="busy || !plans.length"
      @click="create"
    >
      创建推广订单</button
    ><template v-else
      ><p>订单 {{ order.orderNo }} · ¥{{ Number(order.amount).toFixed(2) }}</p>
      <button class="primary" :disabled="busy" @click="pay">
        确认模拟支付
      </button></template
    >
    <p v-if="message" role="status">{{ message }}</p>
  </section>
</template>
<style scoped>
.promotion-purchase {
  border: 1px solid #d6e8e1;
  background: #f2f8f5;
  padding: 18px;
  border-radius: 10px;
  margin: 18px 0;
}
.promotion-purchase p {
  font-size: 13px;
  line-height: 1.6;
}
.promotion-purchase select {
  padding: 9px;
  max-width: 100%;
  margin: 8px;
}
.promotion-purchase button {
  padding: 10px;
}
</style>
