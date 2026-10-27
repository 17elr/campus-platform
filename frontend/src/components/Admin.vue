<script setup>
import { ref, computed, onMounted, watch, nextTick } from "vue";
import Pager from "./Pager.vue";
import PromotionOrders from "./PromotionOrders.vue";
import DeliveryOrders from "./DeliveryOrders.vue";
import CourierReview from "./CourierReview.vue";
import AdManagement from "./AdManagement.vue";
import OperationLogs from "./OperationLogs.vue";
import CategoryRecords from "./CategoryRecords.vue";
import ModerationActions from "./ModerationActions.vue";
const props = defineProps({
  request: Function,
  overview: Boolean,
  itemType: { type: String, default: "" },
  categoryMode: Boolean,
});
const emit = defineEmits(["changed"]);
const tab = ref("items"),
  query = ref(""),
  stats = ref({}),
  trade = ref({}),
  tradeOrders = ref([]),
  users = ref([]),
  items = ref([]),
  loading = ref(false),
  error = ref(""),
  acting = ref(null);
const categoryName = computed(
  () =>
    ({ LOST: "寻物启事", FOUND: "失物招领", SECOND_HAND: "二手闲置" })[
      props.itemType
    ] || "全部信息",
);
const currentWorkspace = computed(() =>
  props.categoryMode
    ? {
        title:
          {
            LOST: "寻物线索核查",
            FOUND: "招领物品核验",
            SECOND_HAND: "二手商品审核",
          }[props.itemType] || "全部信息管理",
        description:
          {
            LOST: "从左侧选择一条线索，在右侧核对遗失地点、物品特征与联系方式。",
            FOUND: "按物品逐件核验图片、拾获地点与发布信息，确认后公开展示。",
            SECOND_HAND:
              "集中检查商品资料、卖家与展示状态，处理不完整或违规的闲置信息。",
          }[props.itemType] ||
          "按分类、状态与关键词查找校园信息，审核后公开或下架。",
      }
    : props.overview
      ? {
          title: "运营数据总览",
          description: "从内容、用户与交易数据，了解校园平台的运行情况。",
        }
      : {
          items: {
            title: "内容审核工作台",
            description: "核对信息内容与发布状态，管理校园内公开展示的信息。",
          },
          users: {
            title: "用户与账号管理",
            description: "查看校园成员，管理账号状态与访问权限。",
          },
          logs: {
            title: "平台操作记录",
            description: "按时间和事件查找配送、审核与异常处理记录。",
          },
          ads: {
            title: "广告投放工作台",
            description: "管理校园广告素材、展示位置与投放状态。",
          },
          promotions: {
            title: "商品推广管理",
            description: "核对推广套餐、订单记录与推广有效期。",
          },
          delivery: {
            title: "配送履约工作台",
            description: "跟进校园配送状态，处理异常与订单履约。",
          },
          couriers: {
            title: "配送员资格审核",
            description: "核验申请人的校园身份，管理配送接单资格。",
          },
          settings: {
            title: "平台费用与规则",
            description: "维护新订单使用的配送费用与商品推广价格。",
          },
        }[tab.value],
);
const settings = ref({}),
  settingSaving = ref(false),
  dateFrom = ref(""),
  dateTo = ref("");
const userPage = ref(1),
  itemPage = ref(1),
  orderPage = ref(1),
  pageSize = ref(10),
  userTotal = ref(0),
  itemTotal = ref(0),
  orderTotal = ref(0);
const itemCounts = ref({}),
  statusFilter = ref(""),
  locationFilter = ref(""),
  appliedFilters = ref({ keyword: "", status: "", location: "" }),
  resultMessage = ref("");
const workflowKind = computed(() => (props.categoryMode ? props.itemType : ""));
const filterCopy = computed(
  () =>
    ({
      LOST: {
        keyword: "物品特征",
        placeholder: "搜索失物名称、颜色或特征",
        location: "遗失地点",
      },
      FOUND: {
        keyword: "招领物品",
        placeholder: "搜索拾获物品或物品类别",
        location: "拾获地点",
      },
      SECOND_HAND: {
        keyword: "商品检索",
        placeholder: "搜索商品名称、说明或类别",
        location: "交接地点",
      },
    })[workflowKind.value] || {
      keyword: "关键词",
      placeholder: "搜索标题、描述或物品类别",
      location: "所在地点",
    },
);
function quickStatus(value) {
  statusFilter.value = value;
  applyItemFilters();
}
const inspecting = ref(null),
  itemDialog = ref(null);
async function inspectItem(item) {
  inspecting.value = item;
  await nextTick();
  itemDialog.value.showModal();
}
function applyItemFilters() {
  appliedFilters.value = {
    keyword: query.value.trim(),
    status: statusFilter.value,
    location: locationFilter.value.trim(),
  };
  if (itemPage.value === 1) load();
  else itemPage.value = 1;
}
function resetItemFilters() {
  query.value = "";
  statusFilter.value = "";
  locationFilter.value = "";
  applyItemFilters();
}
const itemImage = (path) =>
  new URL(
    path,
    import.meta.env.VITE_API_BASE_URL || "http://127.0.0.1:8088/api",
  ).href;
const trendMax = computed(() =>
  Math.max(1, ...(trade.value.trend || []).map((x) => Number(x.gross) || 0)),
);
const chartY = (d) => 190 - (Number(d.gross || 0) / trendMax.value) * 160;
const chartPoints = computed(() =>
  (trade.value.trend || [])
    .map((d, i) => i * 100 + 25 + "," + chartY(d))
    .join(" "),
);
const visible = computed(() =>
  tab.value === "users"
    ? users.value.filter((x) =>
        [x.id, x.username, x.nickname, x.title, x.location]
          .join(" ")
          .includes(query.value.trim()),
      )
    : items.value,
);
const status = (s) =>
  ({
    APPROVED: "已公开",
    PENDING: "待审核",
    HIDDEN: "管理下架",
    WITHDRAWN: "用户撤回",
    DELETED: "用户已删除",
  })[s] || s;
