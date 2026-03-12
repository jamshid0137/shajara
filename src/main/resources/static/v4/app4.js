'use strict';
/* ================================================================
   app4.js — Balkan-ga o'xshash konfiguratsiya + Layout
   levelSeparation, siblingSeparation, subtreeSeparation,
   minPartnerSeparation, subLevels — hammasi sozlanadi
================================================================ */

/* ── Balkan-ga o'xshash config ── */
var CFG = {
  levelSeparation:    60,   /* darajalar orasidagi bo'shliq */
  siblingSeparation:  10,   /* aka-ukalar orasidagi bo'shliq */
  subtreeSeparation:  10,   /* subtree orasidagi bo'shliq */
  minPartnerSeparation: 10, /* juft bilan node orasidagi bo'shliq */
  partnerNodeSeparation: 5, /* juft nodelar orasidagi bo'shliq */
  subLevels: 999,           /* ko'rsatiladigan ota-ona darajalari soni */
  padding: 30
};

/* Node o'lchamlari */
var NW=200,NH=90,PR=26;
/* Hisoblangan o'lchamlar */
var HGAP=function(){return CFG.siblingSeparation+CFG.partnerNodeSeparation;};
var VGAP=function(){return CFG.levelSeparation;};

var _persons=[],_relations=[],_nodeMap={},_childMap={},_spouseMap={};
var _collapsed={},_subLevels={}; /* _subLevels[id] = ko'rsatiladigan ota-ona daraja soni */
var _positions={},_gen={};
var _token='',_activeId=null,_editId=null,_childPendingId=null;
var _tx=0,_ty=0,_scale=1,_drag=false,_dx=0,_dy=0,_dtx=0,_dty=0;

/* ── INIT ── */
window.addEventListener('load',function(){
  showLoad(false);
  var p=new URLSearchParams(location.search);
  var tok=p.get('token'),tid=p.get('treeId')||p.get('id');
  if(tok)document.getElementById('tokenInput').value=tok;
  if(tid)document.getElementById('treeIdInput').value=tid;
  initEvents();
  if(tok&&tid)loadTree();
  document.getElementById('searchInput').addEventListener('input',function(){hlSearch(this.value.trim());});
});

/* ── LOAD ── */
function loadTree(){
  _token=document.getElementById('tokenInput').value.trim();
  var tid=document.getElementById('treeIdInput').value.trim();
  if(!_token||!tid){showErr('Token va Tree ID kiriting!');return;}
  showLoad(true);hideErr();
  _collapsed={};_subLevels={};
  Promise.all([
    apiFetch('GET','/api/persons/tree/'+tid,null),
    apiFetch('GET','/api/relations/tree/'+tid,null)
  ]).then(function(r){
    showLoad(false);
    _persons=r[0]||[];_relations=r[1]||[];
    buildMaps();
    _gen=computeGen();
    equalizeSpouseGen();
    _positions=computeLayout();
    renderAll();fitView();updateStats();
  }).catch(function(e){showLoad(false);showErr(e.message);});
}
function reloadTree(){_collapsed={};_subLevels={};loadTree();}

/* ── MAPS ── */
function buildMaps(){
  _nodeMap={};_childMap={};_spouseMap={};
  _persons.forEach(function(p){_nodeMap[p.id]=p;_childMap[p.id]=[];_spouseMap[p.id]=[];});
  _persons.forEach(function(p){
    [p.fatherId,p.motherId].forEach(function(pid){
      if(pid&&_nodeMap[pid]&&_childMap[pid].indexOf(p.id)<0)_childMap[pid].push(p.id);
    });
  });
  _relations.forEach(function(r){
    if((r.type||'').toUpperCase()!=='SPOUSE')return;
    var a=r.fromPersonId,b=r.toPersonId;
    if(!_nodeMap[a]||!_nodeMap[b])return;
    if(_spouseMap[a].indexOf(b)<0)_spouseMap[a].push(b);
    if(_spouseMap[b].indexOf(a)<0)_spouseMap[b].push(a);
  });
}

