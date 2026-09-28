<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import Pager from './Pager.vue'

const props = defineProps({ request: { type: Function, required: true } })
const placements = [
  { value: 'HOME_BANNER', label: '首页横幅' },
  { value: 'SIDEBAR', label: '侧边栏' },
  { value: 'CATEGORY_TOP', label: '分类顶部' },
  { value: 'DETAIL_RECOMMEND', label: '详情推荐' },
]
const statusLabels = { PENDING: '待投放', ACTIVE: '投放中', PAUSED: '已暂停', REJECTED: '已驳回' }
const emptyForm = () => ({ merchantName: '', title: '', imageUrl: '', targetUrl: '', placement: 'HOME_BANNER', amount: '0.00', startAt: '', endAt: '' })
const form = ref(emptyForm())
const editingId = ref(null)
const editorOpen = ref(false)
const editor = ref(null)
const ads = ref([])
const stats = ref({})
const page = ref(1)
const size = ref(10)
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const uploading = ref(false)
const actingId = ref(null)
const error = ref('')
const notice = ref('')
let loadSequence = 0

const apiBase = new URL(import.meta.env.VITE_API_BASE_URL || 'http://127.0.0.1:8080/api', window.location.origin)
const imageSrc = path => path ? new URL(path, apiBase).href : ''
const placementLabel = value => placements.find(item => item.value === value)?.label || value
const dateInput = value => value ? String(value).replace(' ', 'T').slice(0, 16) : ''
const dateLabel = value => value ? String(value).replace('T', ' ').slice(0, 16) : '不限'
const money = value => Number(value || 0).toFixed(2)
const editing = computed(() => editingId.value !== null)

async function load() {
  const sequence = ++loadSequence
  loading.value = true
  error.value = ''
  try {
    const [list, totals] = await Promise.all([
      props.request(`/advertisements/admin?page=${page.value}&size=${size.value}`, undefined, 'get'),
      props.request('/advertisements/admin/stats', undefined, 'get'),
    ])
    if (sequence !== loadSequence) return
    ads.value = list.records || []
    total.value = Number(list.total || 0)
    stats.value = totals || {}
    if (!ads.value.length && page.value > 1) page.value -= 1
  } catch (e) {
    if (sequence === loadSequence) error.value = e.response?.data?.message || e.message
  } finally {
    if (sequence === loadSequence) loading.value = false
  }
}

async function openEditor(ad) {
  error.value = ''
  notice.value = ''
  editingId.value = ad?.id ?? null
  form.value = ad ? {
    merchantName: ad.merchant_name ?? ad.merchantName ?? '',
    title: ad.title ?? '',
    imageUrl: ad.image_url ?? ad.imageUrl ?? '',
    targetUrl: ad.target_url ?? ad.targetUrl ?? '',
    placement: ad.placement ?? 'HOME_BANNER',
    amount: money(ad.amount),
    startAt: dateInput(ad.start_at ?? ad.startAt),
    endAt: dateInput(ad.end_at ?? ad.endAt),
  } : emptyForm()
  editorOpen.value = true
  await nextTick()
  editor.value?.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
}

function closeEditor() {
  editorOpen.value = false
  editingId.value = null
  form.value = emptyForm()
  error.value = ''
}

async function uploadImage(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  error.value = ''
  if (!['image/jpeg', 'image/png'].includes(file.type) || file.size > 5 * 1024 * 1024) {
    error.value = '请选择 5MB 以内的 JPG 或 PNG 图片'
    return
  }
  uploading.value = true
  try {
    const data = new FormData()
    data.append('file', file)
    form.value.imageUrl = await props.request('/images', data)
  } catch (e) {
    error.value = e.response?.data?.message || e.message
  } finally {
    uploading.value = false
  }
}