let loadSequence = 0;
async function load() {
  const seq = ++loadSequence;
  loading.value = true;
  error.value = "";
  try {
    const itemParams = new URLSearchParams({
      page: itemPage.value,
      size: pageSize.value,
      type: props.itemType,
      ...appliedFilters.value,
    });
    if (props.categoryMode) {
      const data = await props.request(
        "/admin/items?" + itemParams,
        undefined,
        "get",
      );
      if (seq !== loadSequence) return;
      items.value = data.records;
      itemTotal.value = data.total;
      itemCounts.value = data.counts || {};
      return;
    }
    const data = await Promise.all(
      [
        "/stats",
        "/users?page=" + userPage.value + "&size=" + pageSize.value,
        "/items?" + itemParams,
        "/trade/stats?" +
          (dateFrom.value ? "from=" + dateFrom.value + "&" : "") +
          (dateTo.value ? "to=" + dateTo.value : ""),
        "/trade/orders?page=" + orderPage.value + "&size=" + pageSize.value,
        "/settings",
      ].map((p) => props.request("/admin" + p, undefined, "get")),
    );
    if (seq !== loadSequence) return;
    userTotal.value = data[1].total;
    itemTotal.value = data[2].total;
    itemCounts.value = data[2].counts || {};
    orderTotal.value = data[4].total;
    settings.value = data[5] || {};
    [stats.value, users.value, items.value, trade.value, tradeOrders.value] = [
      data[0],
      data[1]?.records || data[1] || [],
      data[2]?.records || data[2] || [],
      data[3],
      data[4]?.records || data[4] || [],
    ];
  } catch (e) {
    if (seq === loadSequence)
      error.value = e.response?.data?.message || e.message;
  } finally {
    if (seq === loadSequence) loading.value = false;
  }
}
function setRange(days) {
  const end = new Date();
  const start = new Date();
  start.setDate(end.getDate() - days + 1);
  dateTo.value = end.toISOString().slice(0, 10);
  dateFrom.value = start.toISOString().slice(0, 10);
  load();
}
function clearRange() {
  dateFrom.value = "";
  dateTo.value = "";
  load();
}
async function saveSettings() {
  settingSaving.value = true;
  try {
    settings.value = await props.request(
      "/admin/settings",
      settings.value,
      "put",
    );
    error.value = "平台配置已保存";
  } catch (e) {
    error.value = e.message;
  } finally {
    settingSaving.value = false;
  }
}
async function setStatus(item) {
  if (acting.value !== null) return;
  const next = item.status === "APPROVED" ? "HIDDEN" : "APPROVED";
  if (
    !window.confirm(`${next === "HIDDEN" ? "下架" : "公开"}“${item.title}”？`)
  )
    return;
  acting.value = item.id;
  error.value = "";
  resultMessage.value = "";
  try {
    await props.request(
      `/admin/items/${item.id}/status`,
      { status: next },
      "put",
    );
    await load();
    resultMessage.value =
      next === "HIDDEN"
        ? "信息已下架，用户端不再公开展示。"
        : "信息已公开，可在用户端查看。";
    emit("changed");
  } catch (e) {
    error.value = e.response?.data?.message || e.message;
  } finally {
    acting.value = null;
  }
}
async function manageUser(user, remove = false) {
  if (acting.value !== null) return;
  const next = user.status === 1 ? 0 : 1;
  const message = remove
    ? `删除用户“${user.username}”？账号将永久停用，关联记录保留。`
    : `${next === 0 ? "禁用" : "恢复"}用户“${user.username}”？`;
  if (!window.confirm(message)) return;
  acting.value = user.id;
  error.value = "";
  try {
    await props.request(
      `/admin/users/${user.id}${remove ? "" : "/status"}`,
      remove ? undefined : { status: next },
      remove ? "delete" : "put",
    );
    await load();
  } catch (e) {
    error.value = e.response?.data?.message || e.message;
  } finally {
    acting.value = null;
  }
}
watch([userPage, itemPage, orderPage], load);
watch(pageSize, () => {
  const alreadyFirst =
    userPage.value === 1 && itemPage.value === 1 && orderPage.value === 1;
  userPage.value = 1;
  itemPage.value = 1;
  orderPage.value = 1;
  if (alreadyFirst) load();
});
onMounted(load);
</script>
<template>
  <section
    class="admin-view"
    :class="{ overview: overview, 'category-management': categoryMode }"
    :data-workflow="workflowKind || 'ALL'"
  >
    <header class="admin-head">
      <div>
        <span class="admin-kicker">{{
          categoryMode
            ? "CONTENT MODERATION / " + (itemType || "ALL")
            : overview
              ? "PLATFORM OVERVIEW"
              : "OPERATIONS WORKSPACE"
        }}</span>
        <h2>{{ currentWorkspace.title }}</h2>
        <p>{{ currentWorkspace.description }}</p>
      </div>
      <div class="admin-head-actions">
        <span class="admin-sync"
          ><i></i>
          {{
            loading
              ? "正在加载数据"
              : error
                ? "请查看操作提示"
                : "数据来源：平台记录"
          }}</span
        ><button
          class="admin-refresh"
          :disabled="loading || acting !== null"
          @click="load"
        >
          刷新数据
        </button>
      </div>
    </header>
    <div v-if="overview" class="date-filter">
      <div class="filter-title">
        <span>运营视图</span><strong>选择统计时间范围</strong>
      </div>
      <div class="filter-presets">
        <button @click="setRange(1)">本日</button
        ><button @click="setRange(1)">近 24 小时</button
        ><button @click="setRange(7)">近 7 天</button
        ><button @click="setRange(30)">近 30 天</button
        ><button @click="setRange(90)">本季度</button>
      </div>
      <label>开始日期<input v-model="dateFrom" type="date" /></label
      ><label>结束日期<input v-model="dateTo" type="date" /></label
      ><button class="active" @click="load">查询</button
      ><button @click="clearRange">清除</button
      ><span v-if="dateFrom || dateTo"
        >当前：{{ dateFrom || "不限" }} 至 {{ dateTo || "不限" }}</span
      >
    </div>
    <div v-if="workflowKind === 'LOST'" class="lost-summary">
      <div class="lost-summary-title">
        <svg
          viewBox="0 0 32 32"
          fill="none"
          stroke="currentColor"
          stroke-width="1.5"
          aria-hidden="true"
        >
          <circle cx="13" cy="13" r="8" />
          <path d="m19 19 8 8M9 13h8m-4-4v8" />
        </svg>
        <div>
          <span>寻物线索池</span
          ><strong data-summary-total>{{ itemCounts.total ?? "—" }}</strong>
        </div>
      </div>
      <button
        v-for="(label, state) in {
          PENDING: '待核查',
          APPROVED: '已公开线索',
          HIDDEN: '已下架',
        }"
        :key="state"
        :disabled="loading || acting !== null"
        :aria-pressed="appliedFilters.status === state"
        @click="quickStatus(appliedFilters.status === state ? '' : state)"
      >
        <b :data-summary-state="state">{{ itemCounts[state] ?? "—" }}</b
        ><span>{{ label }}</span
        ><i aria-hidden="true">↗</i>
      </button>
    </div>
    <div v-else-if="workflowKind === 'FOUND'" class="found-intake">
      <div class="found-intake-title">
        <svg
          viewBox="0 0 40 40"
          fill="none"
          stroke="currentColor"
          stroke-width="1.5"
          aria-hidden="true"
        >
          <path d="m20 5 13 6v10c0 6-13 14-13 14S7 27 7 21V11l13-6Z" />
          <path d="m14 20 4 4 9-10" />
        </svg>
        <div>
          <span>招领物品台账</span
          ><strong
            ><b data-summary-total>{{ itemCounts.total ?? "—" }}</b>
            件记录</strong
          >
        </div>
      </div>
      <ol>
        <li><b>01</b>核对物品特征</li>
        <li><b>02</b>确认拾获地点</li>
        <li><b>03</b>检查联系信息</li>
      </ol>
      <button
        :disabled="loading || acting !== null"
        :aria-pressed="appliedFilters.status === 'PENDING'"
        @click="
          quickStatus(appliedFilters.status === 'PENDING' ? '' : 'PENDING')
        "
      >
        <span>待审核</span
        ><strong data-summary-state="PENDING">{{
          itemCounts.PENDING ?? "—"
        }}</strong
        ><span aria-hidden="true">→</span>
      </button>
    </div>
    <div v-else-if="workflowKind === 'SECOND_HAND'" class="market-stock">
      <div>
        <span>商品目录</span
        ><strong data-summary-total>{{ itemCounts.total ?? "—" }}</strong
        ><small>条闲置信息</small>
      </div>
      <nav aria-label="商品展示状态">
        <button
          v-for="(label, state) in {
            '': '全部商品',
            APPROVED: '公开在架',
            PENDING: '待审核',
            HIDDEN: '管理下架',
          }"
          :key="state"
          :disabled="loading || acting !== null"
          :aria-pressed="appliedFilters.status === state"
          @click="quickStatus(state)"
        >
          {{ label
          }}<b :data-summary-state="state || 'total'">{{
            itemCounts[state || "total"] ?? "—"
          }}</b>
        </button>
      </nav>
    </div>
    <div v-else-if="categoryMode" class="content-kpis">
      <div
        v-for="(label, key) in {
          total: '本分类全部',
          PENDING: '待审核',
          APPROVED: '已公开',
          HIDDEN: '管理下架',
        }"
        :key="key"
      >
        <span>{{ label }}</span
        ><strong>{{ itemCounts[key] ?? "—" }}</strong
        ><small>{{
          key === "total" ? "包含撤回与删除记录" : "本分类信息"
        }}</small>
      </div>
    </div>
    <div v-else class="admin-stats">
      <div
        v-for="(label, key) in {
          users: '注册用户',
          items: '全部信息',
          approved: '公开信息',
          pending: '待审核',
        }"
        :key="key"
      >
        <span>{{ label }}</span
        ><strong>{{ stats[key] ?? "—" }}</strong
        ><small>{{
          {
            users: "平台累计注册",
            items: "包含全部发布状态",
            approved: "当前对外展示",
            pending: "需要运营处理",
          }[key]
        }}</small>
      </div>
    </div>
    <div class="admin-workspace">
      <aside v-if="!categoryMode" class="admin-nav" aria-label="后台工作区">
        <span class="admin-nav-label">工作区</span>
        <button
          :class="{ active: tab === 'items' }"
          @click="
            tab = 'items';
            query = '';
          "
        >
          <b>01</b><span>信息管理</span><small>审核公开内容</small>
        </button>
        <button
          :class="{ active: tab === 'users' }"
          @click="
            tab = 'users';
            query = '';
          "
        >
          <b>02</b><span>用户列表</span><small>账号与权限</small>
        </button>
        <span class="admin-nav-label secondary">运营工具</span>
        <button
          v-for="entry in [
            { key: 'logs', no: '03', label: '操作日志', hint: '追踪平台动作' },
            { key: 'ads', no: '04', label: '广告管理', hint: '管理曝光位' },
            {
              key: 'promotions',
              no: '05',
              label: '推广管理',
              hint: '查看推广订单',
            },
            {
              key: 'delivery',
              no: '06',
              label: '配送管理',
              hint: '处理配送履约',
            },
            {
              key: 'couriers',
              no: '07',
              label: '配送员审核',
              hint: '审核接单资格',
            },
            {
              key: 'settings',
              no: '08',
              label: '平台配置',
              hint: '费用与规则',
            },
          ]"
          :key="entry.key"
          class="manage-tab"
          :class="{ active: tab === entry.key }"
          @click="tab = entry.key"
        >
          <b>{{ entry.no }}</b
          ><span>{{ entry.label }}</span
          ><small>{{ entry.hint }}</small>
        </button>
      </aside>
      <div class="admin-main">
        <form
          v-if="tab === 'items'"
          class="content-filters"
          role="search"
          @submit.prevent="applyItemFilters"
        >
          <label class="content-keyword"
            >{{ filterCopy.keyword
            }}<input
              v-model="query"
              aria-label="关键词"
              maxlength="100"
              :placeholder="filterCopy.placeholder"
          /></label>
          <label
            >发布状态<select v-model="statusFilter" aria-label="发布状态">
              <option value="">全部状态</option>
              <option
                v-for="state in [
                  'PENDING',
                  'APPROVED',
                  'HIDDEN',
                  'WITHDRAWN',
                  'DELETED',
                ]"
                :key="state"
                :value="state"
              >
                {{ status(state) }}
              </option>
            </select></label
          >
          <label
            >{{ filterCopy.location
            }}<input
              v-model="locationFilter"
              aria-label="所在地点"
              maxlength="100"
              placeholder="例如：图书馆"
          /></label>
          <button class="content-search" :disabled="loading || acting !== null">
            查询</button
          ><button
            type="button"
            :disabled="loading || acting !== null"
            @click="resetItemFilters"
          >
            重置
          </button>
        </form>
        <div v-if="tab === 'users'" class="admin-toolbar">
          <div class="toolbar-context">
            <span>USER DIRECTORY</span><strong>用户列表</strong>
          </div>
          <input
            v-model="query"
            placeholder="筛选本页账号或昵称"
            aria-label="搜索后台数据"
          />
          <button :disabled="loading || acting !== null" @click="load">
            刷新当前列表
          </button>
        </div>
        <p v-if="error" class="admin-alert" role="alert">{{ error }}</p>
        <p v-if="loading" class="admin-loading" role="status">正在加载数据…</p>
        <p v-if="resultMessage" class="content-success" role="status">
          {{ resultMessage }}
        </p>
        <OperationLogs v-if="tab === 'logs'" :request="request" />
        <AdManagement v-if="tab === 'ads'" :request="request" />
        <div v-if="tab === 'promotions'" class="admin-module">
          <PromotionOrders :request="request" admin />
        </div>
        <div v-if="tab === 'delivery'" class="admin-module">
          <DeliveryOrders :request="request" admin />
        </div>
        <CourierReview v-if="tab === 'couriers'" :request="request" />
        <section v-if="tab === 'settings'" class="settings-panel">
          <div class="module-heading">
            <div>
              <span>PLATFORM SETTINGS</span>
              <h3>商业化配置</h3>
            </div>
            <small>只影响新订单，历史订单保持不变</small>
          </div>
          <div class="setting-grid">
            <label
              >配送费<input
                v-model="settings['delivery.fee']"
                type="number"
                step="0.01" /></label
            ><label
              >配送抽成<input
                v-model="settings['delivery.platformRate']"
                type="number"
                step="0.01" /></label
            ><label
              >24 小时置顶价格<input
                v-model="settings['promotion.TOP_24H.amount']"
                type="number"
                step="0.01" /></label
            ><label
              >首页推荐价格<input
                v-model="settings['promotion.FEATURED_3D.amount']"
                type="number"
                step="0.01" /></label
            ><label
              >分类置顶价格<input
                v-model="settings['promotion.CATEGORY_7D.amount']"
                type="number"
                step="0.01"
            /></label>
          </div>
          <button
            class="active"
            :disabled="settingSaving"
            @click="saveSettings"
          >
            {{ settingSaving ? "保存中…" : "保存平台配置" }}
          </button>
        </section>
        <section v-if="overview" class="dashboard-panels">
          <div class="dashboard-panel">
            <div class="module-heading">
              <div>
                <span>CONTENT HEALTH</span>
                <h3>系统数据分布</h3>
              </div>
              <b>{{ stats.items || 0 }} 条信息</b>
            </div>
            <div class="donut-wrap">
              <div
                class="donut"
                :style="{
                  background: `conic-gradient(#e9a35c 0 ${((stats.approved || 0) / (stats.items || 1)) * 100}%,#dbe3e0 0 100%)`,
                }"
              >
                <b>{{ stats.approved || 0 }}</b
                ><small>公开信息</small>
              </div>
              <div class="legend">
                <p>
                  <i class="dot green"></i>公开信息
                  <b>{{ stats.approved || 0 }}</b>
                </p>
                <p>
                  <i class="dot blue"></i>待审核 <b>{{ stats.pending || 0 }}</b>
                </p>
                <p>
                  <i class="dot gray"></i>全部信息 <b>{{ stats.items || 0 }}</b>
                </p>
              </div>
            </div>
          </div>
          <div class="dashboard-panel">
            <div class="module-heading">
              <div>
                <span>REVENUE MIX</span>
                <h3>平台收入来源</h3>
              </div>
              <b>已支付订单</b>
            </div>
            <div class="source-row">
              <span>交易佣金</span
              ><i
                :style="{
                  width:
                    (Number(trade.commission || 0) /
                      (Number(trade.commission || 0) +
                        Number(trade.gross || 0.01))) *
                      100 +
                    '%',
                }"
              ></i
              ><b>¥{{ trade.commission || "0.00" }}</b>
            </div>
            <div class="source-row">
              <span>商品推广</span
              ><i
                :style="{
                  width:
                    (Number(trade.gross || 0) /
                      (Number(trade.gross || 0) +
                        Number(trade.commission || 0.01))) *
                      100 +
                    '%',
                }"
              ></i
              ><b>¥{{ trade.gross || "0.00" }}</b>
            </div>
            <div class="source-row">
              <span>配送抽成</span><i style="width: 35%"></i><b>待统计</b>
            </div>
            <p class="muted">按已支付订单统计，数据会随时间范围更新。</p>
          </div>
          <div class="dashboard-panel wide">
            <div class="module-heading">
              <div>
                <span>PLATFORM SNAPSHOT</span>
                <h3>系统运行数据</h3>
              </div>
              <b>实时摘要</b>
            </div>
            <table class="system-table">
              <tbody>
                <tr>
                  <td>注册用户</td>
                  <td>{{ stats.users || 0 }}</td>
                  <td>访客用户</td>
                  <td>{{ trade.visitors || 0 }}</td>
                  <td>成交订单</td>
                  <td>{{ trade.orders || 0 }}</td>
                </tr>
                <tr>
                  <td>交易总额</td>
                  <td>¥{{ trade.gross || "0.00" }}</td>
                  <td>平台佣金</td>
                  <td>¥{{ trade.commission || "0.00" }}</td>
                  <td>平均订单</td>
                  <td>
                    ¥{{
                      trade.orders
                        ? (Number(trade.gross || 0) / trade.orders).toFixed(2)
                        : "0.00"
                    }}
                  </td>
                </tr>
                <tr>
                  <td>公开信息</td>
                  <td>{{ stats.approved || 0 }}</td>
                  <td>待审核</td>
                  <td>{{ stats.pending || 0 }}</td>
                  <td>更新时间</td>
                  <td>{{ new Date().toLocaleString("zh-CN") }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>
        <div v-if="overview" class="trade-panel">
          <div class="module-heading">
            <div>
              <span>SECOND-HAND MARKET</span>
              <h3>二手交易数据</h3>
            </div>
            <small>已支付订单统计</small>
          </div>
          <div class="trade-cards">
            <div>
              <span>成交订单</span><b>{{ trade.orders ?? "—" }}</b>
            </div>
            <div>
              <span>交易金额</span><b>¥{{ trade.gross ?? "0.00" }}</b>
            </div>
            <div>
              <span>平台佣金</span><b>¥{{ trade.commission ?? "0.00" }}</b>
            </div>
            <div>
              <span>访客用户</span><b>{{ trade.visitors ?? "—" }}</b>
            </div>
          </div>
          <p>
            当前佣金比例：{{
              ((trade.commissionRate ?? 0.03) * 100).toFixed(1)
            }}% · 统计范围：已支付订单
          </p>
        </div>
        <div v-if="overview" class="trend-panel">
          <div class="module-heading">
            <div>
              <span>LAST 7 DAYS</span>
              <h3>近 7 日交易趋势</h3>
            </div>
            <small>金额与订单数量</small>
          </div>
          <div class="chart-wrap">
            <svg
              viewBox="0 0 700 220"
              role="img"
              aria-label="近7日交易金额趋势"
            >
              <polyline
                :points="chartPoints"
                fill="none"
                stroke="#e9a35c"
                stroke-width="4"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
              <circle
                v-for="(d, i) in trade.trend || []"
                :key="d.date"
                :cx="i * 100 + 25"
                :cy="chartY(d)"
                r="6"
                fill="#fff"
                stroke="#e9a35c"
                stroke-width="3"
              />
              <text
                v-for="(d, i) in trade.trend || []"
                :key="'label-' + d.date"
                :x="i * 100 + 25"
                y="215"
                text-anchor="middle"
              >
                {{ d.date.slice(5) }}
              </text>
            </svg>
          </div>
          <h3 class="orders-title">最近交易订单</h3>
          <table>
            <thead>
              <tr>
                <th>订单号</th>
                <th>金额</th>
                <th>佣金</th>
                <th>状态</th>
                <th>时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="order in tradeOrders" :key="order.id">
                <td>{{ order.orderNo }}</td>
                <td>¥{{ order.amount }}</td>
                <td>¥{{ order.commission }}</td>
                <td>{{ order.status }}</td>
                <td>{{ order.createdAt }}</td>
              </tr>
            </tbody>
          </table>
          <p v-if="!tradeOrders.length">暂无交易订单。</p>
          <Pager
            v-model:page="orderPage"
            v-model:size="pageSize"
            :total="orderTotal"
            :busy="loading"
          />
        </div>
        <section v-if="tab === 'items'" class="content-records">
          <div class="content-list-head">
            <div>
              <h3>
                {{
                  {
                    LOST: "逐条核查线索",
                    FOUND: "逐件核验物品",
                    SECOND_HAND: "商品审核目录",
                  }[workflowKind] || "全部信息记录"
                }}
                <span>{{ itemTotal }} 条匹配</span>
              </h3>
              <p>
                {{
                  {
                    LOST: "选择左侧记录，查看右侧核查内容。",
                    FOUND: "查看物品图片与联系信息，核实后执行审核。",
                    SECOND_HAND:
                      "资料情况表示字段是否填写，请结合详情人工核验。",
                  }[workflowKind] || "审核和下架会影响用户端的公开展示。"
                }}
              </p>
            </div>
            <button :disabled="loading || acting !== null" @click="load">
              刷新列表
            </button>
          </div>
          <CategoryRecords
            v-if="workflowKind"
            :kind="workflowKind"
            :items="items"
            :loading="loading"
            :acting="acting"
            @inspect="inspectItem"
            @moderate="setStatus"
          />
          <div v-else class="admin-table content-table">
            <table>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>物品信息</th>
                  <th>发布人</th>
                  <th>地点 / 类别</th>
                  <th>状态</th>
                  <th>发布时间</th>
                  <th>管理操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in items" :key="item.id">
                  <td>#{{ item.id }}</td>
                  <td class="content-title">
                    <strong>{{ item.title }}</strong>
                    <p>{{ item.description }}</p>
                  </td>
                  <td>#{{ item.userId }}</td>
                  <td>
                    {{ item.location
                    }}<small>{{ item.category || "未分类" }}</small>
                  </td>
                  <td>
                    <span class="content-status" :data-status="item.status">{{
                      status(item.status)
                    }}</span>
                  </td>
                  <td>
                    {{ item.createdAt?.replace("T", " ").slice(0, 16) || "—" }}
                  </td>
                  <td class="content-actions">
                    <ModerationActions
                      :item="item"
                      :busy="loading"
                      :acting="acting"
                      @inspect="inspectItem"
                      @moderate="setStatus"
                    />
                  </td>
                </tr>
                <tr v-if="!items.length && !loading">
                  <td colspan="7" class="content-empty">
                    没有匹配的信息，可调整筛选条件后重试。
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>
        <div v-else-if="tab === 'users'" class="admin-table">
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>账号</th>
                <th>昵称</th>
                <th>角色</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in visible" :key="item.id">
                <td>{{ item.id }}</td>
                <td>{{ item.username }}</td>
                <td>{{ item.nickname }}</td>
                <td>{{ item.role === "ADMIN" ? "管理员" : "普通用户" }}</td>
                <td>
                  {{
                    item.status === -1
                      ? "已删除"
                      : item.status === 1
                        ? "正常"
                        : "已禁用"
                  }}
                </td>
                <td>
                  <button
                    v-if="item.role !== 'ADMIN' && item.status !== -1"
                    :disabled="acting !== null || loading"
                    @click="manageUser(item)"
                  >
                    {{ item.status === 1 ? "禁用" : "恢复" }}</button
                  ><button
                    v-if="item.role !== 'ADMIN' && item.status !== -1"
                    :disabled="acting !== null || loading"
                    @click="manageUser(item, true)"
                  >
                    删除用户</button
                  ><span v-if="item.role === 'ADMIN' || item.status === -1">{{
                    item.role === "ADMIN" ? "管理员受保护" : "关联记录保留"
                  }}</span>
                </td>
              </tr>
              <tr v-if="!visible.length">
                <td colspan="6">暂无匹配数据</td>
              </tr>
            </tbody>
          </table>
        </div>
        <dialog
          ref="itemDialog"
          class="item-inspector"
          aria-labelledby="admin-item-title"
          @close="inspecting = null"
          @click.self="itemDialog.close()"
        >
          <template v-if="inspecting"
            ><div class="inspector-heading">
              <div>
                <span
                  >信息 #{{ inspecting.id }} ·
                  {{ status(inspecting.status) }}</span
                >
                <h2 id="admin-item-title">{{ inspecting.title }}</h2>
              </div>
              <button aria-label="关闭管理详情" @click="itemDialog.close()">
                ×
              </button>
            </div>
            <img
              v-if="inspecting.imageUrl"
              :src="itemImage(inspecting.imageUrl)"
              :alt="inspecting.title"
            />
            <p class="inspector-description">{{ inspecting.description }}</p>
            <dl>
              <div>
                <dt>发布人 ID</dt>
                <dd>{{ inspecting.userId }}</dd>
              </div>
              <div>
                <dt>地点</dt>
                <dd>{{ inspecting.location || "未填写" }}</dd>
              </div>
              <div>
                <dt>物品类别</dt>
                <dd>{{ inspecting.category || "未分类" }}</dd>
              </div>
              <div>
                <dt>联系方式</dt>
                <dd>{{ inspecting.contact || "未填写" }}</dd>
              </div>
              <div>
                <dt>发布时间</dt>
                <dd>{{ inspecting.createdAt?.replace("T", " ") || "—" }}</dd>
              </div>
            </dl></template
          >
        </dialog>
        <Pager
          v-if="tab === 'users'"
          v-model:page="userPage"
          v-model:size="pageSize"
          :total="userTotal"
          :busy="loading || acting !== null"
        /><Pager
          v-else-if="tab === 'items'"
          v-model:page="itemPage"
          v-model:size="pageSize"
          :total="itemTotal"
          :busy="loading || acting !== null"
        />
      </div>
    </div>
  </section>
</template>
<style scoped>
.admin-view:not(.overview) .dashboard-panels,
.admin-view:not(.overview) .trade-panel,
.admin-view:not(.overview) .trend-panel {
  display: none;
}
.admin-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 24px;
  padding-bottom: 26px;
  border-bottom: 1px solid #e4d8c7;
}
.admin-kicker,
.module-heading span,
.toolbar-context span {
  font-size: 10px;
  letter-spacing: 2px;
  color: #b06d37;
  font-weight: 700;
}
.admin-head h2 {
  margin: 10px 0 6px;
  font-size: 30px;
  color: #163b55;
}
.admin-head p {
  margin: 0;
  color: #758287;
}
.admin-head-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}
.admin-sync {
  font-size: 12px;
  color: #648276;
}
.admin-sync i {
  display: inline-block;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #6fa47a;
  margin-right: 6px;
}
.admin-refresh,
.admin-view button {
  border: 1px solid #d8c8b3;
  background: #fffaf2;
  color: #7e552f;
  border-radius: 9px;
  padding: 10px 14px;
}
.admin-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  padding: 24px 0;
  border-bottom: 1px solid #e4d8c7;
}
.admin-stats > div {
  padding: 18px 20px;
  background: #fffaf2;
  border: 1px solid #e2d5c2;
  border-radius: 14px;
}
.admin-stats span,
.admin-stats small {
  display: block;
  color: #7b8788;
}
.admin-stats strong {
  display: block;
  font-size: 32px;
  color: #163b55;
  margin: 9px 0 4px;
}
.admin-stats small {
  font-size: 11px;
}
.date-filter {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin: 22px 0;
  padding: 16px;
  background: #fffaf2;
  border: 1px solid #e2d5c2;
  border-radius: 14px;
}
.filter-title {
  display: flex;
  flex-direction: column;
  margin-right: 8px;
}
.filter-title span {
  font-size: 10px;
  color: #b06d37;
  letter-spacing: 1px;
}
.filter-title strong {
  color: #163b55;
}
.filter-presets {
  display: flex;
  gap: 5px;
}
.date-filter button {
  border: 1px solid #dccbb4;
  background: #f5ead9;
  color: #7d542e;
  border-radius: 8px;
  padding: 8px 11px;
}
.date-filter button.active {
  background: #e9a35c;
  color: #163b55;
  border-color: #e9a35c;
}
.date-filter label {
  display: flex;
  align-items: center;
  gap: 5px;
  color: #667578;
  font-size: 12px;
}
.date-filter input {
  padding: 8px;
  border: 1px solid #dccbb4;
  border-radius: 8px;
  background: #fff;
}
.date-filter span {
  font-size: 12px;
  color: #8c7c6b;
}
.admin-workspace {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 26px;
  padding-top: 26px;
}
.admin-nav {
  display: flex;
  flex-direction: column;
  gap: 7px;
}
.admin-nav-label {
  font-size: 10px;
  letter-spacing: 1.6px;
  color: #9d8060;
  margin: 0 8px 7px;
}
.admin-nav-label.secondary {
  margin-top: 20px;
}
.admin-nav button {
  display: grid;
  grid-template-columns: 30px 1fr;
  grid-template-rows: auto auto;
  column-gap: 9px;
  text-align: left;
  border: 0;
  background: transparent;
  color: #637278;
  border-radius: 11px;
  padding: 12px 10px;
}
.admin-nav button b {
  grid-row: 1/3;
  color: #b9a38b;
  font-size: 11px;
  padding-top: 2px;
}
.admin-nav button span {
  font-size: 13px;
  font-weight: 700;
  color: #405b67;
}
.admin-nav button small {
  font-size: 10px;
  color: #9b9990;
  margin-top: 3px;
}
.admin-nav button.active {
  background: #163b55;
  box-shadow: 0 8px 18px #163b5520;
}
.admin-nav button.active span,
.admin-nav button.active b {
  color: #fff;
}
.admin-nav button.active small {
  color: #b9cbd1;
}
.admin-nav button:hover:not(.active) {
  background: #f0e6d7;
}
.admin-main {
  min-width: 0;
}
.admin-toolbar {
  display: flex;
  align-items: end;
  gap: 12px;
  min-height: 52px;
  margin-bottom: 16px;
}
.toolbar-context {
  display: flex;
  flex-direction: column;
  gap: 5px;
  margin-right: auto;
}
.toolbar-context strong {
  font-size: 21px;
  color: #163b55;
}
.admin-toolbar input {
  width: 280px;
  padding: 11px 13px;
  border: 1px solid #d8c8b3;
  border-radius: 9px;
  background: #fffaf2;
}
.admin-toolbar button {
  border: 1px solid #d8c8b3;
  border-radius: 9px;
  background: #163b55;
  color: #fff;
  padding: 11px 14px;
}
.admin-alert {
  padding: 12px 14px;
  background: #fff0ed;
  color: #a44335;
  border-radius: 9px;
}
.admin-loading {
  color: #9b8167;
}
.admin-module,
.settings-panel,
.dashboard-panel,
.trade-panel,
.trend-panel {
  background: #fffaf2;
  border: 1px solid #e2d5c2;
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 18px;
}
.module-heading {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 20px;
}
.module-heading h3 {
  margin: 7px 0 0;
  color: #163b55;
  font-size: 19px;
}
.module-heading > b,
.module-heading > small {
  color: #9a8977;
  font-size: 11px;
}
.setting-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}
.setting-grid label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: #667578;
  font-size: 12px;
  font-weight: 700;
}
.setting-grid input {
  padding: 10px;
  border: 1px solid #d8c8b3;
  border-radius: 8px;
  background: #fff;
}
.settings-panel > button {
  margin-top: 18px;
  border: 0;
  border-radius: 9px;
  background: #e9a35c;
  color: #163b55;
  padding: 11px 16px;
  font-weight: 700;
}
.dashboard-panels {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
}
.dashboard-panel.wide {
  grid-column: 1/-1;
}
.donut-wrap {
  display: flex;
  align-items: center;
  gap: 22px;
}
.donut {
  width: 138px;
  height: 138px;
  border-radius: 50%;
  display: grid;
  place-content: center;
  text-align: center;
  position: relative;
}
.donut:after {
  content: "";
  position: absolute;
  inset: 21px;
  background: #fffaf2;
  border-radius: 50%;
}
.donut b,
.donut small {
  z-index: 1;
}
.donut b {
  font-size: 28px;
  color: #163b55;
}
.donut small {
  font-size: 11px;
  color: #7c8c8f;
}
.legend p {
  margin: 10px 0;
  color: #6d7d80;
}
.dot {
  display: inline-block;
  width: 9px;
  height: 9px;
  border-radius: 50%;
  margin-right: 7px;
}
.dot.green {
  background: #6fa47a;
}
.dot.blue {
  background: #5f90b1;
}
.dot.gray {
  background: #d6ddd8;
}
.source-row {
  display: grid;
  grid-template-columns: 90px 1fr 80px;
  gap: 10px;
  align-items: center;
  margin: 18px 0;
  color: #6e7c7e;
}
.source-row i {
  height: 9px;
  background: #e9a35c;
  border-radius: 10px;
}
.source-row b {
  text-align: right;
  color: #8b5e32;
}
.muted {
  font-size: 12px;
  color: #8b908c;
}
.system-table,
.trend-panel table {
  width: 100%;
  border-collapse: collapse;
}
.system-table td,
.trend-panel th,
.trend-panel td {
  padding: 13px 10px;
  border-bottom: 1px solid #ecdfcf;
  color: #6d7b7d;
}
.system-table td:nth-child(even) {
  font-weight: 700;
  color: #163b55;
  text-align: right;
}
.trade-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}
.trade-cards div {
  padding: 16px;
  border: 1px solid #ead7bd;
  border-radius: 12px;
  background: #fff7e9;
}
.trade-cards span {
  display: block;
  font-size: 12px;
  color: #8c8378;
}
.trade-cards b {
  display: block;
  color: #163b55;
  font-size: 27px;
  margin-top: 8px;
}
.chart-wrap {
  padding: 12px;
  background: #fff;
  border: 1px solid #eadfce;
  border-radius: 12px;
}
.chart-wrap svg {
  width: 100%;
  height: 250px;
}
.chart-wrap text {
  font-size: 12px;
  fill: #8a8378;
}
.orders-title {
  margin-top: 24px;
  padding-top: 18px;
  border-top: 1px solid #ecdfcf;
  color: #163b55;
}
.admin-table {
  overflow: auto;
  border: 1px solid #e2d5c2;
  border-radius: 14px;
  background: #fffaf2;
}
.admin-table table {
  width: 100%;
  border-collapse: collapse;
}
.admin-table th,
.admin-table td {
  padding: 14px;
  text-align: left;
  border-bottom: 1px solid #ecdfcf;
  white-space: nowrap;
}
.admin-table th {
  font-size: 11px;
  color: #987b5b;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.admin-table td {
  color: #53666b;
}
.admin-table td p {
  max-width: 340px;
  overflow-wrap: anywhere;
  white-space: pre-wrap;
  color: #7d888a;
}
.admin-table td button {
  border: 1px solid #d8c8b3;
  border-radius: 7px;
  background: #f5ead9;
  color: #7e552f;
  padding: 8px 10px;
  margin-right: 5px;
}
.admin-table td button:hover {
  background: #e9a35c;
}
.admin-view .list-pager {
  margin-top: 16px;
}
@media (min-width: 901px) {
  .category-management[data-workflow="LOST"],
  .category-management[data-workflow="FOUND"] {
    padding: 0;
    border: 0;
    border-radius: 0;
    background: transparent;
    box-shadow: none;
  }
  .category-management[data-workflow="LOST"] .admin-head,
  .category-management[data-workflow="FOUND"] .admin-head {
    border-bottom: 0;
    padding: 3px 0 18px;
  }
  .lost-summary {
    display: grid;
    grid-template-columns: 1.3fr repeat(3, minmax(0, 1fr));
    gap: 0;
    border: 1px solid #d9d5c6;
    border-radius: 12px;
    background: #fffdf7;
    padding: 18px 10px;
    margin-top: 8px;
  }
  .lost-summary-title {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 0 20px;
  }
  .lost-summary-title > svg {
    width: 36px;
    height: 36px;
    color: #9c6f32;
  }
  .lost-summary-title > div {
    display: flex;
    align-items: baseline;
    gap: 18px;
    flex-wrap: wrap;
  }
  .lost-summary-title span {
    font-size: 13px;
    color: #627475;
  }
  .lost-summary-title strong {
    font-size: 32px;
    color: #183d50;
    font-variant-numeric: tabular-nums;
  }
  .lost-summary > button {
    display: grid;
    grid-template-columns: 1fr auto;
    text-align: left;
    align-items: center;
    column-gap: 12px;
    border: 0;
    border-left: 1px solid #e5dfd0;
    border-radius: 0;
    background: transparent;
    padding: 6px 25px;
    color: #183d50;
  }
  .lost-summary > button b {
    font-size: 25px;
    font-weight: 600;
    grid-column: 1;
    grid-row: 1;
  }
  .lost-summary > button span {
    font-size: 12px;
    color: #627475;
    grid-column: 1;
    grid-row: 2;
    margin-top: 5px;
  }
  .lost-summary > button i {
    font-style: normal;
    color: #8b713c;
    grid-row: 1/3;
    grid-column: 2;
  }
  .lost-summary > button[aria-pressed="true"] {
    background: #f8edda;
    border-radius: 8px;
  }
  .category-management[data-workflow="LOST"] .content-filters {
    background: #fffdf7;
    border-color: #d9d5c6;
  }
  .found-intake {
    display: flex;
    align-items: center;
    gap: 24px;
    flex-wrap: wrap;
    background: #e8f0e5;
    border: 1px solid #cbd9c6;
    border-radius: 13px;
    padding: 22px 26px;
    margin-top: 8px;
  }
  .found-intake-title {
    display: flex;
    align-items: center;
    gap: 14px;
  }
  .found-intake-title > svg {
    width: 42px;
    height: 42px;
    color: #41704f;
  }
  .found-intake-title span {
    display: block;
    color: #4f6d5a;
    font-size: 12px;
  }
  .found-intake-title strong {
    display: block;
    margin-top: 7px;
    font-size: 13px;
    color: #294a42;
    font-weight: 400;
  }
  .found-intake-title strong b {
    font-size: 29px;
    font-weight: 600;
  }
  .found-intake ol {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 20px;
    flex: 1;
    list-style: none;
    margin: 0;
    padding: 0;
    flex-wrap: wrap;
  }
  .found-intake li {
    font-size: 12px;
    color: #466351;
    white-space: nowrap;
  }
  .found-intake li > b {
    display: inline-grid;
    place-items: center;
    margin-right: 8px;
    width: 24px;
    height: 24px;
    background: #fffef7;
    border-radius: 50%;
    font-size: 10px;
    color: #41704f;
    font-weight: 500;
  }
  .found-intake > button {
    display: flex;
    align-items: center;
    gap: 18px;
    border: 1px solid #b6cbb5;
    background: #fffef7;
    border-radius: 9px;
    padding: 12px 18px;
    color: #32664f;
    font-size: 12px;
  }
  .found-intake > button strong {
    font-size: 24px;
  }
  .found-intake > button[aria-pressed="true"] {
    border-color: #32664f;
    background: #d5e6d5;
  }
  .category-management[data-workflow="FOUND"] .content-filters {
    background: #fffdf7;
    border-color: #cbd9c6;
    padding: 16px 20px;
  }
  .market-stock {
    display: flex;
    align-items: center;
    gap: 28px;
    justify-content: space-between;
    flex-wrap: wrap;
    padding: 22px 0;
    border-bottom: 1px solid #d7dfdc;
  }
  .market-stock > div {
    display: flex;
    align-items: baseline;
    gap: 12px;
    flex-wrap: wrap;
  }
  .market-stock > div > span {
    font-size: 13px;
    color: #526970;
  }
  .market-stock strong {
    font-size: 34px;
    color: #183d50;
  }
  .market-stock small {
    font-size: 11px;
    color: #637477;
  }
  .market-stock nav {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }
  .market-stock button {
    display: flex;
    gap: 8px;
    align-items: center;
    border: 1px solid transparent;
    background: transparent;
    color: #526970;
    font-size: 12px;
    padding: 10px 12px;
  }
  .market-stock button b {
    border-radius: 4px;
    background: #edf1ed;
    color: #526970;
    padding: 3px 6px;
    font-weight: 500;
  }
  .market-stock button[aria-pressed="true"] {
    color: #183d50;
    background: #edf2f3;
    border-color: #b7cbd0;
  }
  .category-management[data-workflow="SECOND_HAND"] .admin-head {
    padding-bottom: 18px;
    border-bottom: 0;
  }
  .category-management[data-workflow="SECOND_HAND"] .content-filters {
    background: #fff;
    border: 0;
    border-bottom: 1px solid #dde2dc;
    border-radius: 0;
    padding: 0 0 21px;
  }
  .category-management {
    padding: 28px;
    background: #fffdf9;
    border: 1px solid #ded9ce;
    border-radius: 20px;
    box-shadow: 0 10px 32px #193d5808;
  }
  .category-management .admin-head {
    align-items: center;
    margin-bottom: 0;
    padding-bottom: 24px;
  }
  .category-management .admin-head p {
    max-width: 58em;
    line-height: 1.8;
  }
  .category-management .admin-workspace {
    grid-template-columns: minmax(0, 1fr);
    padding-top: 22px;
  }
  .content-kpis {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 14px;
    margin-top: 24px;
  }
  .content-kpis > div {
    padding: 18px 20px;
    border: 1px solid #e0e2da;
    border-radius: 12px;
    background: #f7f8f4;
  }
  .content-kpis > div:nth-child(2) {
    background: #fbf1e2;
    border-color: #eed9b8;
  }
  .content-kpis > div:nth-child(3) {
    background: #edf4ee;
    border-color: #d0e0d3;
  }
  .content-kpis span,
  .content-kpis small {
    display: block;
    color: #5c6c70;
    font-size: 12px;
  }
  .content-kpis strong {
    display: block;
    margin: 9px 0 6px;
    font-size: 30px;
    color: #163b55;
    font-variant-numeric: tabular-nums;
  }
  .content-kpis small {
    font-size: 11px;
  }
  .content-filters {
    display: flex;
    align-items: flex-end;
    gap: 12px;
    flex-wrap: wrap;
    padding: 18px;
    background: #f7f7f2;
    border: 1px solid #deded3;
    border-radius: 12px;
    margin-bottom: 22px;
  }
  .content-filters label {
    display: flex;
    flex-direction: column;
    gap: 8px;
    flex: 1;
    min-width: 120px;
    font-size: 12px;
    font-weight: 600;
    color: #435d65;
  }
  .content-filters label.content-keyword {
    flex: 2;
    min-width: 220px;
  }
  .content-filters input,
  .content-filters select {
    min-width: 0;
    width: 100%;
    height: 40px;
    padding: 9px 11px;
    font: inherit;
    border: 1px solid #becac7;
    border-radius: 7px;
    background: #fff;
    color: #244855;
  }
  .content-filters button {
    min-height: 40px;
    border-color: #bdcbc6;
    background: #fff;
  }
  .content-filters button.content-search {
    background: #204e65;
    color: #fff;
    border-color: #204e65;
  }
  .content-list-head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 16px;
    margin-bottom: 14px;
  }
  .content-list-head h3 {
    margin: 0;
    font-size: 18px;
    color: #183d50;
  }
  .content-list-head h3 span {
    margin-left: 10px;
    font-weight: 400;
    font-size: 12px;
    color: #647477;
  }
  .content-list-head p {
    margin: 7px 0 0;
    font-size: 12px;
    color: #647477;
  }
  .content-table {
    background: #fff;
    border-color: #d7dfd9;
  }
  .content-table th {
    background: #f2f5f1;
    color: #52696e;
    font-size: 12px;
    letter-spacing: 0;
  }
  .content-table td {
    background: #fff;
    font-size: 12px;
    border-color: #e4e8e1;
    padding: 16px 12px;
  }
  .content-table th {
    padding: 13px 12px;
  }
  .content-table tr:hover td {
    background: #fafbf7;
  }
  .content-table td.content-title {
    min-width: 180px;
    max-width: 280px;
    white-space: normal;
  }
  .content-table td.content-title strong {
    font-size: 13px;
    color: #183d50;
    line-height: 1.6;
  }
  .content-table td.content-title p {
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    font-size: 12px;
    line-height: 1.7;
    color: #637477;
    margin: 7px 0 0;
    white-space: normal;
  }
  .content-table td small {
    display: block;
    margin-top: 7px;
    font-size: 11px;
    color: #647477;
  }
  .content-status {
    display: inline-block;
    padding: 5px 8px;
    border-radius: 5px;
    background: #eaf3ec;
    color: #32664f;
    font-size: 11px;
  }
  .content-status[data-status="PENDING"] {
    background: #fbedd2;
    color: #855b1d;
  }
  .content-status[data-status="HIDDEN"] {
    background: #fae8e3;
    color: #9b4939;
  }
  .content-status[data-status="WITHDRAWN"],
  .content-status[data-status="DELETED"] {
    background: #eef0ed;
    color: #657471;
  }
  .content-table td.content-actions button {
    margin: 3px 4px 3px 0;
    padding: 7px 9px;
    background: #edf3f4;
    color: #245570;
    border-color: #c9dadf;
  }
  .content-table td.content-actions button.content-hide {
    color: #9b4939;
    background: #fff3ed;
    border-color: #eac5b9;
  }
  .content-table td.content-actions button:disabled,
  .admin-view button:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
  .content-table td.content-empty {
    text-align: center;
    padding: 48px 16px;
    color: #637477;
  }
  .content-success {
    padding: 12px 14px;
    background: #e9f4eb;
    color: #32664f;
    border-radius: 9px;
    font-size: 13px;
  }
  .item-inspector {
    width: min(720px, 80vw);
    max-height: 85vh;
    overflow: auto;
    padding: 28px;
    border: 1px solid #d7dfd9;
    border-radius: 16px;
    color: #183d50;
    background: #fffdf9;
    box-shadow: 0 25px 80px #142c4538;
  }
  .item-inspector::backdrop {
    background: #102b4066;
    backdrop-filter: blur(3px);
  }
  .inspector-heading {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 20px;
    border-bottom: 1px solid #e1e4db;
    padding-bottom: 15px;
  }
  .inspector-heading span {
    color: #637477;
    font-size: 12px;
  }
  .inspector-heading h2 {
    font-size: 23px;
    margin: 10px 0 0;
    overflow-wrap: anywhere;
  }
  .inspector-heading button {
    font-size: 22px;
    padding: 4px 12px;
  }
  .item-inspector img {
    display: block;
    max-width: 100%;
    height: 220px;
    object-fit: contain;
    margin: 20px auto;
  }
  .inspector-description {
    white-space: pre-wrap;
    overflow-wrap: anywhere;
    line-height: 1.9;
    color: #4e656d;
    margin: 22px 0;
  }
  .item-inspector dl {
    border-top: 1px solid #e1e4db;
    padding-top: 8px;
  }
  .item-inspector dl > div {
    display: grid;
    grid-template-columns: 110px minmax(0, 1fr);
    gap: 16px;
    padding: 9px 0;
    font-size: 13px;
  }
  .item-inspector dt {
    color: #637477;
  }
  .item-inspector dd {
    margin: 0;
    overflow-wrap: anywhere;
  }
}
</style>
