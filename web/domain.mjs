export const leagues=[['eng.1','英超','47'],['esp.1','西甲','87'],['ita.1','意甲','55'],['ger.1','德甲','54'],['fra.1','法甲','53'],['uefa.champions','欧冠','42'],['usa.1','美职联','130'],['chn.1','中超','120'],['eng.fa','英足总杯','132'],['eng.league_cup','英联赛杯','133'],['ger.dfb_pokal','德国杯','209'],['esp.copa_del_rey','国王杯','138']];
export const dayKey=value=>new Date(new Date(value).getTime()+28800000).toISOString().slice(0,10);
export const shiftDay=(day,n)=>new Date(Date.parse(day+'T00:00:00Z')+n*86400000).toISOString().slice(0,10);
export function move(ids,id,offset){const result=[...ids],i=result.indexOf(id),j=i+Math.sign(offset);if(i>=0&&j>=0&&j<result.length){result.splice(i,1);result.splice(j,0,id);}return result;}
export function select(matches,p={}){return [...new Map(matches.filter(m=>(!p.league||p.league==='all'||p.league===m.league)&&(!p.live||m.state==='in')&&(!p.onlyFavorites||[m.home.id,m.away.id].some(id=>p.favorites.includes(id)))).map(m=>[m.id,m])).values()].sort((a,b)=>a.time-b.time||a.id.localeCompare(b.id));}
export function schedule(root,league){
 if(!Array.isArray(root.events))throw Error('赛程暂不可用');
 return root.events.flatMap(e=>{const c=e.competitions?.[0],h=c?.competitors?.find(x=>x.homeAway==='home'),a=c?.competitors?.find(x=>x.homeAway==='away');if(!h||!a||!e.id||!Number.isFinite(Date.parse(e.date)))return [];
 const st=c.status||e.status||{},type=st.type||{},side=x=>({id:String(x.id||x.team.id),name:x.team.displayName,logo:x.team.logo||`https://a.espncdn.com/i/teamlogos/soccer/500/${x.team.id}.png`,score:String(x.score?.displayValue??x.score?.value??x.score??''),stats:x.statistics||[]});
 const labels={STATUS_POSTPONED:'延期',STATUS_CANCELED:'取消',STATUS_SUSPENDED:'中断',STATUS_ABANDONED:'腰斩',STATUS_HALFTIME:'中场',STATUS_FINAL_PEN:'点球结束',STATUS_FINAL_AET:'加时结束'};
 return [{id:String(e.id),league,time:Date.parse(e.date),home:side(h),away:side(a),state:type.state,status:labels[type.name]||(type.state==='post'?'已结束':type.state==='in'?st.displayClock||'进行中':'未开始'),venue:c.venue?.fullName||'',events:c.details||[],timeValid:c.timeValid!==false}];});
}
export function board(root,id){
 if(String(root.details?.id)!==String(id)||!root.details?.selectedSeason)throw Error('榜单赛事或赛季不匹配');
 const groups=[];function table(data){if(data?.table?.all?.length)groups.push({name:({Eastern:'东区',Western:'西区','Supporters Shield':'总榜（支持者盾）'})[data.leagueName]||data.leagueName,rows:data.table.all});for(const t of data?.tables||[])table(t);}for(const t of root.table||[])table(t.data);
 const stats={};for(const s of root.stats?.players||[]){try{const u=new URL(s.fetchAllUrl);if(['goals','goal_assist'].includes(s.name)&&u.protocol==='https:'&&u.hostname==='data.fotmob.com'&&u.pathname.startsWith('/stats/'+id+'/')&&u.pathname.endsWith('/'+s.name+'.json'))stats[s.name]=u.href;}catch{}}
 return {season:root.details.selectedSeason,groups,stats,fixtures:root.fixtures?.allMatches||[]};
}
export function leaders(root,key){const list=root.TopLists?.find(x=>x.StatName===key);if(!Array.isArray(list?.StatList))throw Error('球员榜单暂不可用');return list.StatList;}
export const normalize=s=>(s||'').replaceAll('ø','o').replaceAll('ß','ss').normalize('NFD').replace(/\p{M}/gu,'').toLowerCase().replace(/[^\p{L}\p{N}]/gu,'');
export function sameFixture(f,m,name){return Math.abs(Date.parse(f.status?.utcTime)-m.time)<=300000&&normalize(name(f.home?.name))===normalize(name(m.home.name))&&normalize(name(f.away?.name))===normalize(name(m.away.name));}
export function roundOf(b,m,name){const f=b.fixtures.filter(f=>sameFixture(f,m,name));if(f.length!==1)return '';const r=f[0].roundName??f[0].round;return /^\d+$/.test(String(r))?'第'+r+'轮':r||'';}
export function squad(root,m){
 if(String(root.header?.id)!==m.id)throw Error('阵容比赛不匹配');
 return (root.rosters||[]).filter(t=>[m.home.id,m.away.id].includes(String(t.team.id))).map(t=>({id:String(t.team.id),name:t.team.displayName,formation:t.formation,players:(t.roster||[]).map(p=>({id:String(p.athlete.id),name:p.athlete.displayName||p.athlete.fullName,number:p.jersey||'',position:p.position?.displayName||p.athlete.position?.displayName||'位置待提供',starter:!!p.starter,in:!!p.subbedIn,out:!!p.subbedOut,red:(p.stats||[]).some(s=>s.name==='redCards'&&Number(s.value??s.displayValue)>0)||(p.plays||[]).some(x=>x.redCard),replaces:String(p.subbedInFor?.athlete?.id||''),stats:p.stats||[],events:p.plays||[]}))}));
}
export function applyRatings(teams,root,m,expectedId,name){
 const g=root.general;if(String(g?.matchId)!==String(expectedId)||String(g.parentLeagueId)!==leagues.find(l=>l[0]===m.league)?.[2]||!sameFixture({home:g.homeTeam,away:g.awayTeam,status:{utcTime:g.matchTimeUTCDate}},m,name))throw Error('评分比赛不匹配');
 for(const t of teams){const side=root.content?.lineup?.[t.id===m.home.id?'homeTeam':'awayTeam'];if(normalize(name(side?.name))!==normalize(name(t.name)))continue;
 for(const p of t.players){const matches=[...side.starters||[],...side.subs||[]].filter(x=>p.number&&String(x.shirtNumber)===String(p.number)&&(normalize(x.name)===normalize(p.name)||(normalize(x.lastName).length>=4&&p.name.split(/[ -]/).some(n=>normalize(n)===normalize(x.lastName)))));if(matches.length!==1)continue;const x=matches[0],r=Number(x.performance?.rating);p.fotmobId=String(x.id);if(r>0&&r<=10)p.rating=r;const pos=x.verticalLayout;if(pos&&Number.isFinite(pos.x)&&Number.isFinite(pos.y)&&pos.x>=0&&pos.x<=1&&pos.y>=0&&pos.y<=1)p.xy=[pos.x,pos.y];}}
}
export function onPitch(p){return (p.starter||p.in)&&!p.out&&!p.red;}
export function pitchPlayers(team,starting=false){const positions=new Map(team.players.filter(p=>p.xy).map(p=>[p.id,p.xy]));function slot(p,seen=new Set()){if(seen.has(p.id))return null;seen.add(p.id);return positions.get(p.id)||slotSafe(team.players.find(q=>q.id===p.replaces),seen);}function slotSafe(p,seen){return p?slot(p,seen):null;}return team.players.filter(p=>starting?p.starter:onPitch(p)).map(p=>({...p,xy:slot(p)})).filter(p=>p.xy);}
