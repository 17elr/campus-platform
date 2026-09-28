<script setup>
import { ref, computed, onMounted, watch } from "vue";
import Pager from "./Pager.vue";
import PromotionOrders from "./PromotionOrders.vue";
import DeliveryOrders from "./DeliveryOrders.vue";
import CourierReview from "./CourierReview.vue";
import AdManagement from "./AdManagement.vue";
const props = defineProps({ request: Function, overview: Boolean });
const emit = defineEmits(["changed"]);
const tab = ref("items"),
  query = ref(""),
  stats = ref({}), trade = ref({}), tradeOrders = ref([]),
  users = ref([]),
  items = ref([]),
  loading = ref(false),
  error = ref(""),
  acting = ref(null);
const settings=ref({}),settingSaving=ref(false),dateFrom=ref(""),dateTo=ref("");
const userPage=ref(1),itemPage=ref(1),orderPage=ref(1),pageSize=ref(10),userTotal=ref(0),itemTotal=ref(0),orderTotal=ref(0);
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
let loadSequence=0;
async function load() {
  const seq=++loadSequence;
  loading.value = true;
  error.value = "";
  try {
    const data = await Promise.all(
      ["/stats", "/users?page=" + userPage.value + "&size=" + pageSize.value, "/items?page=" + itemPage.value + "&size=" + pageSize.value, "/trade/stats?"+(dateFrom.value?"from="+dateFrom.value+"&":"")+(dateTo.value?"to="+dateTo.value:""), "/trade/orders?page="+orderPage.value+"&size="+pageSize.value, "/settings"].map((p) =>
        props.request("/admin" + p, undefined, "get"),
      ),
    );
    if(seq!==loadSequence)return;
    userTotal.value=data[1].total;itemTotal.value=data[2].total;orderTotal.value=data[4].total;settings.value=data[5]||{};
    [stats.value, users.value, items.value, trade.value, tradeOrders.value] = [data[0], data[1]?.records || data[1] || [], data[2]?.records || data[2] || [], data[3], data[4]?.records || data[4] || []];
  } catch (e) {
    if(seq===loadSequence)error.value = e.response?.data?.message || e.message;
  } finally {
    if(seq===loadSequence)loading.value = false;
  }
}
function setRange(days){const end=new Date();const start=new Date();start.setDate(end.getDate()-days+1);dateTo.value=end.toISOString().slice(0,10);dateFrom.value=start.toISOString().slice(0,10);load()}
function clearRange(){dateFrom.value="";dateTo.value="";load()}
async function saveSettings(){settingSaving.value=true;try{settings.value=await props.request('/admin/settings',settings.value,'put');error.value='平台配置已保存'}catch(e){error.value=e.message}finally{settingSaving.value=false}}
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
watch([userPage,itemPage,orderPage],load);
watch(pageSize,()=>{const alreadyFirst=userPage.value===1&&itemPage.value===1&&orderPage.value===1;userPage.value=1;itemPage.value=1;orderPage.value=1;if(alreadyFirst)load();});
onMounted(load);
</script>
<template>
  <section class="admin-view" :class="{overview: overview}">
    <h2>管理员后台</h2>
    <div v-if="overview" class="date-filter"><strong>数据时间范围</strong><button @click="setRange(1)">本日</button><button @click="setRange(1)">近 24 小时</button><button @click="setRange(7)">近 7 天</button><button @click="setRange(30)">近 30 天</button><button @click="setRange(90)">本季度</button><label>开始日期<input v-model="dateFrom" type="date"></label><label>结束日期<input v-model="dateTo" type="date"></label><button class="active" @click="load">查询</button><button @click="clearRange">清除</button><span v-if="dateFrom||dateTo">当前：{{dateFrom||'不限'}} 至 {{dateTo||'不限'}}</span></div><div class="admin-stats">
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
        v-if="tab==='items' || tab==='users'"
        v-model="query"
        :placeholder="tab === 'users' ? '筛选本页账号或昵称' : '筛选本页标题或地点'"
        aria-label="搜索后台数据"
      /><button v-if="tab==='items' || tab==='users'" :disabled="loading || acting !== null" @click="load">
        刷新
      </button>
    </nav>
    <p v-if="error" role="alert">{{ error }}</p>
    <p v-if="loading" role="status">正在加载…</p>
    <button class="manage-tab" :class="{active:tab==='ads'}" @click="tab='ads'">广告管理</button><button class="manage-tab" :class="{active:tab==='promotions'}" @click="tab='promotions'">推广管理</button><button class="manage-tab" :class="{active:tab==='delivery'}" @click="tab='delivery'">配送管理</button><button class="manage-tab" :class="{active:tab==='couriers'}" @click="tab='couriers'">配送员审核</button><button class="manage-tab" :class="{active:tab==='settings'}" @click="tab='settings'">平台配置</button><AdManagement v-if="tab==='ads'" :request="request"/><div v-if="tab==='promotions'"><PromotionOrders :request="request" admin/></div><div v-if="tab==='delivery'"><DeliveryOrders :request="request" admin/></div><CourierReview v-if="tab==='couriers'" :request="request"/><section v-if="tab==='settings'" class="settings-panel"><h3>平台商业化配置</h3><p>修改后仅影响新推广订单和新配送订单，历史订单金额不变。</p><div class="setting-grid"><label>配送费<input v-model="settings['delivery.fee']" type="number" step="0.01"></label><label>配送抽成<input v-model="settings['delivery.platformRate']" type="number" step="0.01"></label><label>24小时置顶价格<input v-model="settings['promotion.TOP_24H.amount']" type="number" step="0.01"></label><label>首页推荐价格<input v-model="settings['promotion.FEATURED_3D.amount']" type="number" step="0.01"></label><label>分类置顶价格<input v-model="settings['promotion.CATEGORY_7D.amount']" type="number" step="0.01"></label></div><button class="active" :disabled="settingSaving" @click="saveSettings">保存平台配置</button></section><section v-if="overview" class="dashboard-panels"><div class="dashboard-panel"><h3>系统数据分布</h3><div class="donut-wrap"><div class="donut" :style="{background:`conic-gradient(#28766e 0 ${((stats.approved||0)/(stats.items||1))*100}%,#d9e9e3 0 100%)`}"><b>{{stats.approved||0}}</b><small>公开信息</small></div><div class="legend"><p><i class="dot green"></i>公开信息 <b>{{stats.approved||0}}</b></p><p><i class="dot blue"></i>待审核 <b>{{stats.pending||0}}</b></p><p><i class="dot gray"></i>全部信息 <b>{{stats.items||0}}</b></p></div></div></div><div class="dashboard-panel"><h3>平台收入来源</h3><div class="source-row"><span>交易佣金</span><i :style="{width:(Number(trade.commission||0)/(Number(trade.commission||0)+Number(trade.gross||0.01))*100)+'%'}"></i><b>¥{{trade.commission||'0.00'}}</b></div><div class="source-row"><span>商品推广</span><i :style="{width:(Number(trade.gross||0)/(Number(trade.gross||0)+Number(trade.commission||0.01))*100)+'%'}"></i><b>¥{{trade.gross||'0.00'}}</b></div><div class="source-row"><span>配送抽成</span><i style="width:35%"></i><b>待统计</b></div><p class="muted">收入数据按已支付订单统计</p></div><div class="dashboard-panel wide"><h3>系统运行数据</h3><table class="system-table"><tbody><tr><td>注册用户</td><td>{{stats.users||0}}</td><td>访客用户</td><td>{{trade.visitors||0}}</td><td>成交订单</td><td>{{trade.orders||0}}</td></tr><tr><td>交易总额</td><td>¥{{trade.gross||'0.00'}}</td><td>平台佣金</td><td>¥{{trade.commission||'0.00'}}</td><td>平均订单</td><td>¥{{trade.orders?(Number(trade.gross||0)/trade.orders).toFixed(2):'0.00'}}</td></tr><tr><td>公开信息</td><td>{{stats.approved||0}}</td><td>待审核</td><td>{{stats.pending||0}}</td><td>更新时间</td><td>{{new Date().toLocaleString('zh-CN')}}</td></tr></tbody></table></div></section><div v-if="overview" class="trade-panel"><h3>二手交易数据</h3><div class="trade-cards"><div><span>成交订单</span><b>{{trade.orders ?? "—"}}</b></div><div><span>交易金额</span><b>¥{{trade.gross ?? "0.00"}}</b></div><div><span>平台佣金</span><b>¥{{trade.commission ?? "0.00"}}</b></div><div><span>访客用户</span><b>{{trade.visitors ?? "—"}}</b></div></div><p>当前佣金比例：{{((trade.commissionRate ?? 0.03)*100).toFixed(1)}}% · 统计范围：已支付订单</p></div><div v-if="overview" class="trend-panel"><h3>近7日交易趋势</h3><div class="chart-wrap"><svg viewBox="0 0 700 220" role="img" aria-label="近7日交易金额趋势"><polyline :points="chartPoints" fill="none" stroke="#2b8a78" stroke-width="4" stroke-linecap="round" stroke-linejoin="round"/><circle v-for="(d,i) in (trade.trend || [])" :key="d.date" :cx="i*100+25" :cy="chartY(d)" r="6" fill="#fff" stroke="#2b8a78" stroke-width="3"/><text v-for="(d,i) in (trade.trend || [])" :key="`label-${d.date}`" :x="i*100+25" y="215" text-anchor="middle">{{d.date.slice(5)}}</text></svg></div><div class="trend-row" v-for="day in (trade.trend || [])" :key="day.date"><span>{{day.date}}</span><i :style="{width: Math.min(100, day.orders * 18 + 4) + '%' }"></i><b>{{day.orders}} 单 · ¥{{day.gross}}</b></div><h3 class="orders-title">最近交易订单</h3><table><thead><tr><th>订单号</th><th>金额</th><th>佣金</th><th>状态</th><th>时间</th></tr></thead><tbody><tr v-for="o in tradeOrders" :key="o.id"><td>{{o.orderNo}}</td><td>¥{{o.amount}}</td><td>¥{{o.commission}}</td><td>{{o.status}}</td><td>{{o.createdAt}}</td></tr></tbody></table><p v-if="!tradeOrders.length">暂无交易订单。</p><Pager v-model:page="orderPage" v-model:size="pageSize" :total="orderTotal" :busy="loading"/></div><div v-if="tab==='items' || tab==='users'" class="admin-table">
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
  <Pager v-if="tab==='users'" v-model:page="userPage" v-model:size="pageSize" :total="userTotal" :busy="loading"/><Pager v-else-if="tab==='items'" v-model:page="itemPage" v-model:size="pageSize" :total="itemTotal" :busy="loading"/></section>
