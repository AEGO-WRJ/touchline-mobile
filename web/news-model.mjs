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

export const cctvURL="https://api.cntv.cn/newList/getMixListByPageId?serviceId=pcsports&id=PAGE0m5hVY1TBruw6MJoS6dj240409,PAGE5ZYxKvZ5yc6Y2ozLkJzr240519,PAGEwKVeW6pLCacN8fwIcnJl210618,PAGEOUdtqIWHx7noLVJBiuaG190703,PAGE4KtzvrrKPSa3A3ae3HjT190703,PAGEbu1GfLnhIWd1brwzFIKE190703,PAGEb22qsLHxmytT4JQtnI6M190703,PAGEJ2VOxAqvorBpCDjxRI0u190703,PAGESYCXAf0h82W51wqlWzLM190703,PAGEmkxkuHDkV3wotUIZVSQV190703,PAGEYKiddAFHw4DLdlGZ9FUv190703,PAGECvsOaSNZCh1HamyAtrep190703,PAGEYWG7DKjU9HCy6446bM9Z190703,PAGEBXz3M0bz9QkgjY6stZrf190703,PAGE46NGJPLUa63y5nQMr1OH230906&p=1&n=100&catg=0,1,2&t=json";
export function cctvNews(root,address=cctvURL){
 if(!Array.isArray(root?.data?.list))throw Error('央视资讯暂不可用');
 const articles=root.data.list.flatMap(a=>{try{const u=new URL(a.url);if(u.protocol!=='https:'||u.hostname!=='sports.cctv.com'||u.port||u.username||!/^\/\d{4}\/\d{2}\/\d{2}\/ARTI[a-zA-Z0-9]+\.shtml$/.test(u.pathname)||typeof a.title!=='string')return [];let image='';try{const img=new URL(a.image);if(img.protocol==='https:'&&img.hostname.endsWith('.img.cctvpic.com')&&!img.port&&!img.username)image=img.href;}catch{}const time=Number(a.focus_date);return [{id:String(a.id||u.href),title:a.title,url:u.href,image,time:Number.isFinite(time)&&time>0?new Date(time).toISOString():'',tag:'足球报道',source:'央视网',language:'中文'}];}catch{return [];}});
 const next=new URL(cctvURL);next.searchParams.set('p',String(Number(new URL(address).searchParams.get('p')||1)+1));
 return {articles:[...new Map(articles.map(a=>[a.id,a])).values()],next:root.data.list.length>=100?next.href:''};
}