/* ── GENERATION (iteratif) ── */
function computeGen(){
  var g={};_persons.forEach(function(p){g[p.id]=0;});
  var changed=true,iter=0;
  while(changed&&iter++<200){
    changed=false;
    _persons.forEach(function(p){
      var v=0;
      if(p.fatherId&&_nodeMap[p.fatherId])v=Math.max(v,(g[p.fatherId]||0)+1);
      if(p.motherId&&_nodeMap[p.motherId])v=Math.max(v,(g[p.motherId]||0)+1);
      if(g[p.id]!==v){g[p.id]=v;changed=true;}
    });
  }
  return g;
}
function equalizeSpouseGen(){
  var changed=true;
  while(changed){
    changed=false;
    _relations.forEach(function(r){
      if((r.type||'').toUpperCase()!=='SPOUSE')return;
      var a=r.fromPersonId,b=r.toPersonId;
      if(!_nodeMap[a]||!_nodeMap[b])return;
      var mx=Math.max(_gen[a]||0,_gen[b]||0);
      if(_gen[a]!==mx||_gen[b]!==mx){_gen[a]=mx;_gen[b]=mx;changed=true;}
    });
  }
}

/* ── LAYOUT (Recursive Subtree — bolalar aralashmaydi) ── */
function computeLayout(){
  if(!_persons.length)return{};
  var vgap=VGAP();
  var pos={};

  /* 1. Rootlarni topish — ota-onasi yo'q yoki ota-onasi tree ichida yo'q */
  var roots=_persons.filter(function(p){
    return !(p.fatherId&&_nodeMap[p.fatherId]) && !(p.motherId&&_nodeMap[p.motherId]);
  });
  /* rootlar ichida spouse juftlarni birlashtirish */
  var rootGroups=buildFamilyGroups(roots.map(function(r){return r.id;}));

  /* 2. Har bir subtree kengligini hisoblash (rekursiv) */
  var _widthCache={};
  function subtreeWidth(id){
    if(_widthCache[id]!==undefined) return _widthCache[id];
    var children=getUniqueChildren(id);
    if(!children.length){
      /* Spouse bilan birgalikda kenglik */
      var sw=familyUnitWidth(id);
      _widthCache[id]=sw;
      return sw;
    }
    /* Bolalar subtreelari kengligi yig'indisi */
    var childrenTotal=0;
    children.forEach(function(cid,i){
      childrenTotal+=subtreeWidth(cid);
      if(i<children.length-1) childrenTotal+=CFG.subtreeSeparation;
    });
    var ownW=familyUnitWidth(id);
    var w=Math.max(ownW,childrenTotal);
    _widthCache[id]=w;
    return w;
  }

  /* Oila birligi kengligi: node + spouse(lar) */
  function familyUnitWidth(id){
    var grpSize=1;
    (_spouseMap[id]||[]).forEach(function(s){if(_nodeMap[s])grpSize++;});
    return grpSize*NW+(grpSize-1)*CFG.minPartnerSeparation;
  }

  /* Uniqu bolalar — faqat bir marta (ota va onaning umumiy bolalarini takrorlamaslik) */
  function getUniqueChildren(id){
    var childSet={};
    (_childMap[id]||[]).forEach(function(cid){childSet[cid]=true;});
    /* spouse ning ham bolalarini qo'sh (lekin takror emas) */
    (_spouseMap[id]||[]).forEach(function(sid){
      (_childMap[sid]||[]).forEach(function(cid){childSet[cid]=true;});
    });
    return Object.keys(childSet).map(Number).filter(function(cid){return _nodeMap[cid];});
  }

  /* 3. Rekursiv joylash */
  function placeSubtree(id,left,y,placedSpouse){
    var gen=_gen[id]||0;
    var cy=gen*(NH+vgap);
    var children=getUniqueChildren(id);
    var stW=subtreeWidth(id);
    var unitW=familyUnitWidth(id);

    /* Agar bolalar bo'lmasa — faqat node (va spouse) */
    if(!children.length){
      pos[id]={x:left+(stW-unitW)/2, y:cy};
      placeSpousesOf(id,pos[id].x,cy,placedSpouse);
      return;
    }

    /* Bolalarni chapdan o'ngga ketma-ket joylash */
    var childrenTotalW=0;
    children.forEach(function(cid,i){
      childrenTotalW+=subtreeWidth(cid);
      if(i<children.length-1) childrenTotalW+=CFG.subtreeSeparation;
    });

    var childStartX=left+(stW-childrenTotalW)/2;
    var cx=childStartX;
    var placed2={};
    children.forEach(function(cid,i){
      if(placed2[cid])return;
      placed2[cid]=true;
      placeSubtree(cid,cx,cy+NH+vgap,placed2);
      cx+=subtreeWidth(cid)+CFG.subtreeSeparation;
    });

    /* Ota-onani bolalar markazi ustiga */
    var cxs=children.filter(function(c){return pos[c];}).map(function(c){return pos[c].x;});
    var centerX;
    if(cxs.length){
      centerX=(Math.min.apply(null,cxs)+Math.max.apply(null,cxs))/2;
    } else {
      centerX=left+(stW-NW)/2;
    }
    pos[id]={x:centerX, y:cy};
    placeSpousesOf(id,centerX,cy,placedSpouse);
  }

  /* Spouse larni yoniga joylashtir */
  function placeSpousesOf(id,x,y,placedSpouse){
    var spouses=(_spouseMap[id]||[]).filter(function(s){return _nodeMap[s]&&!placedSpouse[s];});
    var sx=x+NW+CFG.minPartnerSeparation;
    spouses.forEach(function(sid){
      placedSpouse[sid]=true;
      pos[sid]={x:sx, y:y};
      sx+=NW+CFG.minPartnerSeparation;
    });
  }

  /* Family Groups — spouse larni birgalikda root sifatida ishlov berish */
  function buildFamilyGroups(ids){
    var groups=[],placed={};
    ids.forEach(function(id){
      if(placed[id])return;
      placed[id]=true;
      var grp=[id];
      (_spouseMap[id]||[]).forEach(function(s){
        if(!placed[s]&&ids.indexOf(s)>=0){placed[s]=true;grp.push(s);}
      });
      groups.push(grp);
    });
    return groups;
  }

  /* 4. Root guruhlarini chapdan o'ngga joylash */
  var globalX=CFG.padding;
  var placedRootSpouse={};
  rootGroups.forEach(function(grp,gi){
    /* Guruhning eng keng subtree sini hisoblash */
    var groupW=0;
    grp.forEach(function(rid){
      groupW=Math.max(groupW,subtreeWidth(rid));
    });
    /* Har bir root ni joylash */
    grp.forEach(function(rid){
      if(placedRootSpouse[rid])return;
      placedRootSpouse[rid]=true;
      placeSubtree(rid,globalX,0,placedRootSpouse);
    });
    globalX+=groupW+CFG.subtreeSeparation*2;
  });

  /* 5. padding ga siljitish */
  var allX=_persons.map(function(p){return pos[p.id]?pos[p.id].x:0;});
  var allY=_persons.map(function(p){return pos[p.id]?pos[p.id].y:0;});
  var ox=Math.min.apply(null,allX)-CFG.padding,oy=Math.min.apply(null,allY)-CFG.padding;
  _persons.forEach(function(p){if(pos[p.id]){pos[p.id].x-=ox;pos[p.id].y-=oy;}});
  return pos;
}
function avgPX(id,pos){
  var p=_nodeMap[id];if(!p)return 0;
  var xs=[];
  if(p.fatherId&&pos[p.fatherId])xs.push(pos[p.fatherId].x);
  if(p.motherId&&pos[p.motherId])xs.push(pos[p.motherId].x);
  return xs.length?xs.reduce(function(s,x){return s+x;},0)/xs.length:-9999;
}
function withSpouses(ids){
  var res=[],pl={};
  ids.forEach(function(id){
    if(pl[id])return;pl[id]=true;res.push(id);
    (_spouseMap[id]||[]).forEach(function(s){if(!pl[s]&&ids.indexOf(s)>=0){pl[s]=true;res.push(s);}});
  });return res;
}
/* spousePairs: id → juft id listi */
function getSpousePairs(ids){
  var pairs={};/* id → partnerIds (shu daraja ichida) */
  ids.forEach(function(id){
    (_spouseMap[id]||[]).forEach(function(s){
      if(ids.indexOf(s)>=0){
        if(!pairs[id])pairs[id]=[];
        if(pairs[id].indexOf(s)<0)pairs[id].push(s);
      }
    });
  });
  return pairs;
}
function fixOverlap(ids,pos,step){
  /* Juftlarni guruhlarga ajratish — ular doim yonma-yon bo'lishi shart */
  var spPairs=getSpousePairs(ids);
  var groups=[],placed={};
  /* Guruhlar: [id, spouseId] yoki [id] */
  ids.forEach(function(id){
    if(placed[id])return;
    placed[id]=true;
    var grp=[id];
    (spPairs[id]||[]).forEach(function(s){
      if(!placed[s]){placed[s]=true;grp.push(s);}
    });
    groups.push(grp);
  });
  /* Guruhlarni x bo'yicha tartiblash */
  groups.sort(function(a,b){
    var ax=Math.min.apply(null,a.map(function(id){return pos[id]?pos[id].x:0;}));
    var bx=Math.min.apply(null,b.map(function(id){return pos[id]?pos[id].x:0;}));
    return ax-bx;
  });
  /* Har bir guruhni joylash — guruh ichida juftlar yonma-yon */
  var curX=-Infinity;
  groups.forEach(function(grp){
    /* Guruhni ichki tartiblash (x bo'yicha) */
    grp.sort(function(a,b){return(pos[a]?pos[a].x:0)-(pos[b]?pos[b].x:0);});
    grp.forEach(function(id,i){
      var wantX=pos[id]?pos[id].x:0;
      var minX=(i===0)?curX+step:pos[grp[i-1]].x+NW+CFG.minPartnerSeparation;
      if(i===0) minX=Math.max(wantX,curX===-Infinity?wantX:curX+step);
      pos[id].x=Math.max(wantX,minX);
    });
    curX=pos[grp[grp.length-1]].x;
  });
}

