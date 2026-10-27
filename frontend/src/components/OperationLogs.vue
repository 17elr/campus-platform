<script setup>
import { onMounted, ref, watch } from "vue";
import Pager from "./Pager.vue";

const props = defineProps({ request: { type: Function, required: true } });
const actions = [
  { key: "", label: "全部操作" },
  { key: "DELIVERY_CLAIMED", label: "配送抢单" },
  { key: "DELIVERY_STATUS", label: "配送状态" },
  { key: "DELIVERY_DISPUTED", label: "配送异常" },
  { key: "DELIVERY_RESOLVED", label: "异常处理" },
  { key: "COURIER_REVIEW", label: "配送员审核" },
];
const labels = Object.fromEntries(
  actions.map((item) => [item.key, item.label]),
);
const from = ref(""),
  to = ref(""),
  action = ref(""),
  keyword = ref("");
const page = ref(1),
  size = ref(10),
  total = ref(0),
  pending = ref(0),
  rows = ref([]),
  loading = ref(false),
  error = ref("");
let sequence = 0;
async function load() {
  const current = ++sequence;
  loading.value = true;
  error.value = "";
  try {
    const params = new URLSearchParams({ page: page.value, size: size.value });
    if (from.value) params.set("from", from.value);
    if (to.value) params.set("to", to.value);
    if (action.value) params.set("action", action.value);
    if (keyword.value.trim()) params.set("keyword", keyword.value.trim());
    const result = await props.request(
      `/admin/operation-logs?${params}`,
      undefined,
      "get",
    );
    if (current !== sequence) return;
    rows.value = result.records || [];
    total.value = result.total || 0;
    pending.value = result.pending || 0;
  } catch (e) {
    if (current === sequence)
      error.value = e.response?.data?.message || e.message;
  } finally {
    if (current === sequence) loading.value = false;
  }
}
function reset() {
  from.value = "";
  to.value = "";
  action.value = "";
  keyword.value = "";
  page.value = 1;
  load();
}
watch([page, size], load);
onMounted(load);
</script>

<template>
  <section class="operation-logs">
    <div class="log-head">
      <div>
        <h3>操作日志</h3>
        <p>记录配送抢单、履约状态、异常处理和配送员审核。</p>
      </div>
      <strong>待同步 {{ pending }} 条</strong>
    </div>
    <div class="log-filters">
      <label>开始日期<input v-model="from" type="date" /></label
      ><label>结束日期<input v-model="to" type="date" /></label
      ><label
        >操作类型<select v-model="action">
          <option v-for="item in actions" :key="item.key" :value="item.key">
            {{ item.label }}
          </option>
        </select></label
      ><label class="keyword"
        >关键词<input
          v-model.trim="keyword"
          maxlength="100"
          placeholder="账号、订单号或说明" /></label
      ><button
        @click="
          page = 1;
          load();
        "
      >
        查询</button
      ><button @click="reset">清除</button>
    </div>
    <p v-if="error" class="log-error" role="alert">{{ error }}</p>
    <div v-if="loading" class="log-empty">正在加载日志…</div>
    <div v-else-if="!rows.length" class="log-empty">
      当前筛选条件下没有操作日志。
    </div>
    <table v-else class="log-table">
      <thead>
        <tr>
          <th>时间</th>
          <th>操作</th>
          <th>操作人</th>
          <th>目标</th>
          <th>说明</th>
          <th>事件编号</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in rows" :key="row.id">
          <td>{{ row.createdAt }}</td>
          <td>
            <span class="log-tag">{{ labels[row.action] || row.action }}</span>
          </td>
          <td>{{ row.username || `用户 ${row.userId}` }}</td>
          <td>#{{ row.targetId }}</td>
          <td>{{ row.detail || "—" }}</td>
          <td class="event-id">{{ row.eventId || "旧记录" }}</td>
        </tr>
      </tbody>
    </table>
    <Pager
      v-model:page="page"
      v-model:size="size"
      :total="total"
      :busy="loading"
    />
  </section>
</template>

<style scoped>
.operation-logs {
  margin-top: 18px;
  padding: 22px;
  background: #fff;
  border: 1px solid #dce8e5;
  border-radius: 10px;
}
.log-head {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
}
.log-head h3 {
  margin: 0 0 6px;
  color: #173e3b;
}
.log-head p {
  margin: 0;
  color: #718785;
}
.log-head strong {
  color: #28766e;
}
.log-filters {
  display: flex;
  align-items: end;
  gap: 10px;
  flex-wrap: wrap;
  padding: 18px 0;
  border-bottom: 1px solid #e1ece8;
}
.log-filters label {
  font-size: 12px;
  color: #54706c;
}
.log-filters input,
.log-filters select {
  display: block;
  margin-top: 5px;
  padding: 9px;
  border: 1px solid #cbdcd6;
  border-radius: 6px;
  background: #fff;
  color: #183c3b;
  font: inherit;
}
.log-filters .keyword {
  min-width: 190px;
}
.log-filters button {
  padding: 9px 14px;
  border: 1px solid #cbdcd6;
  border-radius: 6px;
  background: #e7f1ef;
  color: #24645f;
  cursor: pointer;
}
.log-table {
  width: 100%;
  margin-top: 16px;
  border-collapse: collapse;
  font-size: 13px;
}
.log-table th,
.log-table td {
  padding: 12px 8px;
  border-bottom: 1px solid #e5eeeb;
  text-align: left;
  vertical-align: top;
}
.log-table th {
  color: #53716c;
  font-weight: 700;
}
.log-tag {
  display: inline-block;
  padding: 4px 7px;
  border-radius: 4px;
  background: #e8f3ef;
  color: #28766e;
}
.event-id {
  max-width: 150px;
  color: #8aa09d;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.log-empty {
  text-align: center;
  padding: 42px;
  color: #7b918d;
}
.log-error {
  padding: 10px;
  background: #fff1ef;
  color: #a62b25;
  border-radius: 6px;
}
@media (max-width: 760px) {
  .log-table {
    display: block;
    overflow-x: auto;
    white-space: nowrap;
  }
  .log-head {
    flex-direction: column;
  }
}
</style>
