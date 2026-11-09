import {cached,native} from './transport.mjs';
import {chineseNews,englishNews,cctvNews,cctvURL} from './news-model.mjs';
import {leagues} from './domain.mjs';
const esc=s=>String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const $=s=>document.querySelector(s);
const when=t=>t&&Number.isFinite(Date.parse(t))?new Date(t).toLocaleString('zh-CN',{timeZone:'Asia/Shanghai',month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit',hour12:false}):'时间未提供';
const topics=[['1','推荐'],['3','英超'],['5','西甲'],['4','意甲'],['6','德甲']];
export function createNews(prefs,save){
 let controller,articles=[],next='',pages=0,source=prefs.newsSource==='en'?'en':prefs.newsProvider==='zh'?'zh':'cctv',topic='1',league='eng.1';
 const url=()=>source==='cctv'?cctvURL:source==='zh'?'https://api.dongqiudi.com/app/tabs/web/'+topic+'.json':'https://site.api.espn.com/apis/site/v2/sports/soccer/'+league+'/news?limit=25';
 const stop=()=>controller?.abort();
 function render(force=false){
  stop();articles=[];next='';pages=0;
  $('#main').innerHTML=`<div class="page-heading"><div><h2>足球资讯</h2><p>追踪赛场内外的新动态</p></div></div><div class="segmented" aria-label="资讯语言"><button data-news-source="cctv" class="${source==='cctv'?'active':''}">央视网</button><button data-news-source="zh" class="${source==='zh'?'active':''}">懂球帝</button><button data-news-source="en" class="${source==='en'?'active':''}">ESPN 英文</button></div><div class="news-topics">${source==='cctv'?'<p class="muted">足球文字报道 · 手机网页阅读</p>':source==='zh'?`<div class="chips">${topics.map(([id,title])=>`<button data-news-topic="${id}" class="${topic===id?'active':''}">${title}</button>`).join('')}</div>`:`<select id="news-league" aria-label="资讯赛事">${leagues.map(l=>`<option value="${l[0]}" ${l[0]===league?'selected':''}>${l[1]}</option>`).join('')}</select>`}</div><p id="news-status" class="source" role="status">正在读取资讯…</p><section id="news-content" aria-label="足球资讯列表"><div class="loading-card"></div><div class="loading-card"></div></section><button id="news-more" class="wide" hidden>加载更多</button>`;
  document.querySelectorAll('[data-news-source]').forEach(b=>b.onclick=()=>{source=b.dataset.newsSource;prefs.newsSource=source==='en'?'en':'zh';if(source!=='en')prefs.newsProvider=source;save();render();});
  document.querySelectorAll('[data-news-topic]').forEach(b=>b.onclick=()=>{topic=b.dataset.newsTopic;render();});
  if($('#news-league'))$('#news-league').onchange=e=>{league=e.target.value;render();};
  $('#news-more').onclick=()=>load(true);load(false,force);
 }
 async function load(more=false,force=false){
  stop();controller=new AbortController();const signal=controller.signal,target=$('#news-content'),status=$('#news-status'),button=$('#news-more');
  if(!target)return;const address=more?next:url(),parser=source==='cctv'?cctvNews:source==='zh'?chineseNews:englishNews;if(!address)return;button.disabled=true;
  try{
   const r=await cached(address,600000,signal,force),data=source==='cctv'?parser(r.value,address):parser(r.value);if(signal.aborted||!target.isConnected)return;
   articles=[...new Map([...(more?articles:[]),...data.articles].map(a=>[a.id,a])).values()];pages++;next=data.next===address?'':data.next;
   status.textContent=(source==='cctv'?'央视网 · 中文报道':source==='zh'?'懂球帝 · 原生中文':'ESPN · 英文原文')+' · '+when(new Date(r.time).toISOString())+(r.stale?' · 离线缓存，内容可能过期':' 获取');
   target.innerHTML=articles.map((a,i)=>`<button class="news-card ${i===0?'news-lead':''}" data-article="${esc(a.url)}"><span class="news-copy"><span class="news-kicker">${esc(a.tag||'足球')}</span><strong>${esc(a.title)}</strong><small>${esc(a.source)} · ${esc(when(a.time))}<span aria-hidden="true"> ↗</span></small></span>${a.image?`<img class="news-cover" src="${esc(a.image)}" loading="lazy" alt="">`:''}</button>`).join('')||'<p class="empty">这个栏目暂时没有足球资讯</p>';
   target.querySelectorAll('[data-article]').forEach(b=>b.onclick=()=>native('openArticle',{url:b.dataset.article}));
   target.querySelectorAll('img').forEach(i=>i.onerror=()=>i.remove());button.hidden=!next||pages>=5;button.textContent='加载更多';
  }catch(e){if(signal.aborted||!target.isConnected)return;status.textContent='资讯连接失败，可重试或切换来源';if(!articles.length)target.innerHTML='<div class="empty">暂时无法获取资讯<br><button id="news-retry">重新加载</button></div>';else button.textContent='加载失败，点击重试';const retry=$('#news-retry');if(retry)retry.onclick=()=>load(false,true);
  }finally{if(!signal.aborted&&button.isConnected)button.disabled=false;}
 }
 return {render,stop};
}