/* ── KO'RINADIGAN NODELAR (subLevels bilan) ── */
function getVisibleIds(){
  var vis={};_persons.forEach(function(p){vis[p.id]=true;});
  /* Collapsed: bolalarni yashir */
  _persons.forEach(function(p){if(_collapsed[p.id])hideDesc(p.id,vis);});
  /* subLevels: har bir node uchun ko'rsatiladigan ota-ona daraja soni */
  _persons.forEach(function(p){
    var maxUp=(_subLevels[p.id]!==undefined)?_subLevels[p.id]:CFG.subLevels;
    if(maxUp<999) hideAncBeyond(p.id,vis,maxUp,0,{});
  });
  return vis;
}
function hideDesc(id,vis){(_childMap[id]||[]).forEach(function(c){vis[c]=false;hideDesc(c,vis);});}
/* subLevels: depth > maxUp bo'lgan ajdodlarni yashir */
function hideAncBeyond(id,vis,maxUp,depth,visited){
  if(visited[id])return;visited[id]=true;
  var p=_nodeMap[id];if(!p)return;
  [p.fatherId,p.motherId].forEach(function(pid){
    if(!pid||!_nodeMap[pid])return;
    if(depth>=maxUp){vis[pid]=false;hideAncBeyond(pid,vis,maxUp,depth+1,visited);}
    else{hideAncBeyond(pid,vis,maxUp,depth+1,visited);}
  });
}

