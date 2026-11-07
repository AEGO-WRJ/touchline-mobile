import {peek,remember} from './transport.mjs';
const day=86400000,key=league=>'rounds:'+league;
export function cachedRound(match){
 const entry=peek(key(match.league))?.value?.[match.id],age=Date.now()-entry?.time;
 return entry&&entry.home===match.home.id&&entry.away===match.away.id&&age>=0&&age<14*day?entry:null;
}
export const needsRound=match=>{const entry=cachedRound(match);return !entry||Date.now()-entry.time>=day;};
export function saveRounds(matches,time){
 if(!Number.isFinite(time)||Date.now()<time)return;
 for(const league of new Set(matches.map(m=>m.league))){
  const entries={...peek(key(league))?.value};
  for(const [id,e] of Object.entries(entries))if(Date.now()-e.time>=14*day||Date.now()<e.time)delete entries[id];
  for(const m of matches.filter(m=>m.league===league&&m.round))entries[m.id]={round:m.round,home:m.home.id,away:m.away.id,time};
  if(Object.keys(entries).length)remember(key(league),entries);
 }
}
