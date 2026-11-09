export const leagues=[['eng.1','英超','47'],['esp.1','西甲','87'],['ita.1','意甲','55'],['ger.1','德甲','54'],['fra.1','法甲','53'],['uefa.champions','欧冠','42'],['usa.1','美职联','130'],['chn.1','中超','120'],['eng.fa','英足总杯','132'],['eng.league_cup','英联赛杯','133'],['ger.dfb_pokal','德国杯','209'],['esp.copa_del_rey','国王杯','138'],['uefa.europa','欧联','73'],['uefa.europa.conf','欧协联','10216'],['fifa.cwc','世俱杯','78']];

export const nationalLeagues=[['fifa.world','世界杯','77'],['uefa.euro','欧洲杯','50'],['afc.asian.cup','亚洲杯','290'],['uefa.nations','欧国联',null],['fifa.worldq.uefa','世预赛欧洲区','10195'],['fifa.worldq.afc','世预赛亚洲区','10197'],['uefa.euroq','欧洲杯预选赛','10607'],['afc.cupq','亚洲杯预选赛',null],['fifa.friendly','国家队友谊赛','114'],['conmebol.america','美洲杯','44'],['concacaf.gold','金杯赛','298'],['concacaf.nations.league','中北美及加勒比海国家联赛','9821'],['fifa.worldq.conmebol','世预赛南美区','10199'],['fifa.worldq.concacaf','世预赛中北美及加勒比海区','10198']];
export const scheduleLeagues=[...leagues,...nationalLeagues];
export const nationalNames={'England':'英格兰','France':'法国','Spain':'西班牙','Germany':'德国','Portugal':'葡萄牙','Italy':'意大利','Netherlands':'荷兰','Belgium':'比利时','Croatia':'克罗地亚','Denmark':'丹麦','Switzerland':'瑞士','Austria':'奥地利','Norway':'挪威','Sweden':'瑞典','Poland':'波兰','Serbia':'塞尔维亚','Türkiye':'土耳其','Turkey':'土耳其','Turkiye':'土耳其','Ukraine':'乌克兰','Scotland':'苏格兰','Wales':'威尔士','Czechia':'捷克','Czech Republic':'捷克','Greece':'希腊','Hungary':'匈牙利','China':'中国','China PR':'中国','Japan':'日本','South Korea':'韩国','Korea Republic':'韩国','Iran':'伊朗','Australia':'澳大利亚','Saudi Arabia':'沙特阿拉伯','Qatar':'卡塔尔','Uzbekistan':'乌兹别克斯坦','Iraq':'伊拉克','United Arab Emirates':'阿联酋','Jordan':'约旦','Oman':'阿曼','Bahrain':'巴林','Thailand':'泰国','Vietnam':'越南','Indonesia':'印度尼西亚'};
Object.assign(nationalNames,{"Argentina": "阿根廷", "Bolivia": "玻利维亚", "Brazil": "巴西", "Canada": "加拿大", "Chile": "智利", "Colombia": "哥伦比亚", "Costa Rica": "哥斯达黎加", "Cuba": "古巴", "Curaçao": "库拉索", "Dominican Republic": "多米尼加共和国", "Ecuador": "厄瓜多尔", "El Salvador": "萨尔瓦多", "Guatemala": "危地马拉", "Haiti": "海地", "Honduras": "洪都拉斯", "Jamaica": "牙买加", "Mexico": "墨西哥", "Nicaragua": "尼加拉瓜", "Panama": "巴拿马", "Paraguay": "巴拉圭", "Peru": "秘鲁", "Suriname": "苏里南", "Trinidad and Tobago": "特立尼达和多巴哥", "United States": "美国", "Uruguay": "乌拉圭", "Venezuela": "委内瑞拉"});
export const isNational=id=>nationalLeagues.some(l=>l[0]===id);
export const enabledLeagues=p=>scheduleLeagues.filter((l,i)=>(p.scheduleScope!=='club'||!isNational(l[0]))&&(p.scheduleScope!=='national'||isNational(l[0]))&&(isNational(l[0])?p.national!==false:p.cups!==false||i<8||i>=12)&&(p.league==='national'?isNational(l[0]):!p.league||p.league==='all'||p.league===l[0]));
export const dayKey=value=>new Date(new Date(value).getTime()+28800000).toISOString().slice(0,10);
export const shiftDay=(day,n)=>new Date(Date.parse(day+'T00:00:00Z')+n*86400000).toISOString().slice(0,10);
export function inScheduleWindow(match,date,now=Date.now()){
 const day=dayKey(match.time);if(day>=date&&day<=shiftDay(date,6))return true;
 if(date!==dayKey(now))return false;
 return (match.state==='post'&&day>=shiftDay(date,-2)&&day<date)||(match.state==='in'&&day>=shiftDay(date,-2)&&day<date);
}
export function move(ids,id,offset){const result=[...ids],i=result.indexOf(id),j=i+Math.sign(offset);if(i>=0&&j>=0&&j<result.length){result.splice(i,1);result.splice(j,0,id);}return result;}
export function select(matches,p={}){return [...new Map(matches.filter(m=>(p.scheduleScope!=='club'||!isNational(m.league))&&(p.scheduleScope!=='national'||isNational(m.league))&&(!isNational(m.league)||p.national!==false)&&(!p.league||p.league==='all'||p.league===m.league||(p.league==='national'&&isNational(m.league)))&&(!p.live||m.state==='in')&&(!p.onlyFavorites||[m.home.id,m.away.id].some(id=>p.favorites.includes(id)))).map(m=>[m.id,m])).values()].sort((a,b)=>a.time-b.time||a.id.localeCompare(b.id));}
export function schedule(root,league){
 if(!Array.isArray(root.events))throw Error('赛程暂不可用');
 return root.events.flatMap(e=>{const c=e.competitions?.[0],h=c?.competitors?.find(x=>x.homeAway==='home'),a=c?.competitors?.find(x=>x.homeAway==='away');if(!h||!a||!e.id||!Number.isFinite(Date.parse(e.date)))return [];
 if(isNational(league)&&![h,a].some(x=>nationalNames[x.team.displayName]))return [];
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
 const g=root.general;if(String(g?.matchId)!==String(expectedId)||String(g.parentLeagueId)!==scheduleLeagues.find(l=>l[0]===m.league)?.[2]||!sameFixture({home:g.homeTeam,away:g.awayTeam,status:{utcTime:g.matchTimeUTCDate}},m,name))throw Error('评分比赛不匹配');
 for(const t of teams){const side=root.content?.lineup?.[t.id===m.home.id?'homeTeam':'awayTeam'];if(normalize(name(side?.name))!==normalize(name(t.name)))continue;
 for(const p of t.players){const matches=[...side.starters||[],...side.subs||[]].filter(x=>p.number&&String(x.shirtNumber)===String(p.number)&&(normalize(x.name)===normalize(p.name)||(normalize(x.lastName).length>=4&&p.name.split(/[ -]/).some(n=>normalize(n)===normalize(x.lastName)))));if(matches.length!==1)continue;const x=matches[0],r=Number(x.performance?.rating);p.fotmobId=String(x.id);if(r>0&&r<=10)p.rating=r;const pos=x.verticalLayout;if(pos&&Number.isFinite(pos.x)&&Number.isFinite(pos.y)&&pos.x>=0&&pos.x<=1&&pos.y>=0&&pos.y<=1)p.xy=[pos.x,pos.y];}}
}
export function onPitch(p){return (p.starter||p.in)&&!p.out&&!p.red;}
export function pitchPlayers(team,starting=false){const positions=new Map(team.players.filter(p=>p.xy).map(p=>[p.id,p.xy]));function slot(p,seen=new Set()){if(seen.has(p.id))return null;seen.add(p.id);return positions.get(p.id)||slotSafe(team.players.find(q=>q.id===p.replaces),seen);}function slotSafe(p,seen){return p?slot(p,seen):null;}return team.players.filter(p=>starting?p.starter:onPitch(p)).map(p=>({...p,xy:slot(p)})).filter(p=>p.xy);}