/* ── RENDER ── */
function renderAll(){
  var ns='http://www.w3.org/2000/svg';
  var lc=document.getElementById('lc'),ln=document.getElementById('ln');
  lc.innerHTML='';ln.innerHTML='';
  var vis=getVisibleIds();
  /* === CHIZIQLAR HOZIRCHA O'CHIRILGAN ===
  _relations.forEach(function(r){
    if((r.type||'').toUpperCase()!=='SPOUSE')return;
    if(!vis[r.fromPersonId]||!vis[r.toPersonId])return;
    var el=drawSpouseLine(ns,r.fromPersonId,r.toPersonId);
    if(el)lc.appendChild(el);
  });
  _persons.forEach(function(p){
    if(!vis[p.id])return;
    [p.fatherId,p.motherId].forEach(function(pid){
      if(pid&&vis[pid]&&_nodeMap[pid])lc.appendChild(drawPCLine(ns,pid,p.id));
    });
  });
  === CHIZIQLAR OXIRI === */
  _persons.forEach(function(p){if(vis[p.id]){var g=drawNode(ns,p);if(g)ln.appendChild(g);}});
  applyTransform();
}

function drawPCLine(ns,fId,tId){
  var f=_positions[fId],t=_positions[tId];
  var x1=f.x+NW/2,y1=f.y+NH,x2=t.x+NW/2,y2=t.y,my=(y1+y2)/2;
  var path=document.createElementNS(ns,'path');
  path.setAttribute('d','M'+x1+' '+y1+' C'+x1+' '+my+','+x2+' '+my+','+x2+' '+y2);
  path.setAttribute('fill','none');path.setAttribute('stroke','rgba(99,102,241,.5)');
  path.setAttribute('stroke-width','2');path.setAttribute('stroke-linecap','round');
  return path;
}
function drawSpouseLine(ns,id1,id2){
  var a=_positions[id1],b=_positions[id2];if(!a||!b)return null;
  var left=a.x<b.x?a:b,right=a.x<b.x?b:a;
  var x1=left.x+NW,y1=left.y+NH/2,x2=right.x,y2=right.y+NH/2,mx=(x1+x2)/2,my=(y1+y2)/2;
  var g=document.createElementNS(ns,'g');
  var line=document.createElementNS(ns,'line');
  line.setAttribute('x1',x1);line.setAttribute('y1',y1);line.setAttribute('x2',x2);line.setAttribute('y2',y2);
  line.setAttribute('stroke','rgba(236,72,153,.4)');line.setAttribute('stroke-width','2');line.setAttribute('stroke-dasharray','5,3');
  g.appendChild(line);
  var t=document.createElementNS(ns,'text');
  t.setAttribute('x',mx);t.setAttribute('y',my+5);t.setAttribute('text-anchor','middle');t.setAttribute('font-size','12');
  t.textContent='💍';g.appendChild(t);return g;
}

