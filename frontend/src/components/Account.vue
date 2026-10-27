<script setup>
import { ref, computed, onMounted, watch } from "vue";
import Pager from "./Pager.vue";
import PromotionOrders from "./PromotionOrders.vue";
import PromotionPurchase from "./PromotionPurchase.vue";
import DeliveryOrders from "./DeliveryOrders.vue";
const props = defineProps({ user: Object, request: Function });
const emit = defineEmits(["changed"]);
const tab = ref("profile"),
  items = ref([]),
  orders = ref([]),
  loading = ref(false),
  saving = ref(false),
  busy = ref(false),
  error = ref("");
const profile = ref({ ...props.user });
const apiBase =
  import.meta.env.VITE_API_BASE_URL || "http://127.0.0.1:8088/api";
const rows = computed(() => orders.value);
const page = ref(1),
  size = ref(10),
  total = ref(0),
  promoting = ref(null);
const status = (s) =>
  ({
    APPROVED: "已公开",
    WITHDRAWN: "已下架",
    HIDDEN: "管理员下架",
    PENDING_PAYMENT: "待付款",
    PAID: "已付款",
  })[s] || s;
const imageSrc = (p) => (p ? new URL(p, apiBase).href : "");
let loadSequence = 0;
async function load() {
  const seq = ++loadSequence,
    selectedTab = tab.value;
  if (
    selectedTab === "profile" ||
    selectedTab === "promotions" ||
    selectedTab === "delivery"
  ) {
    loading.value = false;
    return;
  }
  loading.value = true;
  error.value = "";
  try {
    const endpoint =
      selectedTab === "items"
        ? "/items/mine"
        : `/trade/orders?role=${selectedTab}`;
    const data = await props.request(
      endpoint +
        (endpoint.includes("?") ? "&" : "?") +
        `page=${page.value}&size=${size.value}`,
      undefined,
      "get",
    );
    if (seq !== loadSequence) return;
    if (selectedTab === "items") items.value = data.records;
    else orders.value = data.records;
    total.value = data.total;
  } catch (e) {
    if (seq === loadSequence) error.value = e.message;
  } finally {
    if (seq === loadSequence) loading.value = false;
  }
}
watch(tab, () => {
  promoting.value = null;
  if (page.value === 1) load();
  else page.value = 1;
});
watch(page, load);
watch(size, () => {
  if (page.value === 1) load();
  else page.value = 1;
});
async function saveProfile() {
  saving.value = true;
  error.value = "";
  try {
    const u = await props.request(
      "/users/profile",
      {
        nickname: profile.value.nickname,
        bio: profile.value.bio || "",
        avatarUrl: profile.value.avatarUrl || null,
      },
      "put",
    );
    Object.assign(profile.value, u);
    Object.assign(props.user, u);
    emit("changed");
  } catch (e) {
    error.value = e.response?.data?.message || e.message;
  } finally {
    saving.value = false;
  }
}
async function uploadAvatar(e) {
  const f = e.target.files[0];
  e.target.value = "";
  if (!f) return;
  if (
    !["image/jpeg", "image/png"].includes(f.type) ||
    f.size > 5 * 1024 * 1024
  ) {
    error.value = "请选择5MB以内的 JPG/PNG 图片";
    return;
  }
  const fd = new FormData();
  fd.append("file", f);
  try {
    profile.value.avatarUrl = await props.request("/images", fd);
  } catch (e) {
    error.value = e.response?.data?.message || e.message;
  }
}
async function manageItem(item, remove) {
  if (busy.value || !confirm(`${remove ? "删除" : "下架"}“${item.title}”？`))
    return;
  busy.value = true;
  try {
    await props.request(
      `/items/${item.id}${remove ? "" : "/withdraw"}`,
      undefined,
      remove ? "delete" : "put",
    );
    await load();
  } catch (e) {
    error.value = e.response?.data?.message || e.message;
  } finally {
    busy.value = false;
  }
}
onMounted(load);
</script>
<template>
  <section class="account">
    <div class="profile-card">
      <div class="avatar-wrap">
        <img
          v-if="profile.avatarUrl"
          :src="imageSrc(profile.avatarUrl)"
          alt="头像"
        /><span v-else>{{
          (profile.nickname || profile.username || "访").slice(0, 1)
        }}</span
        ><label
          >更换头像<input
            type="file"
            accept="image/jpeg,image/png"
            @change="uploadAvatar"
        /></label>
      </div>
      <div class="profile-copy">
        <span class="eyebrow">PERSONAL SPACE / 个人空间</span>
        <h2>{{ profile.nickname || profile.username }}</h2>
        <p>
          @{{ profile.username }} ·
          {{ profile.role === "ADMIN" ? "管理员" : "普通用户" }}
        </p>
        <textarea
          v-model.trim="profile.bio"
          maxlength="500"
          placeholder="写一句简介，让别人更了解你…"
        ></textarea
        ><button class="save" :disabled="saving" @click="saveProfile">
          {{ saving ? "保存中…" : "保存个人资料" }}
        </button>
      </div>
    </div>
    <nav>
      <button
        v-for="(label, key) in {
          profile: '资料概览',
          items: '我的发布',
          buy: '我买到的',
          sell: '我卖出的',
          promotions: '推广订单',
          delivery: '配送订单',
        }"
        :key="key"
        :class="{ active: tab === key }"
        @click="tab = key"
      >
        {{ label }}</button
      ><button :disabled="loading || busy" @click="load">刷新</button>
    </nav>
    <p v-if="error" role="alert">{{ error }}</p>
    <p v-if="loading" role="status">正在加载…</p>
    <div v-else-if="tab === 'profile'" class="profile-summary">
      <h3>账号信息</h3>
      <p>用户名：{{ profile.username }}</p>
      <p>昵称：{{ profile.nickname }}</p>
      <p>身份：{{ profile.role === "ADMIN" ? "管理员" : "普通用户" }}</p>
      <p>简介：{{ profile.bio || "还没有填写简介" }}</p>
    </div>
    <DeliveryOrders
      v-if="tab === 'profile'"
      :request="request"
      :user-id="user.id"
    />
    <p v-if="tab === 'profile'" class="delivery-tip">
      也可以点击上方“配送订单”查看完整配送记录。
    </p>
    <DeliveryOrders
      v-else-if="tab === 'delivery'"
      :request="request"
      :user-id="user.id"
    /><PromotionOrders
      v-else-if="tab === 'promotions'"
      :request="request"
      @changed="emit('changed')"
    />
    <div v-else-if="tab === 'items'">
      <article v-for="item in items" :key="item.id">
        <h3>{{ item.title }}</h3>
        <p>{{ item.description }}</p>
        <p>
          {{ status(item.status) }} · {{ item.location }} · {{ item.createdAt }}
        </p>
        <button
          v-if="item.status === 'APPROVED' && item.type === 'SECOND_HAND'"
          @click="promoting = promoting === item.id ? null : item.id"
        >
          推广商品</button
        ><PromotionPurchase
          v-if="promoting === item.id"
          :key="item.id"
          :item="item"
          :request="request"
          @changed="emit('changed')"
        /><button
          v-if="item.status === 'APPROVED'"
          :disabled="busy"
          @click="manageItem(item, false)"
        >
          下架</button
        ><button
          class="danger"
          :disabled="busy"
          @click="manageItem(item, true)"
        >
          删除
        </button>
      </article>
      <p v-if="!items.length">还没有发布信息。</p>
    </div>
    <div v-else>
      <article v-for="order in rows" :key="order.id">
        <div class="order-head">
          <h3>订单 {{ order.orderNo }}</h3>
        </div>
        <p>
          商品编号：{{ order.productId }} · {{ status(order.status) }} · ¥{{
            order.amount
          }}
        </p>
        <p v-if="tab === 'sell'">
          平台佣金：¥{{ order.commission }} · 卖家收入：¥{{
            order.sellerAmount
          }}
        </p>
        <p>创建时间：{{ order.createdAt }}</p>
      </article>
      <p v-if="!rows.length">暂无订单。</p>
    </div>
    <Pager
      v-if="['items', 'buy', 'sell'].includes(tab)"
      v-model:page="page"
      v-model:size="size"
      :total="total"
      :busy="loading || busy"
    />
  </section>
