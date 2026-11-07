export const isGoalEvent=e=>e.type==='Goal'||e.scoringPlay===true;
export function mountEvents(target,render,prefs,save){
 target.innerHTML='<div class="event-heading"><h3>关键事件</h3><label><input type="checkbox">只看进球</label></div><div class="event-list"></div>';
 const toggle=target.querySelector('input'),list=target.querySelector('.event-list');
 toggle.checked=!!prefs.onlyGoals;
 const show=()=>{list.innerHTML=render(!!prefs.onlyGoals);};
 toggle.onchange=()=>{prefs.onlyGoals=toggle.checked;save();show();};show();
}
