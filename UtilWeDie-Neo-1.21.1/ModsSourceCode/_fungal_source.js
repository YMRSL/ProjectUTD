function _err(npc,where,e){try{npc.say('[ERR '+where+'] '+e);}catch(x){}}
function distSq2(npc,o){
 var op=o.getPos();var p=npc.getPos();
 var dx=op.getX()-p.getX();var dy=op.getY()-p.getY();var dz=op.getZ()-p.getZ();
 return dx*dx+dy*dy+dz*dz;
}
function isHighInfection(o){
 try{return o.getType()==1&&o.getMCEntity().getTags().contains('sona_no_fungal_aggro');}catch(e){return false;}
}
function tick(event){try{
 var npc=event.npc;var w=npc.getWorld();
 var sd=npc.getStoreddata();var td=npc.getTempdata();
 var pos=npc.getPos();var now=w.getTotalTime();

 // self-tag so the gunshot-lure broadcast can find this npc
 if(!sd.has('lureTag')){try{npc.getMCEntity().addTag('sona_lure_listener');}catch(te){}sd.put('lureTag',1);}

 // record spawn point
 if(!(sd.has('sX')&&sd.has('sY')&&sd.has('sZ'))){
  sd.put('sX',Math.floor(pos.getX()));sd.put('sY',Math.floor(pos.getY()));sd.put('sZ',Math.floor(pos.getZ()));
 }

 // ambient sound (100t)
 try{
  var lastIdle=td.has('lastIdle')?td.get('lastIdle'):-99999;
  if((now-lastIdle)>=100){td.put('lastIdle',now);w.playSoundAt(pos,'minecraft:entity.zombie.ambient',1.0,1.0);}
 }catch(se){_err(npc,'idle',se);}

 // 原生 CNPC 索敌独家负责仇恨(阵营+AggroRange+视线+转身/追击)。脚本只读取、只反应,从不 setAttackTarget。
 var tgt=npc.getAttackTarget();
 if(tgt!=null&&!tgt.isAlive())tgt=null;

 // combat
 if(tgt!=null&&tgt.isAlive()){
  if(tgt.getType()==1){
   if(isHighInfection(tgt)){
    // 初见高感染玩家:一次性「辨认」定身(强力缓慢 ~7秒),过后正常追逐
    var tid='';try{tid=''+tgt.getUUID();}catch(ue){tid='';}
    if((td.has('recogT')?td.get('recogT'):'')!=tid){
     td.put('recogT',tid);td.put('recogUntil',now+140);
     try{npc.addPotionEffect(1,7,5,false);}catch(sl){_err(npc,'slow',sl);}
    }
    if(now<(td.has('recogUntil')?td.get('recogUntil'):0)){
     try{clearDig(npc,td);}catch(c2){}
     return; // 辨认中:被缓慢钉住,不追不跳
    }
    // 辨认结束 -> 落入下方正常追逐(挖墙+跃扑)
   }
   try{digWall(npc,w,td,tgt,now,pos);}catch(de){_err(npc,'dig',de);}
   // wolf-like leap toward target (script reproduction of native CanLeap; auto-off during freeze)
   try{
    var lj=td.has('lastLeap')?td.get('lastLeap'):-999;
    var onG=true;try{onG=npc.getMCEntity().onGround();}catch(og){onG=true;}
    if(onG&&(now-lj)>=120){
     var ldx=tgt.getX()-npc.getX();var ldz=tgt.getZ()-npc.getZ();var lL=Math.sqrt(ldx*ldx+ldz*ldz)||0.001;
     if(lL>2){td.put('lastLeap',now);npc.setMotionX(ldx/lL*1.5);npc.setMotionZ(ldz/lL*1.5);npc.setMotionY(0.32);}
    }
   }catch(lpe){}
  }else{
   try{clearDig(npc,td);}catch(cd){}
  }
  return;
 }

 // 无目标:清掉辨认记录,下次重新仇恨到高感染玩家再触发一次定身
 if(td.has('recogT')&&td.get('recogT')!=''){td.put('recogT','');}

 // gunshot lure: when no target, pathfind to the broadcast point (default targeting already had priority above)
 try{
  var pd=npc.getMCEntity().getPersistentData();
  if(pd.contains('SonaLureUntil')&&now<pd.getLong('SonaLureUntil')){
   var lastL=td.has('lureAt')?td.get('lureAt'):-999;
   if((now-lastL)>=20||!npc.isNavigating()){
    td.put('lureAt',now);
    npc.navigateTo(pd.getDouble('SonaLureX'),pd.getDouble('SonaLureY'),pd.getDouble('SonaLureZ'),1.0);
   }
   return;
  }
 }catch(lue){}

 // throttled wander / cluster
 var last=td.has('patAt')?td.get('patAt'):-9999;
 if((now-last)<60){return;}
 td.put('patAt',now);

 var myName=npc.getName();var myId=npc.getUUID();
 var aliveCount=1;
 var near=w.getNearbyEntities(pos,48,2);
 var leader=npc;var leaderId=myId;
 for(var j=0;j<near.length;j++){var n=near[j];
  if(n.getType()!=2)continue;
  if(n.getName()!=myName)continue;
  if(!n.isAlive())continue;
  var nd=n.getPos();
  var ndx=nd.getX()-pos.getX();var ndz=nd.getZ()-pos.getZ();
  var nd2=ndx*ndx+ndz*ndz;
  if(nd2<=(32*32))aliveCount++;
  var nid=n.getUUID();
  if(nid<leaderId){leader=n;leaderId=nid;}
 }
 td.put('kin',aliveCount);

 if(aliveCount<5){
  var sx0=sd.get('sX');var sy0=sd.get('sY');var sz0=sd.get('sZ');
  if(!npc.isNavigating()){
   var hx=sx0-pos.getX();var hz=sz0-pos.getZ();
   if((hx*hx+hz*hz)>(20*20)){
    npc.navigateTo(sx0,sy0,sz0,0.9);
   }else{
    var aa=Math.random()*Math.PI*2;var rr=4+Math.random()*6;
    npc.navigateTo(Math.floor(sx0+Math.cos(aa)*rr),sy0,Math.floor(sz0+Math.sin(aa)*rr),0.9);
   }
  }
  return;
 }

 if(leaderId==myId){
  var sx=sd.get('sX');var sy=sd.get('sY');var sz=sd.get('sZ');
  var hasPt=sd.has('pX')&&sd.has('pY')&&sd.has('pZ');
  var lastPick=sd.has('pAt')?sd.get('pAt'):-99999;
  var redo=!hasPt;
  if(hasPt){
   var tx=sd.get('pX');var tz=sd.get('pZ');
   var ex=tx-pos.getX();var ez=tz-pos.getZ();
   if((ex*ex+ez*ez)<=4)redo=true;
   if((now-lastPick)>=200)redo=true;
   if(!npc.isNavigating())redo=true;
  }
  if(redo){
   var dist=18+Math.random()*10;
   var hdx=sx-pos.getX();var hdz=sz-pos.getZ();
   var distHome=Math.sqrt(hdx*hdx+hdz*hdz);
   var ang;
   if(distHome>40){ang=Math.atan2(hdz,hdx);}else{ang=Math.random()*Math.PI*2;}
   var nx=Math.floor(pos.getX()+Math.cos(ang)*dist);
   var nz=Math.floor(pos.getZ()+Math.sin(ang)*dist);
   var ny=Math.floor(pos.getY());
   sd.put('pX',nx);sd.put('pY',ny);sd.put('pZ',nz);sd.put('pAt',now);
   npc.navigateTo(nx,ny,nz,1.0);
  }else if(!npc.isNavigating()){
   npc.navigateTo(sd.get('pX'),sd.get('pY'),sd.get('pZ'),1.0);
  }
 }else{
  var lsd=leader.getStoreddata();
  if(lsd.has('pX')&&lsd.has('pY')&&lsd.has('pZ')){
   var lx=lsd.get('pX');var ly=lsd.get('pY');var lz=lsd.get('pZ');
   var cx=lx-pos.getX();var cz=lz-pos.getZ();
   if((cx*cx+cz*cz)>4&&!npc.isNavigating()){
    npc.navigateTo(lx,ly,lz,1.0);
   }
  }else if(!npc.isNavigating()){
   npc.navigateTo(sd.get('sX'),sd.get('sY'),sd.get('sZ'),0.9);
  }
 }
}catch(e){_err(event.npc,'tick',e);}}

