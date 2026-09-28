<script setup>
import { onMounted, ref, watch } from 'vue'
import Pager from './Pager.vue'

const props = defineProps({ request: { type: Function, required: true } })
const applications = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const loading = ref(false)
const busy = ref(false)
const error = ref('')
const notice = ref('')
let sequence = 0

async function load() {
  const current = ++sequence
  loading.value = true
  error.value = ''
  try {
    const data = await props.request(`/delivery/admin/couriers?page=${page.value}&size=${size.value}`, undefined, 'get')
    if (current !== sequence) return
    applications.value = data.records || []
    total.value = Number(data.total || 0)
  } catch (e) { if (current === sequence) error.value = e.response?.data?.message || e.message }
  finally { if (current === sequence) loading.value = false }
}

async function review(application, status) {
  const note = window.prompt(status === 'APPROVED' ? '审核说明（可留空）：' : '请填写驳回原因：')
  if (note === null) return
  busy.value = true; error.value = ''; notice.value = ''
  try {
    await props.request(`/delivery/admin/couriers/${application.id}`, { status, note: note.trim() }, 'put')
    notice.value = `${application.username} 的申请已${status === 'APPROVED' ? '通过' : '驳回'}。`
    await load()
  } catch (e) { error.value = e.response?.data?.message || e.message }
  finally { busy.value = false }
}

watch([page, size], load)
onMounted(load)
</script>

<template>
  <section class="courier-review">
    <div class="review-head"><div><h3>配送员审核</h3><p>核对校园身份后开通接单权限</p></div><button :disabled="loading" @click="load">刷新</button></div>
    <p v-if="error" class="error" role="alert">{{ error }}</p><p v-if="notice" class="notice" role="status">{{ notice }}</p>
    <p v-if="loading">正在加载申请…</p><p v-else-if="!applications.length">暂无配送员申请。</p>
    <div v-else class="review-list"><article v-for="item in applications" :key="item.id" class="review-row"><div><strong>{{ item.nickname || item.username }}</strong><small>@{{ item.username }} · 校园身份编号 {{ item.campusId }}</small><p v-if="item.note">{{ item.note }}</p><small>{{ item.createdAt }}</small></div><div class="review-actions"><span>{{ { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回' }[item.status] || item.status }}</span><button v-if="item.status === 'PENDING'" :disabled="busy" @click="review(item, 'APPROVED')">通过</button><button v-if="item.status === 'PENDING'" :disabled="busy" @click="review(item, 'REJECTED')">驳回</button></div></article></div>
    <Pager v-model:page="page" v-model:size="size" :total="total" :busy="loading || busy" />
  </section>
</template>

<style scoped>
.courier-review{margin:18px 0;padding:22px;background:#fff;border:1px solid #d9e7e2;border-radius:8px;color:#244947}.review-head,.review-row,.review-actions{display:flex;align-items:center;justify-content:space-between;gap:14px}.review-head h3{margin:0;font-size:18px}.review-head p,.review-row p,.review-row small{color:#738883}.review-head p{margin:5px 0 0}.review-row{padding:16px 0;border-bottom:1px solid #e1ebe7}.review-row>div:first-child{min-width:0}.review-row strong,.review-row small{display:block}.review-row small{font-size:12px;margin-top:5px}.review-actions{flex-wrap:wrap;justify-content:flex-end}.review-actions span{color:#47786e;white-space:nowrap}.courier-review button{padding:8px 12px;border:1px solid #cbded7;border-radius:6px;background:#fff;color:#245e55}.review-actions button:first-of-type{background:#286f64;color:#fff}.courier-review button:disabled{opacity:.5}.error{color:#a33d35}.notice{color:#267148}@media(max-width:650px){.review-row{align-items:flex-start;flex-direction:column}.review-actions{justify-content:flex-start}}
</style>
