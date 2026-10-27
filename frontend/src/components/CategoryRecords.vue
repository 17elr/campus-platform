<script setup>
import { computed, ref, watch } from "vue";
import ModerationActions from "./ModerationActions.vue";

const props = defineProps({
  kind: String,
  items: Array,
  loading: Boolean,
  acting: Number,
});
const emit = defineEmits(["inspect", "moderate"]);
const selectedId = ref(null);
const selected = computed(
  () =>
    props.items.find((item) => item.id === selectedId.value) || props.items[0],
);
watch(
  () => props.items,
  (items) => {
    if (!items.some((item) => item.id === selectedId.value))
      selectedId.value = items[0]?.id ?? null;
  },
);
const statusLabel = (value) =>
  ({
    PENDING: "待审核",
    APPROVED: "已公开",
    HIDDEN: "管理下架",
    WITHDRAWN: "用户撤回",
    DELETED: "用户已删除",
  })[value] || value;
const dateLabel = (value) => value?.replace("T", " ").slice(0, 16) || "未记录";
const imageUrl = (value) =>
  new URL(
    value,
    import.meta.env.VITE_API_BASE_URL || "http://127.0.0.1:8088/api",
  ).href;
const hasText = (value) => Boolean(value?.trim());
</script>

<template>
  <p v-if="!items.length && !loading" class="records-empty">
    没有匹配的信息，可调整筛选条件后重试。
  </p>

  <div v-else-if="kind === 'LOST'" class="lost-workbench">
    <div class="lost-queue" aria-label="寻物线索列表">
      <button
        v-for="item in items"
        :key="item.id"
        class="lost-record"
        :data-record-id="item.id"
        :aria-pressed="selected?.id === item.id"
        @click="selectedId = item.id"
      >
        <span class="lost-record-head"
          ><span>寻物线索 #{{ item.id }}</span
          ><span class="record-state" :data-state="item.status">{{
            statusLabel(item.status)
          }}</span></span
        >
        <strong>{{ item.title }}</strong>
        <span class="lost-excerpt">{{
          item.description || "未填写物品描述"
        }}</span>
        <span class="lost-record-foot"
          ><span
            >{{ item.location || "地点未填写" }} ·
            {{ item.category || "未分类" }}</span
          ><time>{{ dateLabel(item.createdAt) }}</time></span
        >
      </button>
    </div>

    <aside v-if="selected" class="lost-review" aria-label="当前线索核查">
      <div class="review-caption">
        <span>当前核查</span><b>#{{ selected.id }}</b>
      </div>
      <h3>{{ selected.title }}</h3>
      <p class="review-publisher">
        发布人 #{{ selected.userId }} · {{ dateLabel(selected.createdAt) }}
      </p>
      <img
        v-if="selected.imageUrl"
        class="lost-review-photo"
        :src="imageUrl(selected.imageUrl)"
        :alt="selected.title"
      />
      <div class="review-description">
        <span>物品特征与遗失线索</span>
        <p>{{ selected.description || "发布者尚未填写描述" }}</p>
      </div>
      <dl class="review-fields">
        <div>
          <dt>遗失地点</dt>
          <dd>{{ selected.location || "未填写" }}</dd>
        </div>
        <div>
          <dt>物品类别</dt>
          <dd>{{ selected.category || "未分类" }}</dd>
        </div>
        <div>
          <dt>联系信息</dt>
          <dd>{{ selected.contact || "未填写" }}</dd>
        </div>
      </dl>
      <div class="field-checks">
        <span>发布信息完整性</span>
        <p
          v-for="field in [
            { key: 'description', label: '物品描述' },
            { key: 'location', label: '遗失地点' },
            { key: 'contact', label: '联系方式' },
          ]"
          :key="field.key"
        >
          <span>{{ field.label }}</span
          ><b :class="{ missing: !hasText(selected[field.key]) }">{{
            hasText(selected[field.key]) ? "已填写" : "未填写"
          }}</b>
        </p>
      </div>
      <div class="review-decision">
        <p>核实线索与联系方式后，决定是否公开。</p>
        <ModerationActions
          :item="selected"
          :busy="loading"
          :acting="acting"
          @inspect="emit('inspect', $event)"
          @moderate="emit('moderate', $event)"
        />
      </div>
    </aside>
  </div>

  <div
    v-else-if="kind === 'FOUND'"
    class="found-board"
    aria-label="招领物品核验卡片"
  >
    <article
      v-for="item in items"
      :key="item.id"
      class="found-record"
      :data-record-id="item.id"
    >
      <div class="found-photo">
        <img
          v-if="item.imageUrl"
          :src="imageUrl(item.imageUrl)"
          :alt="item.title"
          loading="lazy"
        />
        <div v-else class="photo-placeholder">
          <svg
            viewBox="0 0 48 48"
            fill="none"
            stroke="currentColor"
            stroke-width="1.5"
            aria-hidden="true"
          >
            <rect x="8" y="10" width="32" height="28" rx="4" />
            <circle cx="19" cy="20" r="3" />
            <path d="m10 34 10-9 7 6 6-5 7 8" /></svg
          ><span>未上传物品照片</span>
        </div>
        <span class="record-state" :data-state="item.status">{{
          statusLabel(item.status)
        }}</span>
        <span class="found-reference">招领 #{{ item.id }}</span>
      </div>
      <div class="found-body">
        <span class="found-category">{{ item.category || "未分类物品" }}</span>
        <h3>{{ item.title }}</h3>
        <p class="found-description">
          {{ item.description || "未填写物品特征" }}
        </p>
        <dl>
          <div>
            <dt>拾获地点</dt>
            <dd>{{ item.location || "未填写" }}</dd>
          </div>
          <div>
            <dt>拾获发布人</dt>
            <dd>#{{ item.userId }}</dd>
          </div>
          <div>
            <dt>联系信息</dt>
            <dd>{{ item.contact || "未填写" }}</dd>
          </div>
        </dl>
      </div>
      <footer class="found-footer">
        <time>{{ dateLabel(item.createdAt) }} 发布</time
        ><ModerationActions
          :item="item"
          :busy="loading"
          :acting="acting"
          @inspect="emit('inspect', $event)"
          @moderate="emit('moderate', $event)"
        />
      </footer>
    </article>
  </div>

  <div
    v-else-if="kind === 'SECOND_HAND'"
    class="market-catalog"
    aria-label="二手商品审核目录"
  >
    <table>
      <thead>
        <tr>
          <th>商品 / 发布编号</th>
          <th>卖家与交接地点</th>
          <th>资料情况</th>
          <th>展示状态</th>
          <th>发布时间</th>
          <th>商品操作</th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="item in items"
          :key="item.id"
          class="market-record"
          :data-record-id="item.id"
        >
          <td>
            <div class="catalog-product">
              <div class="catalog-thumbnail">
                <img
                  v-if="item.imageUrl"
                  :src="imageUrl(item.imageUrl)"
                  :alt="item.title"
                  loading="lazy"
                /><svg
                  v-else
                  viewBox="0 0 36 36"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="1.3"
                  aria-label="未上传商品图片"
                  role="img"
                >
                  <path d="m5 11 13-6 13 6v15l-13 6-13-6V11Z" />
                  <path d="m5 11 13 7 13-7M18 18v14M11 8l14 7" />
                </svg>
              </div>
              <div class="catalog-title">
                <strong>{{ item.title }}</strong
                ><span>#{{ item.id }} · {{ item.category || "未分类" }}</span>
                <p>{{ item.description || "未填写商品说明" }}</p>
              </div>
            </div>
          </td>
          <td>
            <b class="catalog-seller">卖家 #{{ item.userId }}</b
            ><span class="catalog-location">{{
              item.location || "地点未填写"
            }}</span>
          </td>
          <td>
            <div class="catalog-checks">
              <span :class="{ incomplete: !item.imageUrl }">{{
                item.imageUrl ? "附有图片" : "缺少图片"
              }}</span
              ><span :class="{ incomplete: !hasText(item.description) }">{{
                hasText(item.description) ? "附有描述" : "缺少描述"
              }}</span
              ><span :class="{ incomplete: !hasText(item.contact) }">{{
                hasText(item.contact) ? "可联系卖家" : "缺少联系方式"
              }}</span>
            </div>
          </td>
          <td>
            <span class="record-state" :data-state="item.status">{{
              statusLabel(item.status)
            }}</span>
          </td>
          <td>
            <time>{{ dateLabel(item.createdAt) }}</time>
          </td>
          <td>
            <ModerationActions
              :item="item"
              :busy="loading"
              :acting="acting"
              @inspect="emit('inspect', $event)"
              @moderate="emit('moderate', $event)"
            />
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.records-empty {
  text-align: center;
  padding: 45px 20px;
  color: #637477;
}
@media (min-width: 901px) {
  .record-state {
    display: inline-block;
    padding: 5px 8px;
    border-radius: 5px;
    background: #eaf3ec;
    color: #32664f;
    font-size: 11px;
    white-space: nowrap;
    font-weight: 500;
  }
  .record-state[data-state="PENDING"] {
    background: #fbedd2;
    color: #855b1d;
  }
  .record-state[data-state="HIDDEN"] {
    background: #fae8e3;
    color: #9b4939;
  }
  .record-state[data-state="WITHDRAWN"],
  .record-state[data-state="DELETED"] {
    background: #eef0ed;
    color: #596a65;
  }
  .lost-workbench {
    display: grid;
    grid-template-columns: minmax(0, 1.1fr) minmax(300px, 0.9fr);
    gap: 24px;
    align-items: start;
  }
  .lost-queue {
    display: flex;
    flex-direction: column;
    gap: 12px;
    min-width: 0;
  }
  .lost-queue .lost-record {
    display: block;
    width: 100%;
    padding: 20px;
    text-align: left;
    border: 1px solid #dedfd4;
    border-left: 3px solid transparent;
    border-radius: 10px;
    background: #fffdf9;
    color: #183d50;
    transition:
      border-color 0.15s,
      background 0.15s;
  }
  .lost-queue .lost-record[aria-pressed="true"] {
    background: #fff8eb;
    border-color: #d8b879;
    border-left-color: #a36a23;
    box-shadow: 0 3px 12px #a36a2310;
  }
  .lost-queue .lost-record:hover {
    border-color: #bda779;
  }
  .lost-record-head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 12px;
    font-size: 11px;
    color: #6a715f;
  }
  .lost-record > strong {
    display: block;
    margin: 13px 0 8px;
    font-size: 17px;
    line-height: 1.5;
    color: #183d50;
    overflow-wrap: anywhere;
  }
  .lost-excerpt {
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    overflow: hidden;
    font-size: 13px;
    line-height: 1.8;
    color: #596e73;
  }
  .lost-record-foot {
    display: flex;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 8px;
    border-top: 1px solid #e7e3d6;
    padding-top: 13px;
    margin-top: 14px;
    font-size: 11px;
    color: #627475;
  }
  .lost-review {
    position: sticky;
    top: 24px;
    padding: 26px;
    border: 1px solid #ccd9d7;
    border-radius: 12px;
    background: #f2f6f4;
    min-width: 0;
  }
  .review-caption {
    display: flex;
    justify-content: space-between;
    color: #4f6c6d;
    font-size: 12px;
    letter-spacing: 0.5px;
  }
  .lost-review h3 {
    font-size: 22px;
    line-height: 1.5;
    margin: 16px 0 9px;
    color: #183d50;
    overflow-wrap: anywhere;
  }
  .review-publisher {
    font-size: 11px;
    color: #637477;
    line-height: 1.7;
    margin: 0;
  }
  .lost-review-photo {
    display: block;
    max-width: 100%;
    width: 100%;
    height: 170px;
    object-fit: contain;
    margin-top: 20px;
    background: #fff;
    border-radius: 7px;
  }
  .review-description {
    margin: 22px 0 0;
    border-top: 1px solid #d9e2db;
    padding-top: 20px;
  }
  .review-description > span,
  .field-checks > span {
    font-size: 11px;
    font-weight: 700;
    color: #526d6d;
  }
  .review-description p {
    font-size: 14px;
    color: #294a56;
    line-height: 1.9;
    white-space: pre-wrap;
    overflow-wrap: anywhere;
    margin: 9px 0 16px;
    max-height: 240px;
    overflow: auto;
  }
  .review-fields {
    margin: 0;
  }
  .review-fields > div {
    display: grid;
    grid-template-columns: 80px minmax(0, 1fr);
    gap: 12px;
    padding: 8px 0;
    font-size: 12px;
  }
  .review-fields dt {
    color: #607677;
  }
  .review-fields dd {
    margin: 0;
    overflow-wrap: anywhere;
    color: #294a56;
  }
  .field-checks {
    margin-top: 20px;
    padding: 16px;
    background: #fffdf9;
    border: 1px solid #dae1d7;
    border-radius: 8px;
  }
  .field-checks p {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    margin: 11px 0 0;
    font-size: 12px;
    color: #5b7174;
  }
  .field-checks b {
    font-weight: 500;
    color: #32664f;
  }
  .field-checks b.missing {
    color: #95592b;
  }
  .review-decision {
    margin-top: 20px;
    border-top: 1px solid #d9e2db;
    padding-top: 18px;
  }
  .review-decision > p {
    font-size: 11px;
    line-height: 1.8;
    color: #586f71;
    margin: 0 0 12px;
  }
  .found-board {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(min(100%, 290px), 1fr));
    gap: 20px;
  }
  .found-record {
    display: flex;
    flex-direction: column;
    min-width: 0;
    background: #fff;
    border: 1px solid #ceddd2;
    border-radius: 13px;
    overflow: hidden;
  }
  .found-photo {
    position: relative;
    height: 155px;
    background: #eaf0e8;
    border-bottom: 1px solid #d5dfd1;
  }
  .found-photo > img {
    width: 100%;
    height: 100%;
    display: block;
    object-fit: contain;
    padding: 12px;
  }
  .photo-placeholder {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 9px;
    width: 100%;
    height: 100%;
    color: #637d6a;
  }
  .photo-placeholder svg {
    width: 42px;
    height: 42px;
  }
  .photo-placeholder span {
    font-size: 11px;
  }
  .found-photo > .record-state {
    position: absolute;
    top: 12px;
    right: 12px;
    border: 1px solid #ffffffc0;
  }
  .found-reference {
    position: absolute;
    top: 16px;
    left: 15px;
    font-size: 11px;
    color: #456653;
  }
  .found-body {
    padding: 19px 20px 12px;
    flex: 1;
    min-width: 0;
  }
  .found-category {
    display: inline-block;
    font-size: 10px;
    color: #32664f;
    letter-spacing: 0.5px;
    border-bottom: 1px solid #a4c0aa;
    padding-bottom: 5px;
  }
  .found-body h3 {
    font-size: 18px;
    line-height: 1.5;
    color: #183d50;
    margin: 10px 0 9px;
    overflow-wrap: anywhere;
  }
  .found-description {
    font-size: 12px;
    line-height: 1.8;
    color: #5b7074;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    margin: 0 0 15px;
    min-height: 43px;
  }
  .found-body dl {
    margin: 0;
    padding-top: 12px;
    border-top: 1px dashed #cad9cc;
  }
  .found-body dl > div {
    display: grid;
    grid-template-columns: 72px minmax(0, 1fr);
    gap: 10px;
    font-size: 12px;
    padding: 5px 0;
  }
  .found-body dt {
    color: #61746a;
  }
  .found-body dd {
    margin: 0;
    color: #294a56;
    overflow-wrap: anywhere;
  }
  .found-footer {
    padding: 11px 20px 18px;
  }
  .found-footer > time {
    display: block;
    font-size: 10px;
    color: #61746a;
    margin-bottom: 13px;
  }
  .market-catalog {
    border: 1px solid #d6ddda;
    border-radius: 11px;
    overflow: auto;
    background: #fff;
  }
  .market-catalog table {
    border-collapse: collapse;
    width: 100%;
    text-align: left;
    font-size: 12px;
  }
  .market-catalog th {
    padding: 14px 16px;
    white-space: nowrap;
    color: #516c70;
    background: #edf2f3;
    border-bottom: 1px solid #d6ddda;
    font-weight: 600;
  }
  .market-catalog td {
    padding: 18px 16px;
    border-bottom: 1px solid #e2e7e3;
    vertical-align: middle;
    color: #294a56;
  }
  .market-catalog tbody tr:last-child td {
    border-bottom: 0;
  }
  .market-catalog tbody tr:hover {
    background: #fbfcf8;
  }
  .catalog-product {
    display: flex;
    gap: 15px;
    align-items: center;
    min-width: 210px;
    max-width: 390px;
  }
  .catalog-thumbnail {
    display: grid;
    place-items: center;
    width: 64px;
    height: 70px;
    background: #f2eadc;
    border: 1px solid #e2d5c1;
    border-radius: 9px;
    color: #8a7250;
    flex-shrink: 0;
  }
  .catalog-thumbnail img {
    display: block;
    max-width: 100%;
    max-height: 100%;
    object-fit: contain;
  }
  .catalog-thumbnail svg {
    width: 32px;
    height: 32px;
  }
  .catalog-title {
    min-width: 0;
  }
  .catalog-title strong {
    font-size: 14px;
    line-height: 1.6;
    display: block;
    overflow-wrap: anywhere;
  }
  .catalog-title > span {
    font-size: 10px;
    color: #62747a;
    display: block;
    margin: 5px 0;
  }
  .catalog-title p {
    margin: 0;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    color: #62747a;
    font-size: 12px;
    line-height: 1.7;
    overflow-wrap: anywhere;
  }
  .catalog-seller {
    display: block;
    white-space: nowrap;
    font-weight: 500;
  }
  .catalog-location {
    display: block;
    font-size: 11px;
    margin-top: 7px;
    color: #62747a;
  }
  .catalog-checks {
    display: flex;
    flex-direction: column;
    gap: 7px;
    color: #32664f;
    font-size: 11px;
    white-space: nowrap;
  }
  .catalog-checks > span:before {
    content: "";
    display: inline-block;
    width: 5px;
    height: 5px;
    border-radius: 50%;
    background: #5d8e71;
    margin-right: 6px;
    vertical-align: middle;
  }
  .catalog-checks > .incomplete {
    color: #8c5c30;
  }
  .catalog-checks > .incomplete:before {
    background: #bf985e;
  }
  .market-catalog time {
    white-space: nowrap;
    font-size: 11px;
    color: #62747a;
  }
  .lost-record:focus-visible {
    outline: 2px solid #956622;
    outline-offset: 3px;
  }
}
</style>
