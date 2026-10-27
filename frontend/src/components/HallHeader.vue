<script setup>
import { computed } from "vue";

const props = defineProps({
  counts: Object,
  locations: Array,
  ready: Boolean,
  loading: Boolean,
});
const emit = defineEmits(["search", "publish"]);
const kind = defineModel("kind", { type: String, default: "" });
const keyword = defineModel("keyword", { type: String, default: "" });
const location = defineModel("location", { type: String, default: "" });
const category = defineModel("category", { type: String, default: "" });
const sections = [
  {
    key: "",
    label: "全部信息",
    short: "发现校园",
    en: "EXPLORE THE CAMPUS",
    title: "你关心的校园小事，在这里找到答案。",
    description: "找回遗失的物品，发现需要的好物，也让你的善意被看见。",
    placeholder: "校园卡、耳机、教材……你想找什么？",
    action: "发布一条信息",
  },
  {
    key: "LOST",
    label: "寻物启事",
    short: "寻找失物",
    en: "LOST & LOOKING",
    title: "别着急，让更多人帮你一起找。",
    description: "先看看有没有人拾到，再留下物品特征和遗失地点，让线索找到你。",
    placeholder: "输入遗失物品的名称或特征",
    action: "发布寻物启事",
  },
  {
    key: "FOUND",
    label: "失物招领",
    short: "让失物回家",
    en: "FOUND & RETURNED",
    title: "你拾到的，也许是别人正在找的。",
    description: "拾到物品时留下线索，认领物品时核对特征，让每次归还更安心。",
    placeholder: "输入物品名称，查看是否已被拾到",
    action: "发布招领信息",
  },
  {
    key: "SECOND_HAND",
    label: "二手闲置",
    short: "校园好物集",
    en: "THE CAMPUS MARKET",
    title: "让闲置流动，让喜欢继续。",
    description: "教材、数码、生活好物。把用不到的分享出去，把正需要的带回来。",
    placeholder: "搜索你想淘的校园好物",
    action: "发布我的闲置",
  },
];
const current = computed(
  () => sections.find((section) => section.key === kind.value) || sections[0],
);
const total = computed(() =>
  sections
    .slice(1)
    .reduce(
      (sum, section) => sum + Number(props.counts?.[section.key] || 0),
      0,
    ),
);
function count(key) {
  return props.ready ? (key ? (props.counts?.[key] ?? 0) : total.value) : "—";
}
function resetFilters() {
  keyword.value = "";
  location.value = "";
  category.value = "";
}
</script>

