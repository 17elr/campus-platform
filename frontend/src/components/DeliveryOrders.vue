<script setup>
import { onMounted, ref, watch } from 'vue'
import Pager from './Pager.vue'

const props = defineProps({ request: Function, publicView: Boolean, admin: Boolean, userId: Number })
const types = [{ key: '', label: '全部' }, { key: 'EXPRESS', label: '快递代拿' }, { key: 'ERRAND', label: '校园跑腿' }, { key: 'FOOD', label: '小吃街外卖' }]
const typeName = value => types.find(item => item.key === value)?.label || value
const statusName = value => ({ WAITING: '等待接单', ACCEPTED: '已接单', PICKED_UP: '已取件', DELIVERING: '配送中', COMPLETED: '已完成', CANCELLED: '已取消', DISPUTED: '异常处理中' }[value] || value)
const selectedType = ref('')
const view = ref(props.admin ? 'orders' : 'orders')
const page = ref(1)
const size = ref(10)
const total = ref(0)
const records = ref([])
const loading = ref(false)
const busy = ref(false)
const error = ref('')
const notice = ref('')
const showForm = ref(false)
const form = ref({ deliveryType: 'EXPRESS', pickupLocation: '', deliveryLocation: '', requesterNote: '' })
const courier = ref({ status: 'NONE' })
const application = ref({ campusId: '', note: '' })
const showApplication = ref(false)
const normalize = row => ({
  id: row.id, deliveryType: row.delivery_type ?? row.deliveryType, status: row.status,
  pickupLocation: row.pickup_location ?? row.pickupLocation,
  deliveryLocation: row.delivery_location ?? row.deliveryLocation,
  requesterNote: row.requester_note ?? row.requesterNote,
  senderId: row.sender_id ?? row.senderId, receiverId: row.receiver_id ?? row.receiverId,
  courierId: row.courier_id ?? row.courierId, fee: row.fee,
  deliveryNo: row.delivery_no ?? row.deliveryNo,
  disputeReason: row.dispute_reason ?? row.disputeReason,
  resolutionNote: row.resolution_note ?? row.resolutionNote,
  createdAt: row.created_at ?? row.createdAt,
})
const query = () => {
  if (props.admin) return `/delivery/admin/orders?page=${page.value}&size=${size.value}${selectedType.value ? `&type=${selectedType.value}` : ''}`
  if (props.publicView) return `/delivery/public?page=${page.value}&size=${size.value}${selectedType.value ? `&type=${selectedType.value}` : ''}`
  if (view.value === 'available') return `/delivery/available?page=${page.value}&size=${size.value}${selectedType.value ? `&type=${selectedType.value}` : ''}`
  return `/delivery?page=${page.value}&size=${size.value}`
}
let sequence = 0
async function load() {
  const current = ++sequence
  loading.value = true
  error.value = ''
  try {
    const result = await props.request(query(), undefined, 'get')
    if (current !== sequence) return
    records.value = (result.records || []).map(normalize)
    total.value = Number(result.total || 0)
  } catch (e) {
    if (current === sequence) error.value = e.response?.data?.message || e.message
  } finally {
    if (current === sequence) loading.value = false
  }
}
function switchView(next) {
  if (next === 'available' && courier.value.status !== 'APPROVED') { showApplication.value = true; return }
  view.value = next; page.value = 1; load()
}
function switchType(next) { selectedType.value = next; page.value = 1; load() }
async function loadCourier() {
  if (props.admin || props.publicView) return
  try { courier.value = await props.request('/delivery/courier/me', undefined, 'get') }
  catch (e) { error.value = e.response?.data?.message || e.message }
}
async function applyCourier() {
  busy.value = true; error.value = ''; notice.value = ''
  try {
    courier.value = await props.request('/delivery/courier/applications', application.value, 'post')
    notice.value = '申请已提交，等待管理员审核。'
    showApplication.value = false
  } catch (e) { error.value = e.response?.data?.message || e.message }
  finally { busy.value = false }
}
async function create() {
  busy.value = true; error.value = ''; notice.value = ''
  try {
    await props.request('/delivery', { ...form.value, tradeOrderId: null }, 'post')
    notice.value = '配送需求已发布，等待接单。'
    form.value = { deliveryType: 'EXPRESS', pickupLocation: '', deliveryLocation: '', requesterNote: '' }
    showForm.value = false
    view.value = 'orders'; page.value = 1
    await load()
  } catch (e) { error.value = e.response?.data?.message || e.message }
  finally { busy.value = false }
}
async function change(order, next) {
  busy.value = true; error.value = ''; notice.value = ''
  try {
    if (next === 'ACCEPTED') await props.request(`/delivery/${order.id}/claim`, undefined, 'post')
    else await props.request(`/delivery/${order.id}/status`, { status: next }, 'put')
    notice.value = next === 'ACCEPTED' ? '接单成功。' : `订单已更新为${statusName(next)}。`
    await load()
  } catch (e) { error.value = e.response?.data?.message || e.message }
  finally { busy.value = false }
}
async function dispute(order) {
  const reason = window.prompt('请描述配送异常，管理员将据此处理：')
  if (reason === null) return
  busy.value = true; error.value = ''; notice.value = ''
  try {
    await props.request(`/delivery/${order.id}/dispute`, { reason: reason.trim() }, 'post')
    notice.value = '异常已提交，等待管理员处理。'
    await load()
  } catch (e) { error.value = e.response?.data?.message || e.message }
  finally { busy.value = false }
}
async function resolve(order, action) {
  const note = window.prompt(action === 'CANCEL' ? '填写取消配送的处理说明：' : '填写恢复配送的处理说明：')
  if (note === null) return
  busy.value = true; error.value = ''; notice.value = ''
  try {
    await props.request(`/delivery/admin/orders/${order.id}/dispute`, { action, note: note.trim() }, 'put')
    notice.value = '异常订单已处理。'
    await load()
  } catch (e) { error.value = e.response?.data?.message || e.message }
  finally { busy.value = false }
}
function actions(order) {
  if (props.publicView || props.admin) return []
  if (view.value === 'available') return [{ label: '接单', next: 'ACCEPTED' }]
  if (Number(order.receiverId) === Number(props.userId)) {
    if (order.status === 'WAITING') return [{ label: '取消需求', next: 'CANCELLED' }]
    if (order.status === 'DELIVERING') return [{ label: '确认送达', next: 'COMPLETED' }]
  }
  if (Number(order.courierId) === Number(props.userId)) {
    if (order.status === 'ACCEPTED') return [{ label: '确认取件', next: 'PICKED_UP' }]
    if (order.status === 'PICKED_UP') return [{ label: '开始配送', next: 'DELIVERING' }]
  }
  return []
}
watch([page, size], load)
onMounted(() => { load(); loadCourier() })
</script>

