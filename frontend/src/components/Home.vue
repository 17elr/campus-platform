<script setup>
import { computed, ref, onMounted } from "vue";
const ads = ref([]),
  adApi = import.meta.env.VITE_API_BASE_URL || "http://127.0.0.1:8088/api";
function adClick(id) {
  fetch(adApi + "/advertisements/" + id + "/click", { method: "POST" });
}
function adImage(path) {
  return new URL(path, adApi).href;
}
onMounted(async () => {
  try {
    const r = await fetch(adApi + "/advertisements?placement=HOME_BANNER");
    const j = await r.json();
    ads.value = j.data || [];
    for (const ad of ads.value)
      fetch(adApi + "/advertisements/" + ad.id + "/impression", {
        method: "POST",
      }).catch(() => {});
  } catch {}
});
const props = defineProps({ items: Array, ready: Boolean, error: String });
const emit = defineEmits(["browse", "detail", "retry"]);
const latest = computed(() => props.items.slice(0, 3));
const services = [
  {
    type: "LOST",
    no: "01",
    en: "LOST & LOOKING",
    title: "寻找遗失的珍贵",
    text: "一张校园卡、一串钥匙。把线索留下，让寻找多一份可能。",
    link: "查看寻物启事",
    tone: "blue",
  },
  {
    type: "FOUND",
    no: "02",
    en: "FOUND & RETURNED",
    title: "让善意找到主人",
    text: "拾到的不只是物品，也是别人的牵挂。帮助它回到原处。",
    link: "查看失物招领",
    tone: "green",
  },
  {
    type: "SECOND_HAND",
    no: "03",
    en: "SHARE & REUSE",
    title: "好物，值得再相遇",
    text: "读过的书、闲置的日常。把使用价值传递给下一个人。",
    link: "逛逛二手闲置",
    tone: "amber",
  },
];
</script>
<template>
  <div class="home-page">
    <section v-if="ads.length" class="ad-strip">
      <a
        v-for="ad in ads"
        :key="ad.id"
        :href="ad.targetUrl || '#'"
        target="_blank"
        @click="adClick(ad.id)"
      >
        <img v-if="ad.imageUrl" :src="adImage(ad.imageUrl)" :alt="ad.title" />
        <div v-else>
          <small>{{ ad.merchantName }}</small>
          <b>{{ ad.title }}</b>
        </div>
      </a>
    </section>

    <section class="home-hero">
      <div class="home-copy">
        <span class="home-kicker">CAMPUS CONNECT / 校园互助社区</span>
        <h2>在这里，<br />每一份善意<br /><em>都有回响。</em></h2>
        <p>
          连接校园里的你我。让失物回家，让闲置流动，也让平凡的一天，多一点温暖。
        </p>
        <div class="home-hero-actions">
          <button class="home-cta" @click="emit('browse', '')">
            探索校园信息 <span>↗</span>
          </button>
          <span class="home-footnote">自由浏览 · 真实交流 · 校内互助</span>
        </div>
      </div>

      <aside class="home-hero-side" aria-label="快速入口">
        <div class="hero-side-head">
          <div>
            <span class="home-kicker">QUICK START</span>
            <h3>你想做什么？</h3>
          </div>
          <strong>{{ latest.length || "—" }}</strong>
        </div>
        <button
          v-for="service in services"
          :key="service.type"
          class="hero-queue"
          @click="emit('browse', service.type)"
        >
          <span class="queue-index">{{ service.no }}</span>
          <span class="queue-copy">
            <strong>{{ service.title }}</strong>
            <small>{{ service.link }}</small>
          </span>
          <span class="queue-arrow">↗</span>
        </button>
        <div class="hero-side-note">
          <span class="live-dot"></span>
          <span>校园信息持续更新中</span>
        </div>
      </aside>
    </section>

    <div class="home-strip">
      <span>校园里的事，在校园里解决。</span>
      <p>不必独自寻找，也不必让好物闲置。</p>
      <button @click="emit('browse', '')">进入信息大厅 →</button>
    </div>

    <section class="home-services">
      <div class="home-section-intro">
        <span class="home-kicker">BROWSE BY PURPOSE</span>
        <h2>从一个明确的入口开始。</h2>
        <p>按你的需要，直接进入对应的信息流。</p>
      </div>
      <div class="home-service-grid">
        <button
          v-for="service in services"
          :key="service.type"
          :class="['service-card', service.tone]"
          @click="emit('browse', service.type)"
        >
          <div>
            <span>{{ service.en }}</span>
            <b>{{ service.no }}</b>
          </div>
          <h3>{{ service.title }}</h3>
          <p>{{ service.text }}</p>
          <footer>{{ service.link }} <span>↗</span></footer>
        </button>
      </div>
    </section>

    <section class="home-latest">
      <div class="home-section-head">
        <div>
          <span class="home-kicker">LATEST UPDATES</span>
          <h2>校园推荐与新鲜事</h2>
        </div>
        <button @click="emit('browse', '')">查看全部信息 →</button>
      </div>
      <p v-if="!ready" class="home-data-note">
        {{ error || "正在加载校园信息。" }}
        <button v-if="error" @click="emit('retry')">重新连接</button>
      </p>
      <div v-if="latest.length" class="home-news">
        <button
          v-for="item in latest"
          :key="item.id"
          class="home-news-card"
          @click="emit('detail', item)"
        >
          <div>
            <span>{{
              services
                .find((service) => service.type === item.type)
                ?.link.replace("查看", "")
                .replace("逛逛", "")
            }}</span>
            <time>{{ item.createdAt?.slice(0, 10) }}</time>
          </div>
          <h3>{{ item.title }}</h3>
          <span v-if="item.promotions?.length" class="promotion-badge"
            >推广推荐</span
          >
          <p>{{ item.description }}</p>
          <footer>{{ item.location || "校内" }} <span>查看详情 ↗</span></footer>
        </button>
      </div>
      <p v-else class="home-empty">还没有公开信息，期待校园里的第一份分享。</p>
    </section>

    <section class="home-guide">
      <div>
        <span class="home-kicker">HOW IT WORKS</span>
        <h2>互助，从简单开始。</h2>
        <p>先浏览，再交流，让每次相遇更安心。</p>
      </div>
      <ol>
        <li>
          <b>01</b>
          <div>
            <strong>发现线索</strong>
            <p>按物品类型和地点，查找你关心的信息。</p>
          </div>
        </li>
        <li>
          <b>02</b>
          <div>
            <strong>留言交流</strong>
            <p>登录后在详情中提问，确认物品特征。</p>
          </div>
        </li>
        <li>
          <b>03</b>
          <div>
            <strong>当面核实</strong>
            <p>选择校内公共地点，核实后认领或交换。</p>
          </div>
        </li>
      </ol>
    </section>

    <footer class="home-footer">
      <b>校园互助</b>
      <span>让物品有归属，让善意有回响。</span>
      <button @click="emit('browse', '')">开始探索 ↗</button>
    </footer>
  </div>
</template>
