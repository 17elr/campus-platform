<script setup>
import { ref, onMounted, watch } from "vue";
import Pager from "./Pager.vue";
const props = defineProps({ request: Function, admin: Boolean });
const emit = defineEmits(["changed"]);
const records = ref([]),
  total = ref(0),
  page = ref(1),
  size = ref(10),
  stats = ref(null),
  busy = ref(false),
  error = ref("");
const labels = {
  TOP_24H: "24 小时置顶",
  FEATURED_3D: "首页推荐 3 天",
  CATEGORY_7D: "分类置顶 7 天",
  PENDING_PAYMENT: "待付款",
  ACTIVE: "推广中",
  EXPIRED: "已到期",
  CANCELLED: "已取消",
};
async function load() {
  busy.value = true;
  error.value = "";
  try {
    const base = props.admin ? "/admin/promotions" : "/promotions";
    const data = await props.request(
      `${base}/orders?page=${page.value}&size=${size.value}`,
      undefined,
      "get",
    );
    records.value = data.records;
    total.value = data.total;
    if (props.admin)
      stats.value = await props.request(base + "/stats", undefined, "get");
  } catch (e) {
    error.value = e.message;
  } finally {
    busy.value = false;
  }
}
async function act(o, action) {
  if (busy.value) return;
  busy.value = true;
  error.value = "";
  try {
    await props.request(`/promotions/orders/${o.id}/${action}`);
    await load();
    emit("changed");
  } catch (e) {
    error.value = e.message;
  } finally {
    busy.value = false;
  }
}
watch(page, load);
watch(size, () => {
  if (page.value === 1) load();
  else page.value = 1;
});
onMounted(load);
</script>
<template>
  <section class="promotion-orders">
    <h3>{{ admin ? "商品推广收入与订单" : "我的推广订单" }}</h3>
    <p>
      模拟支付记录，不代表真实到账。商品下架后停止展示；推广期限按支付时起算。
    </p>
    <div v-if="stats" class="promotion-stats">
      <span
        >推广总收入 <b>¥{{ Number(stats.revenue).toFixed(2) }}</b></span
      ><span
        >今日 <b>¥{{ Number(stats.todayRevenue).toFixed(2) }}</b></span
      ><span
        >本月 <b>¥{{ Number(stats.monthRevenue).toFixed(2) }}</b></span
      ><span
        >已付款 {{ stats.paidOrders }} 单 · 未到期
        {{ stats.activeOrders }} 单</span
      >
    </div>
    <button :disabled="busy" @click="load">刷新推广订单</button>
    <p v-if="error" role="alert">{{ error }}</p>
    <p v-if="busy">正在处理…</p>
    <article v-for="o in records" :key="o.id">
      <h4>{{ o.itemTitle }} · {{ labels[o.promotionType] }}</h4>
      <p>
        {{ o.orderNo }} · ¥{{ Number(o.amount).toFixed(2) }} ·
        {{ labels[o.status] }}
      </p>
      <p v-if="o.startAt">{{ o.startAt }} 至 {{ o.endAt }}</p>
      <p v-if="admin">用户 #{{ o.userId }} · 商品 #{{ o.itemId }}</p>
      <template v-if="!admin && o.status === 'PENDING_PAYMENT'"
        ><button :disabled="busy" @click="act(o, 'pay')">模拟支付</button>
        <button :disabled="busy" @click="act(o, 'cancel')">
          取消订单
        </button></template
      >
    </article>
    <p v-if="!busy && !records.length">暂无推广订单。</p>
    <Pager
      v-model:page="page"
      v-model:size="size"
      :total="total"
      :busy="busy"
    />
  </section>
</template>
<style scoped>
.promotion-orders {
  padding: 22px;
  background: white;
  border: 1px solid #d6e8e1;
  border-radius: 12px;
  margin: 20px 0;
}
.promotion-orders article {
  padding: 14px 0;
  border-bottom: 1px solid #e1ebe7;
  overflow-wrap: anywhere;
}
.promotion-orders p {
  color: #627780;
  font-size: 13px;
}
.promotion-orders button {
  padding: 9px 14px;
  background: #e4eef0;
  color: #24645f;
  border-radius: 6px;
}
.promotion-stats {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
  margin: 20px 0;
}
.promotion-stats b {
  display: block;
  font-size: 24px;
  color: #24645f;
}
</style>