<template>
  <section
    class="hall-discovery"
    :data-kind="kind || 'ALL'"
    aria-labelledby="hall-heading"
  >
    <div class="discovery-workspace">
      <div class="discovery-main">
        <div class="discovery-eyebrow">
          <span></span>{{ current.en }}<b>{{ current.short }}</b>
        </div>
        <h2 id="hall-heading">{{ current.title }}</h2>
        <p class="discovery-description">{{ current.description }}</p>

        <form
          class="discovery-search"
          role="search"
          @submit.prevent="emit('search')"
        >
          <div class="discovery-search-line">
            <label class="discovery-keyword">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="1.8"
                aria-hidden="true"
              >
                <circle cx="10.8" cy="10.8" r="6.8" />
                <path d="m16 16 4.5 4.5" />
              </svg>
              <span class="discovery-sr-only">搜索校园信息</span>
              <input
                v-model="keyword"
                type="search"
                :placeholder="current.placeholder"
                aria-controls="hall-results"
              />
            </label>
            <button class="discovery-submit" :disabled="loading">
              {{ loading ? "搜索中…" : "搜索信息"
              }}<span aria-hidden="true">→</span>
            </button>
          </div>
          <div class="discovery-filters">
            <label
              ><span>所在地点</span
              ><select v-model="location">
                <option
                  v-for="place in locations"
                  :key="place"
                  :value="place === '全部地点' ? '' : place"
                >
                  {{ place }}
                </option>
              </select></label
            >
            <label
              ><span>物品类别</span
              ><input v-model="category" placeholder="如：书籍、电子产品"
            /></label>
            <button
              v-if="keyword || location || category"
              type="button"
              class="discovery-reset"
              @click="resetFilters"
            >
              清除筛选
            </button>
          </div>
        </form>
        <div class="discovery-shortcuts">
          <span>{{ kind === "SECOND_HAND" ? "逛逛分类" : "常用地点" }}</span>
          <template v-if="kind === 'SECOND_HAND'"
            ><button
              v-for="value in ['书籍', '电子产品', '生活用品']"
              :key="value"
              :aria-pressed="category === value"
              @click="category = category === value ? '' : value"
            >
              {{ value }}
            </button></template
          >
          <template v-else
            ><button
              v-for="place in ['图书馆', '教学楼', '学生宿舍']"
              :key="place"
              :aria-pressed="location === place"
              @click="location = location === place ? '' : place"
            >
              {{ place }}
            </button></template
          >
        </div>
      </div>

      <aside class="discovery-side" :aria-label="current.short + '指南'">
        <template v-if="!kind">
          <div class="discovery-side-heading">
            <span>校园信息站</span><small>从一件小事开始</small>
          </div>
          <button
            v-for="section in sections.slice(1)"
            :key="section.key"
            class="discovery-channel"
            @click="kind = section.key"
          >
            <span
              :class="['channel-mark', section.key.toLowerCase()]"
              aria-hidden="true"
            >
              <svg
                v-if="section.key === 'LOST'"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="1.7"
              >
                <circle cx="10" cy="10" r="6" />
                <path d="m14.5 14.5 5 5M7 10h6m-3-3v6" />
              </svg>
              <svg
                v-else-if="section.key === 'FOUND'"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="1.7"
              >
                <path d="m12 3 8 4v6c0 4-8 8-8 8s-8-4-8-8V7l8-4Z" />
                <path d="m8 12 3 3 5-6" />
              </svg>
              <svg
                v-else
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="1.7"
              >
                <path d="M5 7h14l1 14H4L5 7Z" />
                <path d="M8 8V6a4 4 0 0 1 8 0v2" />
              </svg>
            </span>
            <span class="channel-copy"
              ><b>{{ section.label }}</b
              ><small>{{ section.short }}</small></span
            >
            <strong>{{ count(section.key) }}</strong
            ><span class="channel-arrow" aria-hidden="true">↗</span>
          </button>
        </template>
        <template v-else-if="kind === 'LOST'">
          <div class="discovery-side-heading">
            <span>寻找，从线索开始</span><small>LOST & LOOKING</small>
          </div>
          <ol class="discovery-steps">
            <li>
              <b>01</b>
              <div>
                <strong>回想最后出现的地点</strong>
                <p>按图书馆、教学楼等地点缩小范围。</p>
              </div>
            </li>
            <li>
              <b>02</b>
              <div>
                <strong>描述独有的物品特征</strong>
                <p>颜色、型号和标记，有助于准确匹配。</p>
              </div>
            </li>
          </ol>
          <div class="discovery-note">
            <strong>{{ count("FOUND") }}</strong
            ><span>条招领信息<br /><small>也许就有你的物品</small></span
            ><button @click="kind = 'FOUND'">去看看 →</button>
          </div>
        </template>
        <template v-else-if="kind === 'FOUND'">
          <div class="discovery-side-heading">
            <span>一份善意，一次归还</span><small>RETURN WITH CARE</small>
          </div>
          <div class="claim-ticket">
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="1.4"
              aria-hidden="true"
            >
              <path d="m12 3 8 4v6c0 4-8 8-8 8s-8-4-8-8V7l8-4Z" />
              <path d="m8 12 3 3 5-6" /></svg
            ><strong>认领之前，记得核验</strong>
            <p>保留一处独有特征，请认领人描述；见面时核对身份与物品。</p>
            <span>校内公共地点 · 当面交接</span>
          </div>
        </template>
        <template v-else>
          <div class="discovery-side-heading">
            <span>校园循环计划</span><small>PASS IT FORWARD</small>
          </div>
          <div class="market-summary">
            <svg viewBox="0 0 96 80" fill="none" aria-hidden="true">
              <rect
                x="12"
                y="17"
                width="30"
                height="49"
                rx="4"
                fill="#e6c79f"
                transform="rotate(-8 12 17)"
              />
              <path
                d="m21 29 13-2m-12 9 13-2"
                stroke="#825b34"
                stroke-width="2"
              />
              <path
                d="M48 28h33l4 39H45l3-39Z"
                fill="#e0e9dd"
                stroke="#5e7b66"
                stroke-width="2"
              />
              <path
                d="M56 31V21a9 9 0 0 1 18 0v10"
                stroke="#5e7b66"
                stroke-width="2"
              />
            </svg>
            <div>
              <strong>{{ count("SECOND_HAND") }}</strong
              ><span>件好物，等待新主人</span>
            </div>
          </div>
          <p class="market-note">
            说明使用情况与物品瑕疵，校内见面，确认后再交易。
          </p>
        </template>
        <button class="discovery-publish" @click="emit('publish')">
          <span aria-hidden="true">＋</span>{{ current.action
          }}<span aria-hidden="true">↗</span>
        </button>
      </aside>
    </div>

    <nav class="discovery-tabs" aria-label="信息分类">
      <button
        v-for="section in sections"
        :key="section.key"
        :aria-pressed="kind === section.key"
        :class="{ 'is-current': kind === section.key }"
        @click="kind = section.key"
      >
        {{ section.label }}<span>{{ count(section.key) }}</span>
      </button>
      <span class="discovery-tabs-note">校内互助，从这里连接</span>
    </nav>
  </section>