<template>
  <section class="delivery-page">
    <header class="delivery-head">
      <div><span class="eyebrow">CAMPUS DELIVERY</span><h2>{{ admin ? '配送订单管理' : '校园配送' }}</h2><p>快递代拿、校园跑腿、小吃街外卖</p></div>
      <button v-if="!publicView && !admin" class="primary" @click="showForm = !showForm">{{ showForm ? '收起表单' : '发布配送需求' }}</button>
    </header>
    <form v-if="showForm && !publicView && !admin" class="delivery-form" @submit.prevent="create">
      <h3>发布配送需求</h3>
      <div class="form-grid"><label>服务类型<select v-model="form.deliveryType"><option v-for="type in types.slice(1)" :key="type.key" :value="type.key">{{ type.label }}</option></select></label><label>取货地点<input v-model.trim="form.pickupLocation" maxlength="200" required placeholder="例如：东门快递站"></label><label>送达地点<input v-model.trim="form.deliveryLocation" maxlength="200" required placeholder="例如：3号宿舍楼下"></label></div>
      <label>需求说明<textarea v-model.trim="form.requesterNote" maxlength="500" rows="2" placeholder="取件码、商品信息或其他说明"></textarea></label>
      <div class="form-footer"><span>请勿在公开信息中填写取件码或联系方式，订单详情仅对参与者可见。</span><button class="primary" :disabled="busy">{{ busy ? '提交中…' : '确认发布' }}</button></div>
    </form>
    <section v-if="!publicView && !admin && showApplication" class="delivery-form courier-form">
      <h3>配送员申请</h3>
      <p v-if="courier.status === 'PENDING'">申请已提交，等待管理员审核。</p>
      <p v-else-if="courier.status === 'REJECTED'">上次申请未通过：{{ courier.reviewNote || '请核对资料后重新申请。' }}</p>
      <p v-if="courier.status !== 'PENDING' && courier.status !== 'APPROVED'">填写校园身份编号，管理员核对后即可接单。</p>
      <form v-if="courier.status !== 'PENDING' && courier.status !== 'APPROVED'" class="form-grid" @submit.prevent="applyCourier"><label>校园身份编号<input v-model.trim="application.campusId" required minlength="4" maxlength="40" pattern="[A-Za-z0-9_-]+" placeholder="学号或校内工号"></label><label>申请说明<input v-model.trim="application.note" maxlength="500" placeholder="配送时段或校区"></label><button class="primary" :disabled="busy">提交申请</button></form>
    </section>
    <div class="delivery-toolbar">
      <div v-if="!publicView && !admin" class="view-tabs"><button :class="{ active: view === 'orders' }" @click="switchView('orders')">我的订单</button><button :class="{ active: view === 'available' }" @click="switchView('available')">可接订单{{ courier.status === 'APPROVED' ? '' : ' · 待认证' }}</button><button v-if="courier.status !== 'APPROVED'" @click="showApplication = !showApplication">{{ courier.status === 'PENDING' ? '审核进度' : '申请配送员' }}</button></div>
      <div v-else class="section-title"><h3>{{ admin ? '全部配送订单' : '公开配送动态' }}</h3><span>{{ total }} 条</span></div>
      <div v-if="publicView || admin || view === 'available'" class="type-tabs"><button v-for="type in types" :key="type.key" :class="{ active: selectedType === type.key }" @click="switchType(type.key)">{{ type.label }}</button></div>
      <button class="refresh" :disabled="loading" @click="load">刷新</button>
    </div>
    <p v-if="error" class="message error" role="alert">{{ error }}</p><p v-if="notice" class="message success" role="status">{{ notice }}</p>
    <div v-if="loading" class="empty">正在加载配送订单…</div>
    <div v-else-if="!records.length" class="empty"><span class="empty-icon">⌁</span><h3>{{ view === 'available' ? '暂时没有可接订单' : '暂无配送订单' }}</h3><p>{{ publicView ? '有新需求时，配送动态会显示在这里。' : view === 'available' ? '稍后刷新查看新的配送需求。' : '选择上方服务类型，发布你的第一笔配送需求。' }}</p><button v-if="!publicView && !admin && view === 'orders'" class="primary" @click="showForm = true">发布配送需求</button></div>
    <div v-else class="order-list"><article v-for="order in records" :key="order.id" class="order-row"><div class="order-main"><div class="order-top"><strong>{{ typeName(order.deliveryType) }}</strong><span class="status" :data-status="order.status">{{ statusName(order.status) }}</span><small>#{{ order.id }}</small></div><p v-if="!publicView && order.pickupLocation && order.deliveryLocation" class="route"><span>{{ order.pickupLocation }}</span><b>→</b><span>{{ order.deliveryLocation }}</span></p><p v-if="order.requesterNote && !publicView && view !== 'available'" class="note">{{ order.requesterNote }}</p><p v-if="order.disputeReason && !publicView" class="note dispute">异常：{{ order.disputeReason }}<template v-if="order.resolutionNote"> · 处理：{{ order.resolutionNote }}</template></p><small class="meta">{{ order.createdAt || '' }}<template v-if="order.deliveryNo"> · {{ order.deliveryNo }}</template></small></div><div class="order-end"><strong v-if="order.fee != null">¥{{ Number(order.fee).toFixed(2) }}</strong><button v-for="action in actions(order)" :key="action.next" :disabled="busy" @click="change(order, action.next)">{{ action.label }}</button><button v-if="admin && order.status === 'DISPUTED'" :disabled="busy" @click="resolve(order, 'RESUME')">恢复配送</button><button v-if="admin && order.status === 'DISPUTED'" :disabled="busy" @click="resolve(order, 'CANCEL')">取消配送</button><button v-if="!admin && !publicView && view === 'orders' && ['ACCEPTED', 'PICKED_UP', 'DELIVERING'].includes(order.status)" :disabled="busy" @click="dispute(order)">报告异常</button></div></article></div>
    <Pager v-model:page="page" v-model:size="size" :total="total" :busy="loading || busy" />
  </section>