function drawNode(ns,p){
  var pos=_positions[p.id];if(!pos)return null;
  var female=(p.gender||'').toUpperCase()==='FEMALE';
  var hasChildren=(_childMap[p.id]||[]).length>0;
  var hasParents=!!(p.fatherId&&_nodeMap[p.fatherId])||(p.motherId&&_nodeMap[p.motherId]);

  var g=document.createElementNS(ns,'g');
  g.setAttribute('class','ft-node');g.setAttribute('data-id',p.id);
  g.setAttribute('transform','translate('+pos.x+','+pos.y+')');
  g.addEventListener('click',function(e){e.stopPropagation();openCtx(p.id,e);});

  var rect=document.createElementNS(ns,'rect');
  rect.setAttribute('class','nbg');rect.setAttribute('width',NW);rect.setAttribute('height',NH);rect.setAttribute('rx',13);
  rect.setAttribute('fill',female?'url(#gf)':'url(#gm)');rect.setAttribute('filter','url(#sh)');
  g.appendChild(rect);

  var sh=document.createElementNS(ns,'rect');
  sh.setAttribute('width',NW);sh.setAttribute('height',4);sh.setAttribute('rx',13);sh.setAttribute('fill','rgba(255,255,255,.15)');
  g.appendChild(sh);

  var clipId='c'+p.id;
  var defs=document.createElementNS(ns,'defs');
  var clip=document.createElementNS(ns,'clipPath');clip.setAttribute('id',clipId);
  var cc=document.createElementNS(ns,'circle');cc.setAttribute('cx',42);cc.setAttribute('cy',NH/2);cc.setAttribute('r',PR);
  clip.appendChild(cc);defs.appendChild(clip);g.appendChild(defs);
  var pbg=document.createElementNS(ns,'circle');
  pbg.setAttribute('cx',42);pbg.setAttribute('cy',NH/2);pbg.setAttribute('r',PR);pbg.setAttribute('fill','rgba(255,255,255,.1)');
  g.appendChild(pbg);
  if(p.photoUrl){
    var img=document.createElementNS(ns,'image');
    img.setAttribute('href',p.photoUrl);img.setAttribute('x',42-PR);img.setAttribute('y',NH/2-PR);
    img.setAttribute('width',PR*2);img.setAttribute('height',PR*2);img.setAttribute('clip-path','url(#'+clipId+')');
    img.setAttribute('preserveAspectRatio','xMidYMid slice');g.appendChild(img);
  }else{
    var ico=document.createElementNS(ns,'text');
    ico.setAttribute('x',42);ico.setAttribute('y',NH/2+6);ico.setAttribute('text-anchor','middle');ico.setAttribute('font-size','22');
    ico.textContent=female?'👩':'👨';g.appendChild(ico);
  }
  var dv=document.createElementNS(ns,'line');
  dv.setAttribute('x1',80);dv.setAttribute('y1',10);dv.setAttribute('x2',80);dv.setAttribute('y2',NH-10);
  dv.setAttribute('stroke','rgba(255,255,255,.1)');dv.setAttribute('stroke-width','1');g.appendChild(dv);

  var years=getYears(p);
  addTxt(g,ns,90,NH/2-(years?8:2),p.name||'—',13,700,'white');
  if(years)addTxt(g,ns,90,NH/2+8,years,10,400,'rgba(255,255,255,.6)');

  /* ▲ subLevels tugmasi — ota-ona darajalarini +1/-1 boshqarish (Balkan subLevels) */
  if(hasParents){
    var nid=p.id;
    var curLv=(_subLevels[nid]!==undefined)?_subLevels[nid]:CFG.subLevels;
    var isHidden=(curLv===0);
    /* [-] tugma: ota-onalarni yashir (0 daraja) */
    var btnHide=makeBtn(ns,NW/2-19,-9,isHidden?'rgba(16,185,129,.8)':'rgba(236,72,153,.7)',isHidden?'+':'-');
    btnHide.title=isHidden?"Ota-onalarni ko'rsat":"Ota-onalarni yashir";
    btnHide.addEventListener('click',function(e){
      e.stopPropagation();
      if((_subLevels[nid]||CFG.subLevels)===0){_subLevels[nid]=1;}else{_subLevels[nid]=0;}
      renderAll();
    });
    g.appendChild(btnHide);
    /* [+] tugma: ota-ona darajalari +1 */
    if(!isHidden){
      var btnMore=makeBtn(ns,NW/2+1,-9,'rgba(99,102,241,.7)','+1');
      btnMore.title='Bir daraja yuqori ko\'rsat';
      btnMore.addEventListener('click',function(e){
        e.stopPropagation();
        var lv=(_subLevels[nid]!==undefined)?_subLevels[nid]:CFG.subLevels;
        _subLevels[nid]=(lv>=999)?999:lv+1;
        renderAll();
      });
      g.appendChild(btnMore);
    }
  }

  /* ▼ Bolalarni yashir/ko'rsat (Balkan expandCollapse) */
  if(hasChildren){
    var coll=_collapsed[p.id];
    var cid2=p.id;
    var btnB=makeBtn(ns,NW/2-9,NH-1,coll?'rgba(16,185,129,.8)':'rgba(124,58,237,.7)',coll?'+':'-');
    btnB.addEventListener('click',function(e){e.stopPropagation();_collapsed[cid2]=!_collapsed[cid2];renderAll();});
    g.appendChild(btnB);
  }
  return g;
}

