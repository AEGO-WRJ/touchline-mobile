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
  if(points.length>8)paths.push({d:'M'+points.join('L')+'Z',length:points.length});
 }return paths.sort((a,b)=>b.length-a.length).slice(0,32).map(p=>p.d);
}
export function startSplash(){
 if(document.querySelector('#startup'))return;
 const cover=document.createElement('section');cover.id='startup';cover.setAttribute('aria-label','看球开屏');
 cover.innerHTML='<button class="startup-skip">跳过</button><div class="startup-center"><div class="startup-art"><svg viewBox="0 0 192 192" aria-hidden="true"><g class="startup-lines"><circle cx="96" cy="96" r="64" pathLength="1"/><path d="M96 59L131 84L118 125H74L61 84Z M96 32V59 M157 76L131 84 M134 147L118 125 M58 147L74 125 M35 76L61 84" pathLength="1"/></g></svg></div><p class="startup-brand">TOUCHLINE</p><h2>看球</h2></div>';
 document.body.append(cover);
 const reduced=matchMedia('(prefers-reduced-motion: reduce)').matches;
 let closed=false;const close=()=>{if(closed)return;closed=true;clearTimeout(timer);cover.remove();};
 const timer=setTimeout(close,reduced?500:2800);cover.querySelector('button').onclick=close;
 let prefs;try{prefs=JSON.parse(localStorage.getItem('preferences')||'{}');}catch{prefs={};}
 const id=prefs?.favorites?.[0];if(!/^\d+$/.test(String(id||'')))return;
 const img=new Image();img.alt='主队队徽';img.className='startup-crest';img.src='crest-'+id+'.png';
 img.onload=()=>{if(closed)return;try{
  const canvas=document.createElement('canvas');canvas.width=canvas.height=192;const ctx=canvas.getContext('2d',{willReadFrequently:true});ctx.drawImage(img,16,16,160,160);
  const paths=outline(ctx.getImageData(0,0,192,192).data,192,192);if(!paths.length)return;
  const group=cover.querySelector('g');group.replaceChildren();for(const d of paths){const path=document.createElementNS('http://www.w3.org/2000/svg','path');path.setAttribute('d',d);path.setAttribute('pathLength','1');group.append(path);}
  cover.querySelector('.startup-art').prepend(img);
 }catch{/* Keep the local football fallback if this crest cannot be decoded. */}};
}
if(typeof window!=='undefined'&&window.Touchline)startSplash();