</template>

<style scoped>
.delivery-page{color:#183c3b}.delivery-head{display:flex;align-items:end;justify-content:space-between;gap:20px;padding:32px 36px;background:#174c4a;color:#fff;border-radius:8px}.delivery-head>div{display:flex;flex-direction:column;align-items:flex-start}.eyebrow{font-size:11px;letter-spacing:2px;color:#a9ddd0}.delivery-head h2{font-size:30px;margin:8px 0}.delivery-head p{margin:0;color:#cee0d9}.delivery-head .primary{background:#dcf2e7;color:#174c4a}.primary{border:0;border-radius:6px;background:#246d64;color:#fff;padding:11px 18px;font-weight:700;cursor:pointer;white-space:nowrap}.delivery-form{margin:20px 0;padding:24px;border:1px solid #d4e3de;border-radius:8px;background:#fff}.delivery-form h3{margin:0 0 18px}.form-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:14px}.delivery-form label{display:block;font-size:13px;font-weight:700;color:#365b58}.delivery-form input,.delivery-form select,.delivery-form textarea{display:block;width:100%;box-sizing:border-box;margin:8px 0 16px;padding:11px;border:1px solid #cbdcd6;border-radius:6px;font:inherit;color:#183c3b;background:#fff}.form-footer{display:flex;justify-content:space-between;align-items:center;gap:20px}.form-footer span{font-size:12px;color:#748b89}.delivery-toolbar{display:flex;align-items:center;gap:12px;flex-wrap:wrap;padding:24px 0 14px;border-bottom:1px solid #d9e5e1}.view-tabs,.type-tabs{display:flex;gap:6px;flex-wrap:wrap}.delivery-toolbar button,.order-end button{border:1px solid #d3e3dd;border-radius:6px;background:#fff;color:#275651;padding:9px 13px;cursor:pointer}.delivery-toolbar button.active,.order-end button{background:#246d64;color:#fff;border-color:#246d64}.delivery-toolbar .refresh{margin-left:auto}.section-title{display:flex;align-items:baseline;gap:12px;margin-right:auto}.section-title h3{margin:0}.section-title span{color:#78908d}.message{padding:11px 14px;border-radius:6px}.error{background:#fff1ef;color:#a62b25}.success{background:#e7f4ed;color:#246d64}.empty{text-align:center;padding:58px 16px;color:#718a87}.empty h3{color:#214e4b;margin:6px 0}.empty-icon{font-size:36px;color:#6aa49b}.order-list{background:#fff}.order-row{display:flex;justify-content:space-between;gap:20px;padding:20px 4px;border-bottom:1px solid #e2ece8}.order-main{min-width:0}.order-top{display:flex;align-items:center;gap:12px;flex-wrap:wrap}.order-top strong{font-size:17px}.order-top small,.meta{color:#849a97}.status{border-radius:4px;padding:4px 8px;background:#ecf4f1;color:#246d64;font-size:12px}.status[data-status="CANCELLED"]{background:#f5eeee;color:#9b5d56}.status[data-status="COMPLETED"]{background:#e6f4ea;color:#287649}.route{display:flex;align-items:center;gap:12px;margin:14px 0 8px;flex-wrap:wrap}.route b{color:#489b8b}.note{color:#607b77;margin:8px 0}.order-end{display:flex;align-items:center;gap:12px;white-space:nowrap}.order-end strong{font-size:18px}.order-end button:disabled,.delivery-toolbar button:disabled,.primary:disabled{opacity:.5;cursor:default}@media(max-width:700px){.delivery-head{padding:24px;align-items:start;flex-direction:column}.delivery-head h2{font-size:25px}.form-grid{grid-template-columns:1fr}.form-footer,.order-row{align-items:start;flex-direction:column}.order-end{width:100%;justify-content:space-between}.delivery-toolbar .refresh{margin-left:0}}
</style>
