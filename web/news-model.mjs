export function newsPageURL(value){try{const u=new URL(value);return u.protocol==='https:'&&u.hostname==='api.dongqiudi.com'&&!u.port&&!u.username&&/^\/app\/tabs\/web\/\d+\.json$/.test(u.pathname)&&[...u.searchParams.keys()].every(k=>['before','after','page','action','child_tab_id','user_pay_type'].includes(k))?u.href:'';}catch{return '';}}
export function newsImage(value){try{const u=new URL(value);return u.protocol==='https:'&&!u.username&&!u.port&&(u.hostname==='a.espncdn.com'||u.hostname.endsWith('.qunliao.info'))?u.href:'';}catch{return '';}}
export function chineseNews(root,now=Date.now()){
 if(!Array.isArray(root?.articles))throw Error('资讯暂不可用');
 const articles=root.articles.filter(a=>a.category==='足球'&&a.channel==='article'&&!a.is_business_ad&&!a.is_redirect_h5&&/^\d+$/.test(String(a.id))&&typeof a.title==='string').map(a=>{
  const candidates=[Number(a.show_time)*1000,Date.parse(String(a.published_at||'').replace(' ','T')+'+08:00'),Date.parse(String(a.created_at||'').replace(' ','T')+'+08:00')];
  const time=candidates.find(t=>Number.isFinite(t)&&t>946684800000&&t<=now+300000);
  return {id:String(a.id),title:a.title,url:'https://www.dongqiudi.com/article/'+a.id,image:newsImage(a.thumb),time:time?new Date(time).toISOString():'',tag:(a.secondary_category||[]).join(' · '),source:'懂球帝',language:'中文'};
 });
 return {articles:[...new Map(articles.map(a=>[a.id,a])).values()],next:newsPageURL(root.next)};
}
export function englishNews(root){if(!Array.isArray(root?.articles))throw Error('资讯暂不可用');return {articles:root.articles.flatMap(a=>{try{const u=new URL(a.links?.web?.href);if(u.protocol!=='https:'||!(u.hostname==='espn.com'||u.hostname.endsWith('.espn.com'))||u.username||u.port)return [];return [{id:String(a.id||u.href),title:a.headline,url:u.href,image:newsImage(a.images?.[0]?.url),time:a.published,tag:'英文原文',source:'ESPN',language:'English'}];}catch{return [];}}),next:''};}