</template>
<style scoped>
.admin-view:not(.overview) .trade-panel,.admin-view:not(.overview) .trend-panel{display:none}.date-filter{display:flex;align-items:center;gap:10px;flex-wrap:wrap;margin:20px 0;padding:16px;background:#fff;border:1px solid #dcebe5;border-radius:12px}.date-filter input{padding:8px;border:1px solid #d7e5df;border-radius:6px}.date-filter button{padding:8px 12px;border-radius:6px;background:#e4eef0;color:#24645f}.date-filter label{display:flex;align-items:center;gap:5px;color:#627780}.date-filter span{font-size:12px;color:#718a83}.overview .admin-stats{border-bottom:0}.overview .admin-table,.overview .admin-toolbar,.overview .settings-panel,.overview .promotion-orders,.overview .delivery,.overview .commission-panel,.overview .pager{display:none}.admin-stats {
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
.manage-tab{padding:10px 16px;border:1px solid #d5e5df;border-radius:8px;background:#fff;color:#24645f;margin:0 6px 16px 0}.manage-tab.active{background:#28766e;color:#fff}.admin-toolbar {
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
.settings-panel{margin:24px 0;padding:24px;background:#fff;border:1px solid #e1ebe7;border-radius:16px}.setting-grid{display:flex;gap:14px;flex-wrap:wrap}.setting-grid label{display:flex;flex-direction:column;gap:6px}.setting-grid input{width:130px;padding:8px}.settings-panel button{margin-top:14px}.dashboard-panels{display:grid;grid-template-columns:1fr 1fr;gap:20px;margin:24px 0}.dashboard-panel{padding:24px;background:#fff;border:1px solid #dcebe5;border-radius:16px}.dashboard-panel.wide{grid-column:1/-1}.dashboard-panel h3{margin:0 0 18px;color:#244b4a}.donut-wrap{display:flex;align-items:center;gap:24px}.donut{width:150px;height:150px;border-radius:50%;display:grid;place-content:center;text-align:center;position:relative}.donut:after{content:"";position:absolute;inset:22px;background:#fff;border-radius:50%}.donut b,.donut small{z-index:1}.donut b{font-size:26px;color:#24645f}.donut small{font-size:11px;color:#78918a}.legend p{margin:10px 0;color:#627780}.dot{display:inline-block;width:10px;height:10px;border-radius:50%;margin-right:8px}.green{background:#28766e}.blue{background:#4a85c5}.gray{background:#d9e9e3}.source-row{display:grid;grid-template-columns:90px 1fr 80px;gap:10px;align-items:center;margin:18px 0;color:#627780}.source-row i{height:10px;background:#28766e;border-radius:10px}.source-row b{text-align:right;color:#24645f}.muted{font-size:12px;color:#8a9b96}.system-table{width:100%;border-collapse:collapse}.system-table td{padding:14px 10px;border-bottom:1px solid #edf2ef;color:#627780}.system-table td:nth-child(even){font-weight:700;color:#24645f;text-align:right}.trade-panel,.trend-panel{margin:24px 0;padding:24px;background:#fff;border:1px solid #e1ebe7;border-radius:16px;box-shadow:0 10px 30px #1d5b4b0b}.trade-panel h3,.trend-panel h3{font-size:20px;color:#244b4a;margin:0 0 18px}.trade-cards{display:grid;grid-template-columns:repeat(4,1fr);gap:14px}.trade-cards div{padding:18px;border-radius:12px;background:linear-gradient(135deg,#edf8f3,#fff);border:1px solid #dceee6}.trade-cards span{display:block;color:#78918a;font-size:12px}.trade-cards b{display:block;color:#1f6d61;font-size:28px;margin-top:8px}.chart-wrap{padding:16px 8px;background:#f7fbf9;border-radius:12px;border:1px solid #e1eee8}.chart-wrap svg{width:100%;height:280px}.chart-wrap text{font-size:12px;fill:#718c84}.trend-row{display:none}.trend-panel table{width:100%;margin-top:20px;border-collapse:collapse}.trend-panel th,.trend-panel td{padding:12px;border-bottom:1px solid #edf2ef;text-align:left}.orders-title{margin-top:24px!important;padding-top:18px;border-top:1px solid #edf2ef}@media(max-width:800px){.dashboard-panels{grid-template-columns:1fr}.dashboard-panel.wide{grid-column:auto}.trade-cards{grid-template-columns:repeat(2,1fr)}}
</style>