async function save() {
  if (saving.value || uploading.value) return
  error.value = ''
  notice.value = ''
  if (form.value.startAt && form.value.endAt && form.value.endAt <= form.value.startAt) {
    error.value = '结束时间须晚于开始时间'
    return
  }
  saving.value = true
  try {
    const payload = { ...form.value, amount: money(form.value.amount) }
    await props.request(editing.value ? `/advertisements/${editingId.value}` : '/advertisements', payload, editing.value ? 'put' : 'post')
    const wasEditing = editing.value
    closeEditor()
    if (!wasEditing && page.value !== 1) page.value = 1
    else await load()
    notice.value = wasEditing ? '广告已更新' : '广告已创建，请在列表中选择投放状态'
  } catch (e) {
    error.value = e.response?.data?.message || e.message
  } finally {
    saving.value = false
  }
}

async function changeStatus(ad, status) {
  if (actingId.value !== null || status === ad.status) return
  actingId.value = ad.id
  error.value = ''
  notice.value = ''
  try {
    await props.request(`/advertisements/${ad.id}/status`, { status }, 'put')
    await load()
    notice.value = `“${ad.title}”已设为${statusLabels[status]}`
  } catch (e) {
    error.value = e.response?.data?.message || e.message
  } finally {
    actingId.value = null
  }
}

async function remove(ad) {
  if (actingId.value !== null || !window.confirm(`删除广告“${ad.title}”？`)) return
  actingId.value = ad.id
  error.value = ''
  notice.value = ''
  try {
    await props.request(`/advertisements/${ad.id}`, undefined, 'delete')
    if (ads.value.length === 1 && page.value > 1) page.value -= 1
    else await load()
    notice.value = '广告已删除'
  } catch (e) {
    error.value = e.response?.data?.message || e.message
  } finally {
    actingId.value = null
  }
}

watch(page, load)
watch(size, () => { if (page.value !== 1) page.value = 1; else load() })
onMounted(load)
</script>

