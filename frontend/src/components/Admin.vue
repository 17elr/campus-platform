<script setup>
import { ref, computed, onMounted } from "vue";
const props = defineProps({ request: Function });
const emit = defineEmits(["changed"]);
const tab = ref("items"),
  query = ref(""),
  stats = ref({}), trade = ref({}), tradeOrders = ref([]),
  users = ref([]),
  items = ref([]),
  loading = ref(false),
  error = ref(""),
  acting = ref(null);
const userPage = ref(1), itemPage = ref(1), pageSize = 10;
const trendMax = computed(() => Math.max(1, ...(trade.value.trend || []).map(x => Number(x.gross) || 0)));
const chartY = (d) => 190 - (Number(d.gross || 0) / trendMax.value) * 160;
const chartPoints = computed(() => (trade.value.trend || []).map((d,i) => (i*100+25)+","+chartY(d)).join(" ")); const visible = computed(() =>
  (tab.value === "users" ? users.value : items.value).filter((x) =>
    [x.id, x.username, x.nickname, x.title, x.location]
      .join(" ")
      .includes(query.value.trim()),
  ),
);
const status = (s) =>
  ({ APPROVED: "已公开", PENDING: "待审核", HIDDEN: "已下架" })[s] || s;
async function load() {
  loading.value = true;
  error.value = "";
  try {
    const data = await Promise.all(
      ["/stats", "/users?page=" + userPage.value + "&size=" + pageSize, "/items?page=" + itemPage.value + "&size=" + pageSize, "/trade/stats", "/trade/orders"].map((p) =>
        props.request("/admin" + p, undefined, "get").catch(() => p === "/stats" ? {} : p.includes("trade/stats") ? {orders:0,gross:0,commission:0,visitors:0,commissionRate:0.03,trend:[]} : p.includes("trade/orders") ? [] : {records:[],total:0}),
      ),
    );
    [stats.value, users.value, items.value, trade.value, tradeOrders.value] = [data[0], data[1]?.records || data[1] || [], data[2]?.records || data[2] || [], data[3], data[4]?.records || data[4] || []];
  } catch (e) {
    error.value = e.response?.data?.message || e.message;
  } finally {
    loading.value = false;
  }
}
async function setStatus(item) {
  const next = item.status === "APPROVED" ? "HIDDEN" : "APPROVED";
  if (
    !window.confirm(`${next === "HIDDEN" ? "下架" : "公开"}“${item.title}”？`)
  )
    return;
  acting.value = item.id;
  error.value = "";
  try {
    await props.request(
      `/admin/items/${item.id}/status`,
      { status: next },
      "put",
    );
    await load();
    emit("changed");
  } catch (e) {
    error.value = e.response?.data?.message || e.message;
  } finally {
    acting.value = null;
  }
}
async function manageUser(user, remove = false) {
  if(acting.value!==null)return;
  const next=user.status===1?0:1;
  const message=remove ? `删除用户“${user.username}”？账号将永久停用，关联记录保留。` : `${next===0?'禁用':'恢复'}用户“${user.username}”？`;
  if(!window.confirm(message))return;
  acting.value=user.id;error.value='';
  try{
    await props.request(`/admin/users/${user.id}${remove?'':'/status'}`,remove?undefined:{status:next},remove?'delete':'put');
    await load();
  }catch(e){error.value=e.response?.data?.message||e.message;}
  finally{acting.value=null;}
}
onMounted(load);
</script>
<template>
  <section class="admin-view">
    <h2>管理员后台</h2>
    <div class="admin-stats">
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
        ><strong>{{ stats[key] ?? "—" }}</strong>
      </div>
    </div>
    <nav class="admin-toolbar" aria-label="后台栏目">
      <button
        :class="{ active: tab === 'items' }"
        @click="
          tab = 'items';
          query = '';
        "
      >
        信息管理</button
      ><button
        :class="{ active: tab === 'users' }"
        @click="
          tab = 'users';
          query = '';
        "
      >
        用户列表</button
      ><input
        v-model="query"
        :placeholder="tab === 'users' ? '搜索账号或昵称' : '搜索标题、地点或描述'"
        aria-label="搜索后台数据"
      /><button :disabled="loading || acting !== null" @click="load">
        刷新
      </button>
    </nav>
    <p v-if="error" role="alert">{{ error }}</p>
    <p v-if="loading" role="status">正在加载…</p>
    <div class="trade-panel"><h3>二手交易数据</h3><div class="trade-cards"><div><span>成交订单</span><b>{{trade.orders ?? "—"}}</b></div><div><span>交易金额</span><b>¥{{trade.gross ?? "0.00"}}</b></div><div><span>平台佣金</span><b>¥{{trade.commission ?? "0.00"}}</b></div><div><span>访客用户</span><b>{{trade.visitors ?? "—"}}</b></div></div><p>当前佣金比例：{{((trade.commissionRate ?? 0.03)*100).toFixed(1)}}% · 统计范围：已支付订单</p></div><div class="trend-panel"><h3>近7日交易趋势</h3><div class="chart-wrap"><svg viewBox="0 0 700 220" role="img" aria-label="近7日交易金额趋势"><polyline :points="chartPoints" fill="none" stroke="#2b8a78" stroke-width="4" stroke-linecap="round" stroke-linejoin="round"/><circle v-for="(d,i) in (trade.trend || [])" :key="d.date" :cx="i*100+25" :cy="chartY(d)" r="6" fill="#fff" stroke="#2b8a78" stroke-width="3"/><text v-for="(d,i) in (trade.trend || [])" :key="`label-${d.date}`" :x="i*100+25" y="215" text-anchor="middle">{{d.date.slice(5)}}</text></svg></div><div class="trend-row" v-for="day in (trade.trend || [])" :key="day.date"><span>{{day.date}}</span><i :style="{width: Math.min(100, day.orders * 18 + 4) + '%' }"></i><b>{{day.orders}} 单 · ¥{{day.gross}}</b></div><h3 class="orders-title">最近交易订单</h3><table><thead><tr><th>订单号</th><th>金额</th><th>佣金</th><th>状态</th><th>时间</th></tr></thead><tbody><tr v-for="o in tradeOrders.slice(0,8)" :key="o.id"><td>{{o.orderNo}}</td><td>¥{{o.amount}}</td><td>¥{{o.commission}}</td><td>{{o.status}}</td><td>{{o.createdAt}}</td></tr></tbody></table></div><div class="admin-table">
      <table>
        <thead>
          <tr v-if="tab === 'users'">
            <th>ID</th>
            <th>账号</th>
            <th>昵称</th>
            <th>角色</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
          <tr v-else>
            <th>ID</th>
            <th>标题</th>
            <th>发布人ID</th>
            <th>地点</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="x in visible" :key="x.id">
            <td>{{ x.id }}</td>
            <template v-if="tab === 'users'"
              ><td>{{ x.username }}</td>
              <td>{{ x.nickname }}</td>
              <td>{{ x.role === "ADMIN" ? "管理员" : "普通用户" }}</td>
              <td>{{ x.status === -1 ? "已删除" : x.status === 1 ? "正常" : "已禁用" }}</td>
              <td><template v-if="x.role !== 'ADMIN' && x.status !== -1"><button :disabled="acting!==null || loading" @click="manageUser(x)">{{x.status===1?'禁用':'恢复'}}</button> <button :disabled="acting!==null || loading" @click="manageUser(x,true)">删除用户</button></template><span v-else>{{x.role==='ADMIN'?'管理员受保护':'关联记录保留'}}</span></td></template
            ><template v-else
              ><td>
                <strong>{{ x.title }}</strong>
                <p>{{ x.description }}</p>
              </td>
              <td>{{ x.userId }}</td>
              <td>{{ x.location }}</td>
              <td>{{ status(x.status) }}</td>
              <td>
                <button :disabled="acting !== null" @click="setStatus(x)">
                  {{
                    acting === x.id
                      ? "处理中…"
                      : x.status === "APPROVED"
                        ? "下架"
                        : "公开"
                  }}
                </button>
              </td></template
            >
          </tr>
          <tr v-if="!visible.length">
            <td colspan="6">暂无匹配数据</td>
          </tr>
        </tbody>
      </table>
    </div>
  <div class="pager"><button :disabled="(tab === 'users' ? userPage : itemPage) <= 1" @click="tab === 'users' ? userPage-- : itemPage--; load()">上一页</button><span>第 {{ tab === 'users' ? userPage : itemPage }} 页</span><button :disabled="visible.length < pageSize" @click="tab === 'users' ? userPage++ : itemPage++; load()">下一页</button></div></section>