function crackProgress(npc,bx,by,bz,prog){
 try{
  var mc=npc.getMCEntity();
  var lvl=mc.level();
  var bp=npc.getWorld().getMCBlockPos(bx,by,bz);
  lvl.destroyBlockProgress(mc.getId(),bp,prog);
 }catch(ce){}
}
function digWall(npc,w,td,tgt,now,pos){
 var px=pos.getX();var py=pos.getY();var pz=pos.getZ();
 var lpx=td.has('lpx')?td.get('lpx'):px;
 var lpz=td.has('lpz')?td.get('lpz'):pz;
 var moved=(px-lpx)*(px-lpx)+(pz-lpz)*(pz-lpz);
 td.put('lpx',px);td.put('lpz',pz);
 var stuckN=td.has('stuckN')?td.get('stuckN'):0;
 var nav=false;try{nav=npc.isNavigating();}catch(ie){nav=false;}
 if(moved<0.0025){stuckN++;}else{stuckN=0;}
 td.put('stuckN',stuckN);

 var dy=tgt.getY()-npc.getY();
 var dx=tgt.getX()-npc.getX();var dz=tgt.getZ()-npc.getZ();
 var L=Math.sqrt(dx*dx+dz*dz);
 var fx,fz;
 if(L<0.5){
  var dyaw=npc.getRotation()*Math.PI/180.0;
  fx=-Math.sin(dyaw);fz=Math.cos(dyaw);
 }else{ fx=dx/L; fz=dz/L; }
 var bx=Math.floor(npc.getX()+fx*0.6);var bz=Math.floor(npc.getZ()+fz*0.6);
 var footY=Math.floor(py);

 var blockedAhead=!isAir(w,bx,footY,bz)||!isAir(w,bx,footY+1,bz);
 if(!(nav&&stuckN>=3)&&!blockedAhead){
  if(td.has('digKey')){clearDig(npc,td);}
  return;
 }

 var seq=[[bx,footY,bz],[bx,footY+1,bz]];
 if(dy>1.0){ seq.push([bx,footY+2,bz]); }
 if(dy<-1.0){ seq.push([bx,footY-1,bz]); seq.push([Math.floor(npc.getX()),footY-1,Math.floor(npc.getZ())]); }
 var step=td.has('brkStep')?td.get('brkStep'):0;

 var cur=null;
 for(var s=0;s<seq.length;s++){
  var idx=(step+s)%seq.length;
  var c=seq[idx];
  if(!isAir(w,c[0],c[1],c[2])&&!blacklisted(w,c[0],c[1],c[2])){
   cur=c;step=idx;break;
  }
 }
 if(cur==null){
  if(td.has('digKey')){clearDig(npc,td);}
  td.put('brkStep',(step+1)%seq.length);
  return;
 }
 td.put('brkStep',step);

 var kin=td.has('kin')?td.get('kin'):1;
 var level=1+Math.floor(kin/5);
 var baseDigTime=20-(level-1)*4;if(baseDigTime<6)baseDigTime=6;
 var hf=hardnessFactor(w,cur[0],cur[1],cur[2]);
 var digTime=Math.round(baseDigTime*hf);
 if(digTime<4)digTime=4;if(digTime>200)digTime=200;
 var key=cur[0]+','+cur[1]+','+cur[2];
 var dkey=td.has('digKey')?td.get('digKey'):'';
 if(dkey!=key){
  if(dkey!=''){var op=dkey.split(',');crackProgress(npc,parseInt(op[0]),parseInt(op[1]),parseInt(op[2]),-1);}
  td.put('digKey',key);
  td.put('digTick',0);
 }
 var digTick=(td.has('digTick')?td.get('digTick'):0)+1;
 td.put('digTick',digTick);

 var lastDigAnim=td.has('lastDigAnim')?td.get('lastDigAnim'):-99999;
 if((now-lastDigAnim)>=16){td.put('lastDigAnim',now);try{npc.playGeckoAnim('attack');}catch(pae){}}

 var hitInterval=Math.round(digTime/3);if(hitInterval<2)hitInterval=2;if(hitInterval>10)hitInterval=10;
 var refDigTime=baseDigTime;
 var lastHitSnd=td.has('lastHitSnd')?td.get('lastHitSnd'):-99999;
 if((now-lastHitSnd)>=hitInterval){
  td.put('lastHitSnd',now);
  var hp=3.0*Math.pow(refDigTime/digTime,0.4);if(hp<2.25)hp=2.25;if(hp>4.0)hp=4.0;
  try{w.playSoundAt(npc.getPos(),'minecraft:block.gravel.hit',1.25,hp);}catch(hse){}
 }
 var prog=Math.floor(digTick/digTime*10);if(prog>9)prog=9;
 crackProgress(npc,cur[0],cur[1],cur[2],prog);

 if(digTick>=digTime){
  w.removeBlock(cur[0],cur[1],cur[2]);
  var bp=Math.pow(refDigTime/digTime,0.25);if(bp<1.0)bp=1.0;if(bp>1.2)bp=1.2;
  try{w.playSoundAt(npc.getPos(),'minecraft:block.gravel.break',1.25,bp);}catch(bse){}
  crackProgress(npc,cur[0],cur[1],cur[2],-1);
  td.put('digKey','');
  td.put('digTick',0);
  td.put('brkStep',(step+1)%seq.length);
 }
}
function clearDig(npc,td){
 var dkey=td.get('digKey');
 if(dkey!=null&&dkey!=''){var op=dkey.split(',');crackProgress(npc,parseInt(op[0]),parseInt(op[1]),parseInt(op[2]),-1);}
 td.put('digKey','');
 td.put('digTick',0);
}
function blockHardness(w,bx,by,bz){
 try{
  var b=w.getBlock(bx,by,bz);if(b==null)return 1.5;
  var st=b.getMCBlockState();if(st==null)return 1.5;
  var h=-1.0;
  try{var lvl=w.getMCLevel();var bp=w.getMCBlockPos(bx,by,bz);h=st.getDestroySpeed(lvl,bp);}catch(e1){h=-1.0;}
  if(h<0.0){try{h=st.getBlock().defaultDestroyTime();}catch(e2){h=-1.0;}}
  if(h<0.0)return 1.5;
  return h;
 }catch(he){return 1.5;}
}
function hardnessFactor(w,bx,by,bz){
 var h=blockHardness(w,bx,by,bz);
 var f=h/1.5;if(f<0.4)f=0.4;if(f>6.0)f=6.0;return f;
}
function isAir(w,x,y,z){
 try{var b=w.getBlock(x,y,z);if(b==null)return true;return b.isAir();}catch(ae){return false;}
}
function blacklisted(w,x,y,z){
 try{var b=w.getBlock(x,y,z);if(b==null)return true;var nm=b.getName();
  if(nm==null)return false;
  if(nm.indexOf('bedrock')>=0)return true;
  if(nm.indexOf('water')>=0)return true;
  if(nm.indexOf('lava')>=0)return true;
  return false;
 }catch(be){return true;}
}

function meleeAttack(event){try{
 var npc=event.npc;
 var t=event.target;
 if(t==null)t=npc.getAttackTarget();
 if(t==null)return;
 if(t.getType()==1){t.addPotionEffect(1,2,2,false);}
 var dx=t.getX()-npc.getX();var dz=t.getZ()-npc.getZ();
 var L=Math.sqrt(dx*dx+dz*dz)||0.001;
 npc.setMotionX(dx/L*0.55);
 npc.setMotionZ(dz/L*0.55);
 npc.setMotionY(0.32);
}catch(e){_err(event.npc,'melee',e);}}
function damaged(event){try{
 var npc=event.npc;
 npc.getWorld().playSoundAt(npc.getPos(),'minecraft:entity.zombie.hurt',1.0,1.0);
 // 被攻击的反击交给原生 OnAttack(还击),脚本不再 setAttackTarget。
}catch(e){_err(event.npc,'damaged',e);}}
function died(event){try{
 var npc=event.npc;
 npc.getWorld().playSoundAt(npc.getPos(),'minecraft:entity.zombie.death',1.0,1.0);
}catch(e){_err(event.npc,'died',e);}}