<template>
  <section class="ad-management" aria-label="广告管理">
    <header class="ad-heading">
      <div><h3>广告管理</h3><p>管理广告内容、投放时段与展示状态</p></div>
      <div class="ad-heading-actions"><button type="button" class="ad-button" :disabled="loading" @click="load">刷新</button><button type="button" class="ad-button primary" @click="openEditor()">新增广告</button></div>
    </header>

    <div class="ad-metrics">
      <div><span>广告总数</span><strong>{{ stats.ads ?? '—' }}</strong></div>
      <div><span>投放金额</span><strong>¥{{ money(stats.revenue) }}</strong></div>
      <div><span>曝光量</span><strong>{{ stats.impressions ?? '—' }}</strong></div>
      <div><span>点击量</span><strong>{{ stats.clicks ?? '—' }}</strong></div>
    </div>

    <p v-if="error" class="ad-error" role="alert">{{ error }}</p>
    <p v-if="notice" class="ad-notice" role="status">{{ notice }}</p>

    <form v-if="editorOpen" ref="editor" class="ad-editor" @submit.prevent="save">
      <div class="ad-editor-heading"><h4>{{ editing ? '编辑广告' : '创建广告' }}</h4><button type="button" class="ad-button" @click="closeEditor">取消</button></div>
      <div class="ad-form-grid">
        <label>商家名称<input v-model.trim="form.merchantName" required maxlength="100" placeholder="商家或社团名称"></label>
        <label>广告标题<input v-model.trim="form.title" required maxlength="150" placeholder="展示给用户的标题"></label>
        <label>广告位置<select v-model="form.placement"><option v-for="placement in placements" :key="placement.value" :value="placement.value">{{ placement.label }}</option></select></label>
        <label>投放金额（元）<input v-model="form.amount" type="number" min="0" max="99999999" step="0.01" required></label>
        <label class="ad-wide">跳转链接<input v-model.trim="form.targetUrl" type="url" maxlength="500" placeholder="https://example.com"></label>
        <label>开始时间<input v-model="form.startAt" type="datetime-local"></label>
        <label>结束时间<input v-model="form.endAt" type="datetime-local" :min="form.startAt || undefined"></label>
      </div>
      <div class="ad-image-field">
        <div v-if="form.imageUrl" class="ad-image-preview"><img :src="imageSrc(form.imageUrl)" alt="广告图片预览"><button type="button" class="ad-button" @click="form.imageUrl = ''">移除图片</button></div>
        <label class="ad-upload">{{ uploading ? '正在上传…' : '上传广告图片' }}<input type="file" accept="image/jpeg,image/png" :disabled="uploading || saving" @change="uploadImage"></label>
        <small>JPG 或 PNG，最大 5MB</small>
      </div>
      <div class="ad-form-actions"><button class="ad-button primary" type="submit" :disabled="saving || uploading">{{ saving ? '保存中…' : editing ? '保存修改' : '创建广告' }}</button></div>
    </form>

    <div class="ad-list-heading"><h4>投放列表</h4><span>共 {{ total }} 条</span></div>
    <p v-if="loading" class="ad-empty" role="status">正在加载广告…</p>
    <div v-else-if="!ads.length" class="ad-empty">暂无广告。点击“新增广告”开始创建。</div>
    <div v-else class="ad-table-scroll"><table class="ad-table"><thead><tr><th>广告</th><th>位置 / 时段</th><th>金额</th><th>效果</th><th>状态</th><th>操作</th></tr></thead><tbody>
      <tr v-for="ad in ads" :key="ad.id">
        <td><div class="ad-identity"><img v-if="ad.image_url" :src="imageSrc(ad.image_url)" alt="" loading="lazy"><span v-else class="ad-placeholder">AD</span><div><strong>{{ ad.title }}</strong><small>{{ ad.merchant_name }}</small><a v-if="ad.target_url" :href="ad.target_url" target="_blank" rel="noopener noreferrer">查看链接 ↗</a></div></div></td>
        <td><strong>{{ placementLabel(ad.placement) }}</strong><small>{{ dateLabel(ad.start_at) }} 至 {{ dateLabel(ad.end_at) }}</small></td>
        <td>¥{{ money(ad.amount) }}</td>
        <td><span>曝光 {{ ad.impressions || 0 }}</span><small>点击 {{ ad.clicks || 0 }}</small></td>
        <td><span class="ad-status" :class="ad.status.toLowerCase()">{{ statusLabels[ad.status] || ad.status }}</span></td>
        <td><div class="ad-row-actions"><button type="button" class="ad-button" :disabled="actingId !== null" @click="openEditor(ad)">编辑</button><button type="button" class="ad-button" :disabled="actingId !== null" @click="changeStatus(ad, ad.status === 'ACTIVE' ? 'PAUSED' : 'ACTIVE')">{{ ad.status === 'ACTIVE' ? '暂停' : '投放' }}</button><button v-if="ad.status === 'PENDING'" type="button" class="ad-button" :disabled="actingId !== null" @click="changeStatus(ad, 'REJECTED')">驳回</button><button type="button" class="ad-button danger" :disabled="actingId !== null" @click="remove(ad)">删除</button></div></td>
      </tr>
    </tbody></table></div>
    <Pager v-model:page="page" v-model:size="size" :total="total" :busy="loading || actingId !== null" />
  </section>
</template>