function makeBtn(ns,x,y,color,lbl){
  var g=document.createElementNS(ns,'g');
  g.setAttribute('class','collapse-btn');g.setAttribute('transform','translate('+x+','+y+')');
  var r=document.createElementNS(ns,'rect');
  r.setAttribute('width',18);r.setAttribute('height',10);r.setAttribute('rx',5);r.setAttribute('fill',color);
  var t=document.createElementNS(ns,'text');
  t.setAttribute('x',9);t.setAttribute('y',8);t.setAttribute('text-anchor','middle');t.setAttribute('font-size','7');t.setAttribute('fill','white');
  t.textContent=lbl;g.appendChild(r);g.appendChild(t);return g;
}
function addTxt(g,ns,x,y,txt,sz,fw,fill){
  var t=document.createElementNS(ns,'text');
  t.setAttribute('x',x);t.setAttribute('y',y);t.setAttribute('font-size',sz);
  if(fw)t.setAttribute('font-weight',fw);t.setAttribute('fill',fill);
  t.setAttribute('font-family','system-ui,sans-serif');t.setAttribute('pointer-events','none');
  t.textContent=trunc(txt,16);g.appendChild(t);
}
function getYears(p){if(!p.birthDate&&!p.diedDate)return '';return(p.birthDate?String(p.birthDate).slice(0,4):'?')+(p.diedDate?' – '+String(p.diedDate).slice(0,4):'');}
function trunc(s,n){return s&&s.length>n?s.slice(0,n-1)+'…':s;}

