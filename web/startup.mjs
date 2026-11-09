function simplify(points){
 if(points.length<3)return points;const a=points[0],b=points.at(-1),dx=b[0]-a[0],dy=b[1]-a[1],length=dx*dx+dy*dy;let far=.6,index=0;
 for(let i=1;i<points.length-1;i++){const p=points[i],t=length?Math.max(0,Math.min(1,((p[0]-a[0])*dx+(p[1]-a[1])*dy)/length)):0,d=(p[0]-a[0]-t*dx)**2+(p[1]-a[1]-t*dy)**2;if(d>far){far=d;index=i;}}
 return index?[...simplify(points.slice(0,index+1)).slice(0,-1),...simplify(points.slice(index))]:[a,b];
}
function smoothPath(points){
 const vertices=simplify(points);if(vertices.length<4)return '';vertices.pop();
 const mix=(a,b,d)=>{const length=Math.hypot(b[0]-a[0],b[1]-a[1]),t=length?Math.min(d/length,.3):0;return a.map((v,i)=>+(v+(b[i]-v)*t).toFixed(2));};
 const corners=vertices.map((p,i)=>({p,before:mix(p,vertices[(i+vertices.length-1)%vertices.length],1.5),after:mix(p,vertices[(i+1)%vertices.length],1.5)}));
 return 'M'+corners.at(-1).after.join(',')+corners.map(c=>'L'+c.before.join(',')+'Q'+c.p.join(',')+' '+c.after.join(',')).join('')+'Z';
}
// Trace opaque pixel boundaries; disconnected islands and holes remain separate paths.
export function outline(alpha,width,height){
 const edges=new Map(),solid=(x,y)=>x>=0&&y>=0&&x<width&&y<height&&alpha[(y*width+x)*4+3]>48;
 const add=(x,y,a,b)=>{const key=x+','+y;if(!edges.has(key))edges.set(key,[]);edges.get(key).push([a,b]);};
 for(let y=0;y<height;y++)for(let x=0;x<width;x++)if(solid(x,y)){
  if(!solid(x,y-1))add(x,y,x+1,y);if(!solid(x+1,y))add(x+1,y,x+1,y+1);
  if(!solid(x,y+1))add(x+1,y+1,x,y+1);if(!solid(x-1,y))add(x,y+1,x,y);
 }
 const paths=[];while(edges.size){const start=edges.keys().next().value;let key=start,points=[start];
  do{const list=edges.get(key);if(!list?.length)break;const end=list.pop();if(!list.length)edges.delete(key);key=end.join(',');points.push(key);}while(key!==start);
  if(points.length>8)paths.push({d:smoothPath(points.map(p=>p.split(',').map(Number))),length:points.length});
 }return paths.sort((a,b)=>b.length-a.length).slice(0,32).map(p=>p.d).filter(Boolean);
}
export function crestLines(data,width,height){
 const bins=new Map();for(let i=0;i<data.length;i+=4){if(data[i+3]<96)continue;const key=[data[i]>>5,data[i+1]>>5,data[i+2]>>5].join(',');const b=bins.get(key)||{n:0,r:0,g:0,b:0};b.n++;b.r+=data[i];b.g+=data[i+1];b.b+=data[i+2];bins.set(key,b);}
 const palette=[];for(const b of [...bins.values()].sort((a,b)=>b.n-a.n)){const c=[b.r/b.n,b.g/b.n,b.b/b.n];if(palette.every(p=>p.reduce((n,v,k)=>n+(v-c[k])**2,0)>3600))palette.push(c);if(palette.length===6)break;}
 if(!palette.length)return [];
 const masks=palette.map(()=>new Uint8ClampedArray(data.length));for(let i=0;i<data.length;i+=4){if(data[i+3]<96)continue;let best=0,distance=Infinity;palette.forEach((c,k)=>{const d=c.reduce((n,v,j)=>n+(v-data[i+j])**2,0);if(d<distance){best=k;distance=d;}});masks[best][i+3]=255;}
 return palette.flatMap((rgb,k)=>{const peak=Math.max(...rgb),color=rgb.map(v=>Math.round(peak<55?180+v*.2:Math.min(255,v*(peak<155?155/peak:1))*.65+255*.35));return outline(masks[k],width,height).map(d=>({d,color:'rgb('+color.join(',')+')'}));});
}
export function startSplash(){
 if(document.querySelector('#startup'))return;
 const cover=document.createElement('section');cover.id='startup';cover.setAttribute('aria-label','看球开屏');
 cover.innerHTML='<button class="startup-skip">跳过</button><div class="startup-center"><div class="startup-art"><svg viewBox="0 0 192 192" aria-hidden="true"><g class="startup-lines"><circle cx="96" cy="96" r="64" pathLength="1"/><path d="M96 59L131 84L118 125H74L61 84Z M96 32V59 M157 76L131 84 M134 147L118 125 M58 147L74 125 M35 76L61 84" pathLength="1"/></g></svg></div><p class="startup-brand">TOUCHLINE</p><h2>看球</h2></div>';
 document.body.append(cover);
 const reduced=matchMedia('(prefers-reduced-motion: reduce)').matches;
 let closed=false;const close=()=>{if(closed)return;closed=true;clearTimeout(timer);cover.remove();};
 const timer=setTimeout(close,reduced?500:3400);cover.querySelector('button').onclick=close;
 let prefs;try{prefs=JSON.parse(localStorage.getItem('preferences')||'{}');}catch{prefs={};}
 const id=prefs?.favorites?.[0];if(!/^\d+$/.test(String(id||'')))return;
 fetch('catalog.json').then(r=>r.json()).then(c=>{if(closed)return;const team=c.teams.find(t=>String(t.Id)===String(id));if(team){cover.querySelector('h2').textContent=c.names[team.Name]||team.Name;cover.querySelector('.startup-brand').textContent='我的主队';}}).catch(()=>{});
 const img=new Image();img.alt='主队队徽';img.className='startup-crest';img.src='crest-'+id+'.png';
 img.onload=()=>{if(closed)return;try{
  const canvas=document.createElement('canvas');canvas.width=canvas.height=600;const ctx=canvas.getContext('2d',{willReadFrequently:true});ctx.imageSmoothingEnabled=true;ctx.imageSmoothingQuality='high';ctx.drawImage(img,50,50,500,500);
  const paths=crestLines(ctx.getImageData(0,0,600,600).data,600,600);if(!paths.length)return;
  cover.querySelector('svg').setAttribute('viewBox','0 0 600 600');const group=cover.querySelector('g');group.replaceChildren();for(const [i,line] of paths.entries()){const path=document.createElementNS('http://www.w3.org/2000/svg','path');path.setAttribute('d',line.d);path.setAttribute('pathLength','1');path.setAttribute('vector-effect','non-scaling-stroke');path.style.stroke=line.color;path.style.animationDelay=(i%5)*.065+'s';group.append(path);}
  cover.querySelector('.startup-art').prepend(img);
 }catch{/* Keep the local football fallback if this crest cannot be decoded. */}};
}
if(typeof window!=='undefined'&&window.Touchline)startSplash();
