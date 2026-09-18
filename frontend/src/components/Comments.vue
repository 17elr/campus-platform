<script setup>
import { ref, watch } from 'vue'
import axios from 'axios'
const props=defineProps({itemId:Number,user:Object})
const emit=defineEmits(['login'])
const rows=ref([]),content=ref(''),reply=ref(null),error=ref(''),loading=ref(false),busy=ref(false),confirmDelete=ref(null)
let generation=0
async function api(method,suffix='',data){const token=localStorage.getItem('campus_token');const r=await axios.request({method,url:`${import.meta.env.VITE_API_BASE_URL || 'http://127.0.0.1:8080/api'}/items/${props.itemId}/comments${suffix}`,data,timeout:10000,headers:token?{Authorization:`Bearer ${token}`}:{}});if(r.data.code!==200)throw Error(r.data.message);return r.data.data}
function message(e){return e.response?.data?.message || (e.request&&!e.response?'留言加载失败，请检查服务后重试':e.message)}
async function load(){const g=++generation;loading.value=true;error.value='';try{const data=await api('get');if(g===generation)rows.value=data}catch(e){if(g===generation)error.value=message(e)}finally{if(g===generation)loading.value=false}}
watch(()=>props.itemId,()=>{rows.value=[];content.value='';reply.value=null;confirmDelete.value=null;load()},{immediate:true})
watch(()=>props.user,()=>{reply.value=null;confirmDelete.value=null})
async function send(){if(!props.user){emit('login');return}if(busy.value||!content.value.trim())return;busy.value=true;error.value='';try{await api('post','',{content:content.value.trim(),parentId:reply.value?.id||null});content.value='';reply.value=null;await load()}catch(e){error.value=message(e)}finally{busy.value=false}}
async function remove(id){if(busy.value)return;busy.value=true;try{await api('delete','/'+id);confirmDelete.value=null;await load()}catch(e){error.value=message(e)}finally{busy.value=false}}
function respond(row){if(!props.user){emit('login');return}reply.value=row}
</script>
<template><section class="comments"><h3>留言交流 <small>{{rows.filter(r=>!r.deleted).length}} 条留言</small></h3><p v-if="loading" role="status">正在加载留言…</p><div v-if="error" role="alert">{{error}} <button @click="load">重试</button></div><p v-if="!loading&&!error&&!rows.length">还没有留言，欢迎交流物品信息。</p><article v-for="row in rows" :key="row.id" class="comment-row"><header><b>{{row.nickname || '校园用户'}}</b><time>{{row.createdAt?.replace('T',' ')}}</time></header><small v-if="row.parentId">回复 {{rows.find(r=>r.id===row.parentId)?.nickname || '已删除留言'}}：</small><p>{{row.content}}</p><div v-if="!row.deleted"><button @click="respond(row)">回复</button><button v-if="user&&(user.id===row.userId||user.role==='ADMIN')" @click="confirmDelete=row.id">删除</button><span v-if="confirmDelete===row.id">确认删除？ <button :disabled="busy" @click="remove(row.id)">确认删除</button><button @click="confirmDelete=null">取消</button></span></div></article><form v-if="user" @submit.prevent="send"><p v-if="reply">回复 {{reply.nickname}} <button type="button" @click="reply=null">取消回复</button></p><label>你的留言<textarea v-model="content" required maxlength="500" rows="3" placeholder="询问物品详情，或与发布者交流…"></textarea></label><div class="comment-submit"><small>{{content.length}} / 500</small><button class="primary" :disabled="busy||!content.trim()">{{busy?'提交中…':'发表留言'}}</button></div></form><button v-else class="outline" @click="emit('login')">登录后留言</button></section></template>