</template>
<style scoped>
.admin-stats {
  display: flex;
  gap: 32px;
  flex-wrap: wrap;
  padding: 20px 0;
  border-bottom: 1px solid #d9e2e8;
}
.admin-stats span {
  display: block;
  color: #647480;
}
.admin-stats strong {
  font-size: 26px;
}
.admin-toolbar {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin: 24px 0;
}
.admin-toolbar input {
  flex: 1;
  min-width: 150px;
  padding: 10px;
  border: 1px solid #cbd5df;
  border-radius: 4px;
}
.admin-view button {
  padding: 10px 14px;
  border-radius: 4px;
  background: #e2e9ef;
  color: #25465c;
}
.admin-view button.active {
  background: #216ab2;
  color: white;
}
.admin-view button:disabled {
  opacity: 0.5;
}
.admin-table {
  overflow: auto;
}
table {
  border-collapse: collapse;
  width: 100%;
  background: white;
}
th,
td {
  text-align: left;
  padding: 14px;
  border-bottom: 1px solid #e0e7ed;
}
td p {
  max-width: 400px;
  overflow-wrap: anywhere;
  white-space: pre-wrap;
  color: #63727e;
}
th {
  white-space: nowrap;
}
td button {
  white-space: nowrap;
}
[role="alert"] {
  color: #b34332;
}
</style>










<style scoped>
.trade-panel,.trend-panel{margin:24px 0;padding:24px;background:#fff;border:1px solid #e1ebe7;border-radius:16px;box-shadow:0 10px 30px #1d5b4b0b}.trade-panel h3,.trend-panel h3{font-size:20px;color:#244b4a;margin:0 0 18px}.trade-cards{display:grid;grid-template-columns:repeat(4,1fr);gap:14px}.trade-cards div{padding:18px;border-radius:12px;background:linear-gradient(135deg,#edf8f3,#fff);border:1px solid #dceee6}.trade-cards span{display:block;color:#78918a;font-size:12px}.trade-cards b{display:block;color:#1f6d61;font-size:28px;margin-top:8px}.chart-wrap{padding:16px 8px;background:#f7fbf9;border-radius:12px;border:1px solid #e1eee8}.chart-wrap svg{width:100%;height:280px}.chart-wrap text{font-size:12px;fill:#718c84}.trend-row{display:none}.trend-panel table{width:100%;margin-top:20px;border-collapse:collapse}.trend-panel th,.trend-panel td{padding:12px;border-bottom:1px solid #edf2ef;text-align:left}.orders-title{margin-top:24px!important;padding-top:18px;border-top:1px solid #edf2ef}@media(max-width:800px){.trade-cards{grid-template-columns:repeat(2,1fr)}}
</style>





