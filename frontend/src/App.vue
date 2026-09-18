<script setup>
import { computed, ref, onMounted, watch, onUnmounted } from "vue";

import axios from "axios";
import Comments from "./components/Comments.vue";
import Home from "./components/Home.vue";
import Admin from "./components/Admin.vue";
import Account from "./components/Account.vue";
// 每次打开页面先展示首页，站内导航仍使用 hash。
const currentPage = ref('home');
window.history.replaceState(null, '', window.location.pathname + window.location.search + '#home');
function navigate(page) {
  currentPage.value = page;
  window.location.hash = page;
  window.scrollTo(0, 0);
}
function browse(value = "") {
  hallPage.value = 1;
  type.value = value;
  keyword.value = "";
  location.value = "";
  navigate("hall");
}
window.addEventListener("hashchange", () => {
  currentPage.value =
    window.location.hash === "#hall"
      ? "hall"
      : window.location.hash === "#admin"
        ? "admin"
        : window.location.hash === "#account" ? "account" : "home";
});
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "http://127.0.0.1:8080/api",
  timeout: 10000,
});
const user = ref(null),
  mode = ref("login"),
  busy = ref(false),
  error = ref("");
const form = ref({ username: "", password: "", nickname: "", confirm: "" });
function openAuth(value = "login") {
  mode.value = value;
  error.value = "";
  form.value.password = "";
  form.value.confirm = "";
  showLogin.value = true;
}
async function request(url, data, method = "post") {
  const token = localStorage.getItem("campus_token");
  const r = await api.request({
    url,
    data,
    method,
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  if (r.data.code !== 200) throw new Error(r.data.message || "操作失败");
  return r.data.data;
}
async function submitAuth() {
  if (busy.value) return;
  error.value = "";
  if (mode.value === "register" && form.value.password !== form.value.confirm) {
    error.value = "两次密码输入不一致";
    return;
  }
  busy.value = true;
  try {
    const { username, password, nickname } = form.value;
    const data = await request("/auth/" + mode.value, {
      username,
      password,
      nickname,
    });
    if (mode.value === "register") {
      mode.value = "login";
      form.value.password = "";
      error.value = "注册成功，请登录";
      return;
    }
    if (!data?.token || !data?.user?.id) throw new Error("登录响应无效");
    localStorage.setItem("campus_token", data.token);
    user.value = data.user;
    showLogin.value = false;
    form.value.password = "";
    notice.value = "登录成功";
  } catch (e) {
    error.value =
      e.response?.data?.message ||
      (e.request && !e.response
        ? "无法连接登录服务，请确认后端已启动"
        : e.message);
  } finally {
    busy.value = false;
  }
}
async function logout() {
  try {
    await request("/auth/logout");
  } catch {
  } finally {
    localStorage.removeItem("campus_token");
    user.value = null;
    notice.value = "已退出登录";
  }
}
onMounted(async () => {
  if (!localStorage.getItem("campus_token")) return;
  try {
    user.value = await request("/auth/me", undefined, "get");
  } catch {
    localStorage.removeItem("campus_token");
  }
});

const types = [
  ["", "全部信息"],
  ["LOST", "寻物启事"],
  ["FOUND", "失物招领"],
  ["SECOND_HAND", "二手闲置"],
];
const seed = [
  [
    "FOUND",
    "在图书馆拾到校园卡",
    "蓝色校园卡，姓名李同学，请带学生证认领",
    "图书馆",
    "证件",
  ],
  [
    "LOST",
    "寻找蓝色保温杯",
    "周五下午遗落，杯身有白色贴纸",
    "教学楼",
    "生活用品",
  ],
  [
    "SECOND_HAND",
    "转让考研英语资料",
    "全新未使用，校内当面交易",
    "学生宿舍",
    "书籍",
  ],
  [
    "FOUND",
    "操场捡到黑色耳机",
    "充电盒有划痕，请描述品牌后认领",
    "操场",
    "电子产品",
  ],
  ["LOST", "求助寻找宿舍钥匙", "三把钥匙串在蓝色挂件上", "学生宿舍", "钥匙"],
  [
    "SECOND_HAND",
    "九成新台灯转让",
    "光线柔和，支持三档调节",
    "二号食堂",
    "生活用品",
  ],
  ["FOUND", "食堂拾到雨伞", "透明长柄雨伞，放在服务台", "一号食堂", "生活用品"],
  ["LOST", "寻找白色帆布包", "内有课程笔记和水杯", "图书馆", "箱包"],
  [
    "SECOND_HAND",
    "闲置绘图板低价出",
    "适合设计专业同学，功能正常",
    "实训楼",
    "电子产品",
  ],
  ["FOUND", "教学楼拾到学生证", "请联系失物招领处", "教学楼", "证件"],
  ["LOST", "寻找银色项链", "昨晚在操场附近掉落", "操场", "饰品"],
  [
    "SECOND_HAND",
    "转让专业课教材",
    "多本教材一起出，保存良好",
    "学生宿舍",
    "书籍",
  ],
];
const demo = ref(
  seed.map((x, i) => ({
    id: i + 1,
    type: x[0],
    title: x[1],
    description: x[2],
    location: x[3],
    category: x[4],
    createdAt: `2026-09-${String(12 - (i % 5)).padStart(2, "0")}`,
    favoriteCount: 3 + i * 2,
  })),
);
const keyword = ref(""),
  type = ref(""),
  location = ref(""),
  sort = ref("最新发布"),
  selected = ref(null),
  favorites = ref([]),
  notice = ref(""),
  showLogin = ref(false);
const locations = [
  "全部地点",
  "图书馆",
  "教学楼",
  "学生宿舍",
  "操场",
  "一号食堂",
  "二号食堂",
  "实训楼",
];
const tags = ["校园卡", "耳机", "教材", "钥匙", "保温杯", "帆布包"];
const announcements = [
  "失物认领请携带有效证件",
  "二手交易建议选择公共区域",
  "请勿发布虚假或违规信息",
];
const hallPage = ref(1), hallSize = 12;
const filtered = computed(() =>
  demo.value
    .filter(
      (x) =>
        (!type.value || x.type === type.value) &&
        (!location.value || x.location === location.value) &&
        (x.title + x.description + x.category).includes(keyword.value.trim()),
    )
    .sort((a, b) =>
      sort.value === "最多收藏"
        ? b.favoriteCount - a.favoriteCount
        : b.id - a.id,
    ),
);
const itemCounts = ref({});
  const pagedFiltered = computed(() => filtered.value.slice((hallPage.value-1)*hallSize, hallPage.value*hallSize));
const itemsReady = ref(false),
  itemsError = ref("");
async function loadItems() {
  try {
    const data = await request("/items?page=1&size=50", undefined, "get");
    itemCounts.value = data.counts || {};
    demo.value = (data.records || data).map((x) => ({
      ...x,
      favoriteCount: x.favoriteCount || 0,
    }));
    itemsReady.value = true;
    itemsError.value = "";
  } catch {
    itemsReady.value = false;
    itemsError.value = "暂未连接物品服务，当前显示演示内容；演示物品不能留言。";
  }
}
onMounted(loadItems);
function favorite(x) {
  if (!user.value) {
    openAuth();
    return;
  }
  if (!favorites.value.includes(x.id)) favorites.value.push(x.id);
  else favorites.value = favorites.value.filter((id) => id !== x.id);
  notice.value = favorites.value.includes(x.id)
    ? "已收藏这条信息"
    : "已取消收藏";
}
async function buy(x) {
  if (!user.value) {
    openAuth();
    return;
  }
  try {
    const o = await request("/trade/orders", {
      productId: x.id,
      amount: x.price || 29,
      paymentMethod: "ALIPAY",
    });
    await request("/trade/orders/" + o.id + "/pay", undefined, "post");
    notice.value = "支付成功，订单已创建";
    selected.value = null;
  } catch (e) {
    notice.value = e.message || "购买失败";
  }
}
function needLogin(action) {
  if (!user.value) {
    openAuth();
    return;
  }
  if (action === "发布") {
    publishError.value = "";
    showPublish.value = true;
  }
}
let noticeTimer;
watch(notice, (value) => {
  clearTimeout(noticeTimer);
  if (value)
    noticeTimer = setTimeout(() => {
      notice.value = "";
    }, 3000);
});
onUnmounted(() => clearTimeout(noticeTimer));
const showPublish = ref(false),
  publishing = ref(false),
  publishError = ref("");
const emptyItem = () => ({
  title: "",
  description: "",
  type: "LOST",
  category: "",
  location: "",
  contact: "",
  imageUrl: "",
});
const publishForm = ref(emptyItem());
const uploading = ref(false);
function imageSrc(path) {
  return new URL(path, api.defaults.baseURL).href;
}
async function uploadImage(event) {
  const file = event.target.files[0];
  event.target.value = "";
  if (!file) return;
  publishError.value = "";
  if (
    !["image/jpeg", "image/png"].includes(file.type) ||
    file.size > 5 * 1024 * 1024
  ) {
    publishError.value = "请选择 5MB 以内的 JPG 或 PNG 图片";
    return;
  }
  uploading.value = true;
  try {
    const data = new FormData();
    data.append("file", file);
    publishForm.value.imageUrl = await request("/images", data);
  } catch (e) {
    publishError.value = e.response?.data?.message || "图片上传失败，请重试";
  } finally {
    uploading.value = false;
  }
}
async function publishItem() {
  if (publishing.value || uploading.value) return;
  publishing.value = true;
  publishError.value = "";
  try {
    await request("/items", publishForm.value);
    showPublish.value = false;
    publishForm.value = emptyItem();
    notice.value = "发布成功，信息已公开展示";
  } catch (e) {
    publishError.value =
      e.response?.data?.message || e.message || "发布失败，请重试";
  } finally {
    publishing.value = false;
  }
}
</script>
<template>
  <div class="portal" :class="{ 'home-layout': currentPage !== 'hall', 'admin-layout': currentPage === 'admin' || currentPage === 'account' }">
    <aside class="left">
      <div class="brand">
        <b>校</b><span>校园互助<small>CAMPUS CONNECT</small></span>
      </div>
      <p class="caption">探索校园</p>
      <button :class="{active:currentPage==='home'}" @click="navigate('home')">首页</button>
      <p class="caption nav-divider">信息分类</p>
      <button
        v-for="t in types"
        :key="t[0]"
        :class="{ active: currentPage === 'hall' && type === t[0] }"
        @click="browse(t[0])"
      >
        {{ t[1] }}
        <em v-if="t[0]">{{ itemCounts[t[0]] ?? demo.filter((x) => x.type === t[0]).length }}</em>
      </button>
      <p v-if="user" class="caption nav-divider">我的空间</p>
      <button v-if="user" :class="{active:currentPage==='account'}" @click="navigate('account')">个人中心</button>
      <button v-if="user?.role==='ADMIN'" :class="{active:currentPage==='admin'}" @click="navigate('admin')">管理员后台</button>
      <div class="notice-box">
        <strong>校园公告</strong>
        <p v-for="a in announcements" :key="a">{{ a }}</p>
      </div>
      <strong class="caption">热门标签</strong>
      <div class="tags">
        <button
          v-for="t in tags"
          :key="t"
          @click="
            browse();
            keyword = t;
          "
        >
          # {{ t }}
        </button>
      </div>
      <div class="user-box">
        <span>{{(user?.nickname || user?.username || '访').slice(0,1)}}</span>
        <div>
          <b>{{ user?.nickname || user?.username || "访客用户" }}</b
          ><small>{{
            user
              ? user.role === "ADMIN"
                ? "管理员"
                : "普通用户"
              : "可自由浏览信息"
          }}</small>
        </div>
      </div>
    </aside>
    <main>
      <header>
        <div>
          <span class="crumb"
            >校园生活 /
            {{
              currentPage === "admin"
                ? "管理员后台"
                : currentPage === "home"
                  ? "首页"
                  : "信息大厅"
            }}</span
          >
          <h1>
            {{
              currentPage === "admin"
                ? "平台管理"
                : currentPage === "home"
                  ? "欢迎来到校园互助"
                  : "校园互助信息大厅"
            }}
          </h1>
          <p>
            {{
              currentPage === "home"
                ? "连接你我，共享美好校园。"
                : "让遗失的物品回家，让闲置的好物继续流动。"
            }}
          </p>
        </div>
        <div>
          <template v-if="!user"
            ><button class="login" @click="openAuth()">登录</button
            ><button class="outline" @click="openAuth('register')">
              注册
            </button></template
          ><template v-else
            ><span>{{ user.nickname || user.username }}</span
            ><button class="login" @click="logout">退出登录</button></template
          ><button class="primary" @click="needLogin('发布')">
            ＋ 发布信息
          </button>
        </div>
      </header>
      <section v-if="currentPage === 'account'"><Account v-if="user" :key="user.id" :user="user" :request="request" @changed="loadItems"/><p v-else>请先登录后查看个人中心。</p></section>
<section v-else-if="currentPage === 'admin'">
        <Admin
          v-if="user?.role === 'ADMIN'"
          :request="request"
          @changed="loadItems"
        />
        <p v-else>请使用管理员账号登录。</p>
      </section>
      <Home
        v-else-if="currentPage === 'home'"
        :items="demo"
        :ready="itemsReady"
        :error="itemsError"
        @browse="browse"
        @detail="selected = $event"
        @retry="loadItems"
      /><template v-else
        ><section class="hero">
          <div>
            <span>在校园，遇见彼此的善意</span>
            <h2>每一次寻找，<br />都值得被认真回应。</h2>
            <button class="hero-btn" @click="needLogin('发布')">
              发布一条互助信息 →
            </button>
          </div>
          <div class="hero-rule">
            <small>校园互助公约</small
            ><b>真实发布<br />友好沟通<br />当面核实</b>
          </div>
        </section>
        <div class="stats">
          <div><small>今日新增</small><b>28</b><span>较昨日 +12%</span></div>
          <div><small>待认领</small><b>16</b><span>正在寻找主人</span></div>
          <div><small>已解决</small><b>132</b><span>本月累计</span></div>
          <div><small>二手闲置</small><b>46</b><span>等待新主人</span></div>
        </div>
        <div class="search">
          <input
            v-model="keyword"
            placeholder="搜索物品名称、特征或描述…"
          /><select v-model="location">
            <option
              v-for="x in locations"
              :key="x"
              :value="x === '全部地点' ? '' : x"
            >
              {{ x }}
            </option></select
          ><select v-model="sort">
            <option>最新发布</option>
            <option>最多收藏</option></select
          ><button class="primary">搜索</button>
        </div>
        <div class="section-title">
          <div>
            <h2>{{ types.find((x) => x[0] === type)?.[1] || "全部信息" }}</h2>
            <p>发现校园里正在发生的互助</p>
          </div>
          <span>{{ filtered.length }} 条信息</span>
        </div>
        <p v-if="itemsError" role="alert">
          {{ itemsError }} <button @click="loadItems">重新连接</button>
        </p>
        <div class="grid">
          <article v-for="x in pagedFiltered" :key="x.id" class="card">
            <div class="card-head">
              <span :class="['tag', x.type.toLowerCase()]">{{
                types.find((t) => t[0] === x.type)?.[1]
              }}</span
              ><small>{{ x.createdAt }}</small>
            </div>
            <img
              v-if="x.imageUrl"
              class="item-photo"
              :src="imageSrc(x.imageUrl)"
              :alt="x.title"
            />
            <h3>{{ x.title }}</h3>
            <p>{{ x.description }}</p>
            <div class="card-meta">⌖ {{ x.location }} · {{ x.category }}</div>
            <div class="card-actions">
              <button @click="selected = x">查看详情</button
              ><button @click="favorite(x)">
                ♡ {{ x.favoriteCount + (favorites.includes(x.id) ? 1 : 0) }}
              </button>
            </div>
          </article>
        </div><div v-if="filtered.length > hallSize" class="hall-pager"><button :disabled="hallPage===1" @click="hallPage--">上一页</button><span>第 {{hallPage}} / {{Math.ceil(filtered.length / hallSize)}} 页</span><button :disabled="hallPage>=Math.ceil(filtered.length / hallSize)" @click="hallPage++">下一页</button></div></template
      >
    </main>
    <aside v-if="currentPage === 'hall'" class="right">
      <div class="right-card">
        <div class="right-title">
          <h3>今日平台</h3>
          <span>实时更新</span>
        </div>
        <div class="big-number">132<small>条公开信息</small></div>
        <div class="progress"><i></i></div>
        <p>本周互助成功率 <b>86%</b></p>
      </div>
      <div class="right-card">
        <h3>最新公告</h3>
        <ul>
          <li v-for="a in announcements" :key="a">
            {{ a }}<small>今天</small>
          </li>
        </ul>
      </div>
      <div class="right-card">
        <h3>热门地点</h3>
        <div
          class="place"
          v-for="(p, i) in ['图书馆', '教学楼', '学生宿舍', '操场']"
          :key="p"
        >
          <span>{{ i + 1 }}</span
          ><b>{{ p }}</b
          ><em>{{ 18 - i * 3 }} 条</em>
        </div>
      </div>
      <div class="right-card rules">
        <h3>互助规则</h3>
        <p>发布真实有效的信息</p>
        <p>当面核实物品与身份</p>
        <p>文明沟通，保护隐私</p>
        <button class="text">查看完整规则 →</button>
      </div>
    </aside>
    <div v-if="notice" class="toast">{{ notice }}</div>
    <div v-if="selected" class="modal" @click.self="selected = null">
      <div class="dialog detail-dialog">
        <button class="close" @click="selected = null">×</button
        ><span class="tag">{{
          types.find((t) => t[0] === selected.type)?.[1]
        }}</span>
        <h2>{{ selected.title }}</h2>
        <p>{{ selected.description }}</p>
        <hr />
        <p>地点：{{ selected.location }}</p>
        <p>物品类别：{{ selected.category }}</p>
        <img
          v-if="selected.imageUrl"
          class="item-photo"
          :src="imageSrc(selected.imageUrl)"
          :alt="selected.title"
        />
        <p v-if="selected.type === 'SECOND_HAND'">
          价格：¥{{ selected.price || 29 }}
        </p>
        <button
          v-if="selected.type === 'SECOND_HAND'"
          class="primary"
          @click="buy(selected)"
        >
          立即购买
        </button>
        <p v-if="itemsReady">联系方式：{{ selected.contact || "未填写" }}</p>
        <Comments
          v-if="itemsReady"
          :key="selected.id"
          :item-id="selected.id"
          :user="user"
          @login="openAuth()"
        />
        <p v-else>演示物品暂不支持留言，请连接服务后重试。</p>
      </div>
    </div>
    <div v-if="showPublish" class="modal">
      <form class="dialog detail-dialog" @submit.prevent="publishItem">
        <button
          type="button"
          class="close"
          :disabled="publishing || uploading"
          @click="showPublish = false"
        >
          ×
        </button>
        <h2>发布校园信息</h2>
        <p>提交后将立即公开展示。</p>
        <label
          >信息类型<select v-model="publishForm.type">
            <option v-for="t in types.slice(1)" :key="t[0]" :value="t[0]">
              {{ t[1] }}
            </option>
          </select></label
        ><label
          >标题<input
            v-model.trim="publishForm.title"
            required
            maxlength="100" /></label
        ><label
          >描述<textarea
            v-model.trim="publishForm.description"
            required
            maxlength="5000"
            rows="4"
          ></textarea></label
        ><label
          >物品类别（可选）<select v-model="publishForm.category">
            <option value="">未选择</option>
            <option
              v-for="category in [
                '书籍',
                '证件',
                '电子产品',
                '生活用品',
                '钥匙',
                '箱包',
                '其他',
              ]"
              :key="category"
            >
              {{ category }}
            </option>
          </select></label
        ><label
          >物品图片（可选，JPG/PNG，最大5MB）<input
            type="file"
            accept="image/jpeg,image/png"
            :disabled="uploading || publishing"
            @change="uploadImage"
        /></label>
        <p v-if="uploading" role="status">正在上传图片…</p>
        <div v-if="publishForm.imageUrl">
          <img
            class="item-photo"
            :src="imageSrc(publishForm.imageUrl)"
            alt="物品图片预览"
          /><button
            type="button"
            :disabled="uploading || publishing"
            @click="publishForm.imageUrl = ''"
          >
            移除图片
          </button>
        </div>
        <label
          >地点<input
            v-model.trim="publishForm.location"
            required
            maxlength="100" /></label
        ><label
          >联系方式<input
            v-model.trim="publishForm.contact"
            required
            maxlength="100"
        /></label>
        <p v-if="publishError" role="alert">{{ publishError }}</p>
        <button class="primary full" :disabled="publishing || uploading">
          {{ publishing ? "正在提交…" : "立即发布" }}
        </button>
      </form>
    </div>
    <div v-if="showLogin" class="modal" @click.self="showLogin = false">
      <form class="dialog login-dialog" @submit.prevent="submitAuth">
        <button
          type="button"
          class="close"
          aria-label="关闭登录"
          @click="showLogin = false"
        >
          ×
        </button>
        <h2>{{ mode === "login" ? "登录校园互助" : "注册校园互助" }}</h2>
        <p>浏览无需登录，发布和互动时再登录即可。</p>
        <label
          >用户名<input
            v-model.trim="form.username"
            required
            pattern="[A-Za-z0-9_]{3,30}"
            maxlength="30"
            autocomplete="username"
            placeholder="3-30位字母、数字或下划线" /></label
        ><label v-if="mode === 'register'"
          >昵称<input
            v-model.trim="form.nickname"
            required
            maxlength="50"
            autocomplete="nickname" /></label
        ><label
          >密码<input
            v-model="form.password"
            type="password"
            required
            :minlength="mode === 'register' ? 8 : 1"
            maxlength="72"
            :autocomplete="
              mode === 'login' ? 'current-password' : 'new-password'
            " /></label
        ><label v-if="mode === 'register'"
          >确认密码<input
            v-model="form.confirm"
            type="password"
            required
            autocomplete="new-password"
        /></label>
        <p v-if="error" role="alert" style="color: #b34332">{{ error }}</p>
        <button class="primary full" :disabled="busy">
          {{ busy ? "正在提交…" : mode === "login" ? "登录" : "注册" }}</button
        ><button
          type="button"
          class="text full"
          @click="openAuth(mode === 'login' ? 'register' : 'login')"
        >
          {{ mode === "login" ? "没有账号？立即注册" : "已有账号？返回登录" }}
        </button>
      </form>
    </div>
  </div>
</template>









<style scoped>.hall-pager{display:flex;justify-content:center;align-items:center;gap:18px;margin:28px 0 42px}.hall-pager button{padding:10px 18px;border-radius:7px;background:#e4eef0;color:#24645f}.hall-pager button:disabled{opacity:.45}</style>

