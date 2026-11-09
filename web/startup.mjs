function simplify(points){
 if(points.length<3)return points;const a=points[0],b=points.at(-1),dx=b[0]-a[0],dy=b[1]-a[1],length=dx*dx+dy*dy;let far=.6,index=0;
 for(let i=1;i<points.length-1;i++){const p=points[i],t=length?Math.max(0,Math.min(1,((p[0]-a[0])*dx+(p[1]-a[1])*dy)/length)):0,d=(p[0]-a[0]-t*dx)**2+(p[1]-a[1]-t*dy)**2;if(d>far){far=d;index=i;}}
 return index?[...simplify(points.slice(0,index+1)).slice(0,-1),...simplify(points.slice(index))]:[a,b];
}
function smoothPath(points){
 const closed=points[0].join(',')===points.at(-1).join(','),vertices=simplify(points);if(vertices.length<3)return closed?'':'M'+vertices.map(p=>p.join(',')).join('L');if(closed)vertices.pop();
 const mix=(a,b)=>{const length=Math.hypot(b[0]-a[0],b[1]-a[1]),t=length?Math.min(1.5/length,.3):0;return a.map((v,i)=>+(v+(b[i]-v)*t).toFixed(2));};
 const corners=vertices.map((p,i)=>({p,before:mix(p,vertices[(i+vertices.length-1)%vertices.length]),after:mix(p,vertices[(i+1)%vertices.length])}));
 const middle=closed?corners:corners.slice(1,-1);
 return 'M'+(closed?corners.at(-1).after:vertices[0]).join(',')+middle.map(c=>'L'+c.before.join(',')+'Q'+c.p.join(',')+' '+c.after.join(',')).join('')+(closed?'Z':'L'+vertices.at(-1).join(','));
}
// Each shared boundary belongs to one color only, preventing double-colored strokes.
export function colorBoundaries(labels,width,height,palette){
 const edges=[],at=(x,y)=>x<0||y<0||x>=width||y>=height?-1:labels[y*width+x];
 const chroma=i=>i<0?-1:Math.max(...palette[i])-Math.min(...palette[i]);
 const add=(a,b,left,right)=>{if(left===right)return;const owner=left<0?right:right<0?left:chroma(left)>=chroma(right)?left:right;edges.push({a,b,owner});};
 for(let y=0;y<height;y++)for(let x=0;x<width;x++){const c=at(x,y);add([x,y],[x+1,y],at(x,y-1),c);add([x,y],[x,y+1],at(x-1,y),c);if(x===width-1)add([x+1,y],[x+1,y+1],c,-1);if(y===height-1)add([x,y+1],[x+1,y+1],c,-1);}
 return edges;
}
function traceEdges(edges){
 const nodes=new Map(),pending=new Set(edges);for(const e of edges)for(const p of [e.a,e.b]){const key=p.join(',');if(!nodes.has(key))nodes.set(key,[]);nodes.get(key).push(e);}
 const paths=[];
 const walk=(edge,start)=>{let current=start,points=[current];while(edge){pending.delete(edge);current=edge.a.join(',')===current.join(',')?edge.b:edge.a;points.push(current);const links=nodes.get(current.join(','));if(current.join(',')===start.join(',')||links.length!==2)break;edge=links.find(e=>pending.has(e));}if(points.length>8){const d=smoothPath(points);if(d)paths.push({d,length:points.length});}};
 for(const [key,list] of nodes)if(list.length!==2)for(const e of list)if(pending.has(e))walk(e,key.split(',').map(Number));
 while(pending.size){const edge=pending.values().next().value;walk(edge,edge.a);}
 return paths.sort((a,b)=>b.length-a.length).slice(0,64).map(p=>p.d);
}
export function crestLines(data,width,height){
 const bins=new Map();for(let i=0;i<data.length;i+=4){if(data[i+3]<96)continue;const key=[data[i]>>5,data[i+1]>>5,data[i+2]>>5].join(',');const b=bins.get(key)||{n:0,r:0,g:0,b:0};b.n++;b.r+=data[i];b.g+=data[i+1];b.b+=data[i+2];bins.set(key,b);}
 const palette=[];for(const b of [...bins.values()].sort((a,b)=>b.n-a.n)){const c=[b.r/b.n,b.g/b.n,b.b/b.n];if(palette.every(p=>p.reduce((n,v,k)=>n+(v-c[k])**2,0)>3600))palette.push(c);if(palette.length===6)break;}
 if(!palette.length)return [];
 const labels=new Int16Array(width*height).fill(-1);for(let i=0;i<data.length;i+=4){if(data[i+3]<96)continue;let best=0,distance=Infinity;palette.forEach((c,k)=>{const d=c.reduce((n,v,j)=>n+(v-data[i+j])**2,0);if(d<distance){best=k;distance=d;}});labels[i/4]=best;}
 const boundaries=colorBoundaries(labels,width,height,palette);return palette.flatMap((rgb,k)=>{const peak=Math.max(...rgb),color=rgb.map(v=>Math.round(peak<55?180+v*.2:Math.min(255,v*(peak<155?155/peak:1))*.65+255*.35));return traceEdges(boundaries.filter(e=>e.owner===k)).map(d=>({d,color:'rgb('+color.join(',')+')'}));});
}
export function startSplash(){
 if(document.querySelector('#startup'))return;
 const cover=document.createElement('section');cover.id='startup';cover.setAttribute('aria-label','看球开屏');
 cover.innerHTML='<button class="startup-skip">跳过</button><div class="startup-center"><div class="startup-art"><svg viewBox="0 0 192 192" aria-hidden="true"><g class="startup-lines"><circle cx="96" cy="96" r="64" pathLength="1"/><path d="M96 59L131 84L118 125H74L61 84Z M96 32V59 M157 76L131 84 M134 147L118 125 M58 147L74 125 M35 76L61 84" pathLength="1"/></g></svg></div><p class="startup-brand">TOUCHLINE</p><h2>看球</h2></div>';
 document.body.append(cover);
 const reduced=matchMedia('(prefers-reduced-motion: reduce)').matches;
 let closed=false;const close=()=>{if(closed)return;closed=true;clearTimeout(timer);cover.remove();};
 let timer=setTimeout(close,reduced?500:2600);cover.querySelector('button').onclick=close;
 let prefs;try{prefs=JSON.parse(localStorage.getItem('preferences')||'{}');}catch{prefs={};}
 const id=prefs?.favorites?.[0];if(!/^\d+$/.test(String(id||'')))return;
 fetch('catalog.json').then(r=>r.json()).then(c=>{if(closed)return;const team=c.teams.find(t=>String(t.Id)===String(id));if(team){cover.querySelector('h2').textContent=c.names[team.Name]||team.Name;cover.querySelector('.startup-brand').textContent='我的主队';}}).catch(()=>{});
 const img=new Image();img.alt='主队队徽';img.className='startup-crest';img.src='crest-'+id+'.png';
 img.onload=()=>{if(closed)return;try{
  const canvas=document.createElement('canvas');canvas.width=canvas.height=600;const ctx=canvas.getContext('2d',{willReadFrequently:true});ctx.imageSmoothingEnabled=true;ctx.imageSmoothingQuality='high';ctx.drawImage(img,50,50,500,500);
  const paths=crestLines(ctx.getImageData(0,0,600,600).data,600,600);if(!paths.length)return;
  cover.querySelector('svg').setAttribute('viewBox','0 0 600 600');const group=cover.querySelector('g');group.replaceChildren();for(const line of paths){const path=document.createElementNS('http://www.w3.org/2000/svg','path');path.setAttribute('d',line.d);path.setAttribute('pathLength','1');path.setAttribute('vector-effect','non-scaling-stroke');path.style.stroke=line.color;group.append(path);}
  const art=cover.querySelector('.startup-art');art.prepend(img);art.classList.add('has-crest');if(!reduced){clearTimeout(timer);timer=setTimeout(close,2600);}
 }catch{/* Keep the local football fallback if this crest cannot be decoded. */}};
}
if(typeof window!=='undefined'&&window.Touchline)startSplash();