</template>
<style scoped>
.account {
  width: 100%;
}
.profile-card {
  display: flex;
  gap: 28px;
  padding: 30px;
  background: linear-gradient(135deg, #e5f2ee, #fbfcfb);
  border: 1px solid #d6e8e1;
  border-radius: 18px;
  align-items: center;
}
.avatar-wrap {
  width: 120px;
  text-align: center;
}
.avatar-wrap img,
.avatar-wrap > span {
  display: grid;
  place-items: center;
  width: 96px;
  height: 96px;
  border-radius: 50%;
  object-fit: cover;
  background: #28766e;
  color: #fff;
  font-size: 34px;
  font-weight: 700;
}
.avatar-wrap label {
  display: block;
  margin-top: 10px;
  font-size: 12px;
  color: #28766e;
  cursor: pointer;
}
.avatar-wrap input {
  display: none;
}
.profile-copy {
  flex: 1;
}
.eyebrow {
  font-size: 10px;
  letter-spacing: 2px;
  color: #71968d;
}
.profile-copy h2 {
  margin: 8px 0;
}
.profile-copy textarea {
  display: block;
  width: min(680px, 100%);
  min-height: 70px;
  margin: 12px 0;
  padding: 12px;
  border: 1px solid #cfe0da;
  border-radius: 8px;
  resize: vertical;
}
.save,
article button {
  border: 0;
  border-radius: 6px;
  background: #28766e;
  color: #fff;
  padding: 9px 14px;
}
.danger {
  background: #a45f58 !important;
}
.account nav {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin: 24px 0;
}
.account nav button {
  border: 0;
  border-radius: 6px;
  background: #e4eef0;
  color: #24546a;
  padding: 10px 16px;
}
.account nav button.active {
  background: #2876a8;
  color: #fff;
}
.profile-summary,
article {
  padding: 22px;
  background: #fff;
  border: 1px solid #dce8e5;
  border-radius: 10px;
  margin-bottom: 14px;
}
.profile-summary p,
article p {
  color: #627780;
}
.order-head {
  display: flex;
  justify-content: space-between;
  gap: 15px;
}
button:disabled {
  opacity: 0.5;
}
[role="alert"] {
  color: #b34332;
}
@media (max-width: 600px) {
  .profile-card {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