</template>

<style scoped>
.hall-discovery {
  display: none;
}
@media (min-width: 901px) {
  .hall-discovery {
    --discovery-accent: #204e65;
    --discovery-tint: #ecf2f4;
    display: block;
    margin-bottom: 26px;
    border: 1px solid #ded9ce;
    border-radius: 20px;
    background: #fffdf9;
    box-shadow: 0 10px 32px #193d5808;
    overflow: hidden;
    color: #183d50;
  }
  .hall-discovery[data-kind="LOST"] {
    --discovery-accent: #a05a2d;
    --discovery-tint: #f8eee2;
  }
  .hall-discovery[data-kind="FOUND"] {
    --discovery-accent: #32664f;
    --discovery-tint: #edf3eb;
  }
  .hall-discovery[data-kind="SECOND_HAND"] {
    --discovery-accent: #806035;
    --discovery-tint: #f7efdf;
  }
  .discovery-workspace {
    display: grid;
    grid-template-columns: minmax(0, 1fr) minmax(220px, 28%);
  }
  .discovery-main {
    min-width: 0;
    padding: 30px 32px 24px;
  }
  .discovery-eyebrow {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 9px;
    color: var(--discovery-accent);
    font-size: 10px;
    font-weight: 700;
    letter-spacing: 1.8px;
  }
  .discovery-eyebrow > span {
    width: 7px;
    height: 7px;
    border-radius: 2px;
    background: var(--discovery-accent);
  }
  .discovery-eyebrow b {
    padding-left: 10px;
    border-left: 1px solid #d9d6cc;
    font-size: 11px;
    letter-spacing: 0;
  }
  .discovery-main h2 {
    max-width: 20em;
    margin: 19px 0 10px;
    color: #183d50;
    font-size: clamp(28px, 2.15vw, 40px);
    font-weight: 700;
    line-height: 1.4;
    letter-spacing: -1px;
    text-wrap: balance;
  }
  .discovery-description {
    max-width: 52em;
    margin: 0;
    font-size: 13px;
    line-height: 1.8;
    color: #647477;
  }
  .discovery-search {
    margin-top: 25px;
  }
  .discovery-search-line {
    display: flex;
    gap: 8px;
    padding: 6px;
    border: 1px solid #c5cfcc;
    border-radius: 11px;
    background: #fff;
    box-shadow: 0 3px 7px #183d5004;
  }
  .discovery-search-line:focus-within {
    border-color: var(--discovery-accent);
    box-shadow: 0 0 0 3px var(--discovery-tint);
  }
  .discovery-keyword {
    min-width: 0;
    flex: 1;
    display: flex;
    align-items: center;
    gap: 10px;
    padding-left: 9px;
  }
  .discovery-keyword svg {
    width: 21px;
    height: 21px;
    flex-shrink: 0;
    color: var(--discovery-accent);
  }
  .discovery-keyword input {
    min-width: 0;
    width: 100%;
    min-height: 40px;
    border: 0;
    background: transparent;
    font: inherit;
    font-size: 13px;
    color: #183d50;
  }
  .discovery-keyword input:focus {
    outline-offset: -2px;
  }
  .discovery-keyword input::placeholder,
  .discovery-filters input::placeholder {
    color: #647477;
  }
  .discovery-submit {
    display: flex;
    align-items: center;
    gap: 16px;
    justify-content: center;
    border-radius: 7px;
    padding: 12px 17px;
    background: var(--discovery-accent);
    color: #fff;
    font-size: 13px;
    font-weight: 600;
    white-space: nowrap;
  }
  .discovery-submit:hover {
    filter: brightness(1.1);
  }
  .discovery-submit:disabled {
    opacity: 0.6;
    cursor: progress;
  }
  .discovery-filters {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-top: 14px;
    flex-wrap: wrap;
  }
  .discovery-filters label {
    display: flex;
    align-items: center;
    gap: 8px;
    flex: 1;
    min-width: 150px;
  }
  .discovery-filters label > span {
    color: #647477;
    white-space: nowrap;
    font-size: 12px;
  }
  .discovery-filters input,
  .discovery-filters select {
    width: 100%;
    min-width: 0;
    min-height: 34px;
    padding: 6px 9px;
    border: 1px solid #d2d8d2;
    border-radius: 6px;
    background: #fff;
    color: #385461;
    font-size: 12px;
  }
  .discovery-reset {
    background: transparent;
    color: var(--discovery-accent);
    font-size: 12px;
    padding: 8px 0;
    text-decoration: underline;
    text-underline-offset: 3px;
  }
  .discovery-shortcuts {
    display: flex;
    align-items: center;
    gap: 13px;
    flex-wrap: wrap;
    margin-top: 18px;
    font-size: 11px;
  }
  .discovery-shortcuts > span {
    color: #647477;
  }
  .discovery-shortcuts button {
    border: 0;
    padding: 5px 0;
    color: #526b73;
    background: transparent;
    font-size: 12px;
  }
  .discovery-shortcuts button:hover,
  .discovery-shortcuts button[aria-pressed="true"] {
    color: var(--discovery-accent);
    text-decoration: underline;
    text-underline-offset: 4px;
  }
  .discovery-side {
    display: flex;
    flex-direction: column;
    padding: 26px 22px 22px;
    background: var(--discovery-tint);
    border-left: 1px solid #e1e2d9;
    min-width: 0;
  }
  .discovery-side-heading {
    display: block;
    margin-bottom: 14px;
  }
  .discovery-side-heading > span {
    display: block;
    font-size: 14px;
    font-weight: 700;
    color: #183d50;
  }
  .discovery-side-heading small {
    display: block;
    margin-top: 6px;
    font-size: 10px;
    letter-spacing: 0.6px;
    color: #566e73;
  }
  .discovery-channel {
    display: flex;
    gap: 11px;
    align-items: center;
    padding: 12px 0;
    width: 100%;
    border: 0;
    border-bottom: 1px solid #d6dfdc;
    background: transparent;
    color: #183d50;
    text-align: left;
  }
  .channel-mark {
    display: grid;
    place-items: center;
    width: 33px;
    height: 33px;
    border-radius: 9px;
    background: #fffdf9;
    color: #315c79;
    flex-shrink: 0;
  }
  .channel-mark.found {
    color: #32664f;
  }
  .channel-mark.second_hand {
    color: #8b5c2c;
  }
  .channel-mark svg {
    width: 20px;
    height: 20px;
  }
  .channel-copy {
    flex: 1;
    min-width: 0;
  }
  .channel-copy b {
    display: block;
    font-size: 12px;
  }
  .channel-copy small {
    display: block;
    margin-top: 4px;
    font-size: 10px;
    color: #5f7478;
  }
  .discovery-channel > strong {
    font-size: 20px;
    font-variant-numeric: tabular-nums;
  }
  .channel-arrow {
    color: #526e77;
  }
  .discovery-channel:hover .channel-copy b {
    text-decoration: underline;
    text-underline-offset: 4px;
  }
  .discovery-publish {
    display: flex;
    justify-content: space-between;
    gap: 8px;
    align-items: center;
    margin-top: auto;
    padding: 13px 12px;
    width: 100%;
    border: 1px solid #a6b9b2;
    border-radius: 8px;
    background: #fffdf9;
    color: var(--discovery-accent);
    font-size: 12px;
    font-weight: 700;
  }
  .discovery-side > .discovery-publish {
    margin-top: 19px;
  }
  .discovery-publish:hover {
    background: #fff;
    border-color: var(--discovery-accent);
  }
  .discovery-steps {
    list-style: none;
    padding: 0;
    margin: 0;
  }
  .discovery-steps li {
    display: flex;
    gap: 10px;
    margin: 14px 0 18px;
  }
  .discovery-steps li > b {
    font-size: 11px;
    color: #a05a2d;
    padding-top: 2px;
  }
  .discovery-steps strong {
    font-size: 12px;
  }
  .discovery-steps p {
    margin: 6px 0 0;
    font-size: 11px;
    line-height: 1.7;
    color: #6c6052;
  }
  .discovery-note {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 9px;
    padding-top: 14px;
    border-top: 1px dashed #cbbb9f;
  }
  .discovery-note > strong {
    font-size: 27px;
    color: var(--discovery-accent);
  }
  .discovery-note > span {
    font-size: 11px;
    line-height: 1.6;
  }
  .discovery-note small {
    font-size: 10px;
    color: #6c6052;
  }
  .discovery-note button {
    margin-left: auto;
    padding: 6px 0;
    font-size: 11px;
    color: var(--discovery-accent);
    background: transparent;
  }
  .claim-ticket {
    margin-top: 3px;
    padding: 17px 15px;
    border: 1px dashed #9cb6a2;
    border-radius: 10px;
    background: #fffefa;
  }
  .claim-ticket svg {
    width: 34px;
    height: 34px;
    display: block;
    color: #32664f;
    margin-bottom: 12px;
  }
  .claim-ticket > strong {
    font-size: 13px;
    display: block;
  }
  .claim-ticket p {
    margin: 9px 0 14px;
    color: #53675a;
    font-size: 11px;
    line-height: 1.9;
  }
  .claim-ticket > span {
    display: block;
    padding-top: 12px;
    border-top: 1px dashed #becdbd;
    font-size: 10px;
    color: #32664f;
  }
  .market-summary {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    margin: 4px 0 10px;
  }
  .market-summary svg {
    width: 96px;
    height: 80px;
  }
  .market-summary > div {
    display: flex;
    flex-wrap: wrap;
    align-items: baseline;
    gap: 8px;
  }
  .market-summary strong {
    font-size: 30px;
    color: var(--discovery-accent);
  }
  .market-summary span {
    font-size: 11px;
  }
  .market-note {
    margin: 0;
    color: #6d5f49;
    font-size: 11px;
    line-height: 1.9;
  }
  .discovery-tabs {
    display: flex;
    align-items: stretch;
    flex-wrap: wrap;
    gap: 6px;
    padding: 0 24px;
    border-top: 1px solid #e1e2d9;
    background: #fffdf9;
  }
  .discovery-tabs button {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 17px 14px;
    border: 0;
    border-bottom: 3px solid transparent;
    background: transparent;
    font-size: 13px;
    color: #536b74;
  }
  .discovery-tabs button span {
    padding: 2px 6px;
    border-radius: 4px;
    background: #f1f1ec;
    color: #5d7074;
    font-size: 11px;
    font-variant-numeric: tabular-nums;
  }
  .discovery-tabs button.is-current {
    color: var(--discovery-accent);
    border-bottom-color: var(--discovery-accent);
    font-weight: 700;
  }
  .discovery-tabs button.is-current span {
    background: var(--discovery-tint);
    color: var(--discovery-accent);
  }
  .discovery-tabs-note {
    display: flex;
    align-items: center;
    margin-left: auto;
    font-size: 11px;
    color: #647477;
    padding: 8px 0;
  }
  .discovery-sr-only {
    position: absolute;
    width: 1px;
    height: 1px;
    padding: 0;
    margin: -1px;
    overflow: hidden;
    clip-path: inset(50%);
    white-space: nowrap;
  }
  button:focus-visible,
  input:focus-visible,
  select:focus-visible {
    outline: 2px solid var(--discovery-accent);
    outline-offset: 3px;
  }
}
</style>