/* ── ZOOM / PAN ── */
function applyTransform(){document.getElementById('vp').setAttribute('transform','translate('+_tx+','+_ty+') scale('+_scale+')');}
function zoomIn(){_scale=Math.min(_scale*1.2,6);applyTransform();}
function zoomOut(){_scale=Math.max(_scale/1.2,.08);applyTransform();}
function fitView(){
  if(!_persons.length)return;
  var xs=_persons.map(function(p){return _positions[p.id]?_positions[p.id].x:0;});
  var ys=_persons.map(function(p){return _positions[p.id]?_positions[p.id].y:0;});
  var minX=Math.min.apply(null,xs)-40,maxX=Math.max.apply(null,xs)+NW+40;
  var minY=Math.min.apply(null,ys)-40,maxY=Math.max.apply(null,ys)+NH+40;
  var w=document.getElementById('wrap').clientWidth,h=document.getElementById('wrap').clientHeight;
  var s=Math.min(w/(maxX-minX),h/(maxY-minY),.98);
  _scale=s;_tx=(w-(maxX-minX)*s)/2-minX*s;_ty=(h-(maxY-minY)*s)/2-minY*s;applyTransform();
}
function initEvents(){
  var svg=document.getElementById('svg');
  svg.addEventListener('wheel',function(e){
    e.preventDefault();
    var r=svg.getBoundingClientRect(),mx=e.clientX-r.left,my=e.clientY-r.top;
    var f=e.deltaY<0?1.12:.89,ns=Math.min(Math.max(_scale*f,.06),7);
    _tx=mx-(mx-_tx)*(ns/_scale);_ty=my-(my-_ty)*(ns/_scale);_scale=ns;applyTransform();
  },{passive:false});
  svg.addEventListener('mousedown',function(e){if(e.button!==0)return;_drag=true;_dx=e.clientX;_dy=e.clientY;_dtx=_tx;_dty=_ty;svg.style.cursor='grabbing';});
  window.addEventListener('mousemove',function(e){if(!_drag)return;_tx=_dtx+(e.clientX-_dx);_ty=_dty+(e.clientY-_dy);applyTransform();});
  window.addEventListener('mouseup',function(){_drag=false;document.getElementById('svg').style.cursor='';});
  svg.addEventListener('click',function(){closeCtx();});
}

