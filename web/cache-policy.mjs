const minute=60000,hour=60*minute,day=24*hour;
export function scheduleTTL(entry,url,now=Date.now()){
 const events=entry?.value?.events;if(!Array.isArray(events))return 0;
 if(!events.length){const key=new URL(url).searchParams.get('dates')||'',stamp=Date.parse(key.replace(/^(\d{4})(\d{2})(\d{2})$/,'$1-$2-$3')+'T00:00:00Z');return stamp+day<now?7*day:6*hour;}
 return Math.min(...events.map(e=>{const c=e.competitions?.[0],s=(c?.status||e.status)?.type||{},delta=Date.parse(e.date)-now;
 if(!Number.isFinite(delta))return 2*minute;
 if(s.state==='in')return 30000;
 if(s.state==='post'){if(delta< -day)return 7*day;if(entry.finalSince&&now-entry.finalSince<15*minute)return 2*minute;return hour;}
 if(['STATUS_POSTPONED','STATUS_CANCELED','STATUS_ABANDONED','STATUS_SUSPENDED'].includes(s.name))return 6*hour;
 if(delta<=0)return delta> -6*hour?30000:30*minute;
 if(delta<=30*minute)return 2*minute;
 if(delta<=6*hour)return 15*minute;
 if(delta<=day)return hour;
 return 6*hour;
 }));
}
export function fresh(entry,ttl,now=Date.now()){const age=now-entry?.time;return !!entry&&age>=0&&age<ttl;}
export function allFinal(root){return !!root.events?.length&&root.events.every(e=>(e.competitions?.[0]?.status||e.status)?.type?.state==='post');}