<style scoped>
.ad-management{margin-top:16px;background:#fff;border:1px solid #d9e7e2;border-radius:8px;padding:24px;color:#244947}
.ad-heading,.ad-editor-heading,.ad-list-heading{display:flex;align-items:center;justify-content:space-between;gap:16px}
.ad-heading h3{font-size:20px;margin:0 0 4px}.ad-heading p{margin:0;color:#70837f;font-size:13px}.ad-heading-actions,.ad-row-actions,.ad-form-actions{display:flex;gap:8px;flex-wrap:wrap}
.ad-button{border:1px solid #cfdfd9;border-radius:6px;padding:8px 12px;background:#fff;color:#285d57;cursor:pointer;white-space:nowrap}.ad-button:hover{border-color:#2b766a;background:#f3faf7}.ad-button.primary{background:#276d64;color:#fff;border-color:#276d64}.ad-button.primary:hover{background:#205b54}.ad-button.danger{color:#a33d35}.ad-button:disabled{opacity:.5;cursor:default}
.ad-metrics{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:1px;margin:22px 0;background:#e1eae7;border:1px solid #e1eae7;border-radius:7px;overflow:hidden}.ad-metrics>div{padding:16px;background:#f7faf9}.ad-metrics span,.ad-metrics strong{display:block}.ad-metrics span{font-size:12px;color:#6d827c}.ad-metrics strong{font-size:23px;margin-top:4px;color:#215f58}
.ad-error,.ad-notice{padding:10px 12px;border-radius:6px;margin:14px 0}.ad-error{color:#a33d35;background:#fff1ef}.ad-notice{color:#1e6651;background:#ecf7ef}
.ad-editor{padding:20px 0;margin:18px 0;border-top:1px solid #dce9e4;border-bottom:1px solid #dce9e4}.ad-editor h4,.ad-list-heading h4{margin:0;font-size:16px}.ad-form-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:14px 18px;margin-top:18px}.ad-form-grid label{display:flex;flex-direction:column;gap:6px;font-size:13px;font-weight:600}.ad-form-grid input,.ad-form-grid select{width:100%;min-width:0;padding:10px 11px;border:1px solid #cbded7;border-radius:6px;background:#fff;color:#253f3b;font:inherit}.ad-form-grid input:focus,.ad-form-grid select:focus{outline:2px solid #aad4c7;outline-offset:1px}.ad-wide{grid-column:1/-1}.ad-image-field{display:flex;align-items:center;flex-wrap:wrap;gap:12px;margin-top:18px}.ad-upload{position:relative;display:inline-flex;padding:9px 12px;border:1px solid #cbded7;border-radius:6px;cursor:pointer;font-size:13px}.ad-upload input{position:absolute;inset:0;opacity:0;width:100%;cursor:pointer}.ad-image-field small{color:#758781}.ad-image-preview{display:flex;align-items:center;gap:10px}.ad-image-preview img{width:96px;height:58px;object-fit:cover;border:1px solid #d9e7e2;border-radius:5px}.ad-form-actions{justify-content:flex-end;margin-top:16px}
.ad-list-heading{margin:22px 0 10px}.ad-list-heading span{font-size:13px;color:#718680}.ad-empty{padding:34px 12px;text-align:center;color:#758781;border-top:1px solid #e1ebe7}.ad-table-scroll{overflow-x:auto}.ad-table{width:100%;border-collapse:collapse;min-width:780px}.ad-table th{font-size:12px;color:#78908a;font-weight:600;background:#f8fbfa}.ad-table th,.ad-table td{padding:14px 10px;text-align:left;border-bottom:1px solid #e4eeea;vertical-align:middle}.ad-table td{font-size:13px}.ad-table td strong,.ad-table td small{display:block}.ad-table td small{color:#748780;font-size:12px;margin-top:4px}.ad-identity{display:flex;align-items:center;gap:10px;min-width:180px}.ad-identity img,.ad-placeholder{width:60px;height:44px;border-radius:4px;object-fit:cover;flex:none}.ad-placeholder{display:grid;place-items:center;background:#e4f0eb;color:#438071;font-weight:700;font-size:11px}.ad-identity a{display:inline-block;color:#27766a;font-size:12px;margin-top:4px}.ad-status{display:inline-block;padding:4px 7px;border-radius:4px;background:#eef2f1;color:#647771;white-space:nowrap}.ad-status.active{background:#e5f4e9;color:#267148}.ad-status.paused{background:#f4efe2;color:#91732b}.ad-status.rejected{background:#f9eae8;color:#a4463f}.ad-row-actions{min-width:190px}.ad-row-actions .ad-button{font-size:12px;padding:6px 8px}
@media(max-width:750px){.ad-management{padding:16px}.ad-heading{align-items:flex-start}.ad-heading-actions{justify-content:flex-end}.ad-metrics{grid-template-columns:repeat(2,minmax(0,1fr))}.ad-form-grid{grid-template-columns:1fr}.ad-wide{grid-column:auto}}@media(max-width:480px){.ad-heading{flex-direction:column}.ad-heading-actions{justify-content:flex-start}.ad-metrics strong{font-size:19px}}
</style>