/* ── CONTEXT MENU ── */
function openCtx(id,evt){
  closeCtx();_activeId=id;
  var m=document.getElementById('ctx');m.classList.add('show');
  var x=evt.clientX,y=evt.clientY;
  if(x+200>window.innerWidth)x=window.innerWidth-210;
  if(y+290>window.innerHeight)y=window.innerHeight-300;
  m.style.left=x+'px';m.style.top=y+'px';
}
function closeCtx(){document.getElementById('ctx').classList.remove('show');}
function ctxFocus(){
  closeCtx();if(!_activeId||!_positions[_activeId])return;
  var p=_positions[_activeId],w=document.getElementById('wrap');
  _tx=w.clientWidth/2-(p.x+NW/2)*_scale;_ty=w.clientHeight/2-(p.y+NH/2)*_scale;applyTransform();
}
function ctxCollapse(){closeCtx();if(_activeId){_collapsed[_activeId]=!_collapsed[_activeId];renderAll();}}
function ctxHideParents(){
  closeCtx();if(!_activeId)return;
  /* SubLevels: 0 (yashir) yoki cheksiz (ko'rsat) */
  var cur=(_subLevels[_activeId]!==undefined)?_subLevels[_activeId]:CFG.subLevels;
  _subLevels[_activeId]=(cur===0)?CFG.subLevels:0;
  renderAll();
}
function ctxParent(){
  closeCtx();if(!_activeId)return;
  var n=_nodeMap[_activeId];if(!n||!n.treeId){showErr('treeId yo\'q');return;}
  apiFetch('POST','/api/persons/add-parent',{id:Number(_activeId),fatherId:null,motherId:null,treeId:n.treeId})
    .then(function(){reloadTree();}).catch(function(e){showErr(e.message);});
}
function ctxSpouse(){
  closeCtx();if(!_activeId)return;
  var n=_nodeMap[_activeId];if(!n||!n.treeId){showErr('treeId yo\'q');return;}
  apiFetch('POST','/api/persons/add-spouse',{id:Number(_activeId),treeId:n.treeId})
    .then(function(){reloadTree();}).catch(function(e){showErr(e.message);});
}
function ctxChild(){closeCtx();if(!_activeId)return;_childPendingId=_activeId;document.getElementById('genderPopup').classList.add('show');}
function confirmChild(gender){
  document.getElementById('genderPopup').classList.remove('show');
  if(!_childPendingId)return;
  var n=_nodeMap[_childPendingId];if(!n||!n.treeId){showErr('treeId yo\'q');return;}
  apiFetch('POST','/api/persons/add-child',{id:Number(_childPendingId),spouseId:null,childGender:gender,treeId:n.treeId})
    .then(function(){_childPendingId=null;reloadTree();}).catch(function(e){_childPendingId=null;showErr(e.message);});
}
function closeGender(){document.getElementById('genderPopup').classList.remove('show');_childPendingId=null;}
function ctxEdit(){
  closeCtx();if(!_activeId)return;
  var n=_nodeMap[_activeId];if(!n)return;_editId=_activeId;
  document.getElementById('ef-name').value=n.name||'';
  document.getElementById('ef-birth').value=n.birthDate?String(n.birthDate).slice(0,10):'';
  document.getElementById('ef-died').value=n.diedDate?String(n.diedDate).slice(0,10):'';
  document.getElementById('ef-photo').value=n.photoUrl||'';
  document.getElementById('editModal').classList.add('show');
}
function saveEdit(){
  if(!_editId)return;
  apiFetch('PUT','/api/persons/'+_editId,{
    name:document.getElementById('ef-name').value.trim()||null,
    birthDate:document.getElementById('ef-birth').value||null,
    diedDate:document.getElementById('ef-died').value||null,
    photoUrl:document.getElementById('ef-photo').value.trim()||null
  }).then(function(){closeEdit();reloadTree();}).catch(function(e){showErr(e.message);});
}
function closeEdit(){document.getElementById('editModal').classList.remove('show');_editId=null;}
function ctxDelete(){
  closeCtx();if(!_activeId)return;
  var n=_nodeMap[_activeId];
  if(!confirm("O'chirish: "+(n?n.name:_activeId)+'?'))return;
  apiFetch('DELETE','/api/persons/'+_activeId,null)
    .then(function(){_activeId=null;reloadTree();}).catch(function(e){showErr(e.message);});
}

/* ── SEARCH ── */
function hlSearch(q){
  document.querySelectorAll('.ft-node').forEach(function(g){
    var id=g.getAttribute('data-id'),p=_nodeMap[id];
    var match=q&&p&&(p.name||'').toLowerCase().indexOf(q.toLowerCase())>=0;
    var r=g.querySelector('.nbg');if(!r)return;
    if(match){r.setAttribute('stroke','rgba(16,185,129,.9)');r.setAttribute('stroke-width','3');}
    else{r.removeAttribute('stroke');r.removeAttribute('stroke-width');}
  });
}

/* ── STATS ── */
function updateStats(){
  document.getElementById('sT').textContent=_persons.length;
  document.getElementById('sM').textContent=_persons.filter(function(p){return(p.gender||'').toUpperCase()==='MALE';}).length;
  document.getElementById('sF').textContent=_persons.filter(function(p){return(p.gender||'').toUpperCase()==='FEMALE';}).length;
  document.getElementById('sS').textContent=_relations.filter(function(r){return(r.type||'').toUpperCase()==='SPOUSE';}).length;
  document.getElementById('stats').style.display='flex';
}

/* ── API ── */
function apiFetch(method,url,body){
  var opts={method:method,headers:{'Authorization':'Bearer '+_token,'Content-Type':'application/json'}};
  if(body&&(method==='POST'||method==='PUT'))opts.body=JSON.stringify(body);
  return fetch(url,opts).then(function(r){
    if(!r.ok)return r.text().then(function(t){try{t=JSON.parse(t).message||t;}catch(e){}throw new Error(r.status+': '+t);});
    if(r.status===204)return null;
    var ct=r.headers.get('content-type')||'';
    return ct.indexOf('application/json')>=0?r.json():null;
  });
}
function showLoad(v){document.getElementById('loading').classList.toggle('hidden',!v);}
function showErr(msg){document.getElementById('errMsg').textContent=msg;document.getElementById('errModal').classList.add('show');}
function hideErr(){document.getElementById('errModal').classList.remove('show');}
