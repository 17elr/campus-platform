const {chromium}=require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const assert=require('node:assert/strict');
const {execFileSync}=require('node:child_process');
const fs=require('node:fs'),path=require('node:path');
const out=path.resolve(__dirname,'../test-results');fs.mkdirSync(out,{recursive:true});
const apiBase=process.env.API_BASE||'http://127.0.0.1:8080/api';
async function api(url,data,token){const r=await fetch(apiBase+url,{method:data?'POST':'GET',headers:{'Content-Type':'application/json',...(token?{Authorization:'Bearer '+token}:{})},body:data?JSON.stringify(data):undefined});const j=await r.json();assert.equal(j.code,200,j.message);return j.data;}
(async()=>{
 let item;const browser=await chromium.launch({channel:'msedge',headless:true});
 try{
 const seller=await api('/auth/login',{username:'demo1',password:'demo12345'});
 item=await api('/items',{title:'浏览器推广验证 '+Date.now(),description:'仅供自动化回归，结束后清理',type:'SECOND_HAND',category:'测试类别',location:'图书馆',contact:'test'},seller.token);
 const context=await browser.newContext({viewport:{width:1440,height:1000}});const page=await context.newPage();const errors=[];page.on('pageerror',e=>errors.push(e.message));
 await page.addInitScript(token=>localStorage.setItem('campus_token',token),seller.token);
 await page.goto(process.env.TEST_BASE_URL||'http://localhost:5173');
 await page.getByRole('button',{name:'个人中心',exact:true}).waitFor();
 await page.getByRole('button',{name:'探索校园信息'}).click();
 await page.locator('.card').filter({hasText:item.title}).getByRole('button',{name:'查看详情'}).click();
 await page.getByRole('button',{name:'创建推广订单',exact:true}).click();
 await page.getByRole('button',{name:'确认模拟支付',exact:true}).click();
 await page.getByText('模拟支付成功，推广已生效。',{exact:true}).waitFor();
 await page.locator('.detail-dialog .close').click();
 await page.locator('.card').first().getByText(item.title,{exact:true}).waitFor();
 await page.locator('.card').first().getByText('推广 · 置顶',{exact:true}).waitFor();
 await page.screenshot({path:path.join(out,'promotion-hall.png'),fullPage:true});
 const before=await page.locator('.card h3').first().textContent();
 await page.locator('.list-pager').getByRole('button',{name:'下一页'}).click();
 await page.waitForFunction(title=>document.querySelector('.card h3')?.textContent!==title,before);
 await page.getByRole('button',{name:'个人中心',exact:true}).click();
 await page.getByRole('button',{name:'推广订单',exact:true}).click();
 await page.locator('.promotion-orders article').filter({hasText:item.title}).getByText(/推广中/).waitFor();
 await page.screenshot({path:path.join(out,'promotion-account.png'),fullPage:true});
 const admin=await api('/auth/login',{username:'admin',password:'admin123'});
 await page.evaluate(token=>localStorage.setItem('campus_token',token),admin.token);
 // Replace the startup token for subsequent navigation.
 await context.clearCookies();
 const adminPage=await browser.newPage({viewport:{width:1440,height:1000}});
 adminPage.on('pageerror',e=>errors.push(e.message));await adminPage.addInitScript(token=>localStorage.setItem('campus_token',token),admin.token);
 await adminPage.goto(process.env.TEST_BASE_URL||'http://localhost:5173');
 await adminPage.getByRole('button',{name:'管理员后台',exact:true}).click();
 await adminPage.locator('.promotion-orders article').filter({hasText:item.title}).waitFor();
 const stats=await api('/admin/promotions/stats',undefined,admin.token);
 assert((await adminPage.locator('.promotion-stats').textContent()).includes(Number(stats.revenue).toFixed(2)));
 await adminPage.screenshot({path:path.join(out,'promotion-admin.png'),fullPage:true});
 await page.setViewportSize({width:390,height:844});await page.screenshot({path:path.join(out,'promotion-mobile.png'),fullPage:true});
 assert.deepEqual(errors,[]);
 fs.writeFileSync(path.join(out,'promotion-browser-report.json'),JSON.stringify({passed:['real detail purchase and payment','promotion badge and ranking','hall server pagination','account real order','admin real revenue'],uncaughtErrors:errors},null,2));
 console.log('PASS real browser purchase, ranking, pagination, account and admin revenue; no page errors');
 }finally{
 await browser.close();
 if(item)execFileSync('mysql',['-u','root','-D','campus_platform','-e',`DELETE FROM item_promotion WHERE item_id=${Number(item.id)}; DELETE FROM item WHERE id=${Number(item.id)};`],{env:{...process.env,MYSQL_PWD:process.env.DB_PASSWORD||'123456'}});
 }
})().catch(e=>{console.error(e);process.exitCode=1});
