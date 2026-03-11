/**
 * familytree-engine/src/algorithms/layout.js
 *
 * Reingold-Tilford daraxt joylashuv algoritmi
 * + Oilaviy daraxt uchun Partner (pids) kengaytmasi
 *
 * Algoritmning 4 bosqichi:
 *   1. firstWalk()  — pastdan yuqoriga: preliminary x hisoblash
 *   2. apportion()  — aka-uka subtreelari orasidagi masofani tenglashtirish
 *   3. secondWalk() — yuqoridan pastga: haqiqiy x,y koordinatalarni belgilash
 *   4. Partners     — pids[] bo'yicha juftlarni yonga joylashtirish
 */

import { LAYOUT, ORIENTATION } from '../core/constants.js';

// ─── Ichki yordamchi strukturalar ─────────────────────────────────────────

/**
 * Layout hisoblash uchun ishlatiladigan ichki node
 */
class LayoutNode {
  constructor(data, level = 0) {
    this.id       = data.id;
    this.data     = data;           // original data
    this.level    = level;

    // Reingold-Tilford uchun
    this.prelim   = 0;              // dastlabki x pozitsiya
    this.modifier = 0;              // o'zgartirgich
    this.shift    = 0;              // siljish (moveSubtree)
    this.change   = 0;              // bosqichli o'zgarish
    this.thread   = null;           // "ip" — tez traversal uchun
    this.ancestor = this;           // ancestor reference

    // Oila munosabatlari
    this.parent        = null;
    this.children      = [];        // farzandlar
    this.partnerOf     = null;      // pids juft node
    this.isPartner     = false;

    // Hisoblangan koordinatalar
    this.x = 0;
    this.y = 0;
    this.w = 0;                     // kenglik (template dan)
    this.h = 0;                     // balandlik (template dan)

    // Qo'shni nodelar (chiziq chizish uchun)
    this.leftNeighbor  = null;
    this.rightNeighbor = null;

    // Collapse/expand
    this.collapsed = false;
  }

  /** Birinchi farzand */
  get firstChild() { return this.children[0] || null; }

  /** Oxirgi farzand */
  get lastChild() { return this.children[this.children.length - 1] || null; }

  /** Chap aka-uka (siblings ichida) */
  get leftSibling() {
    if (!this.parent) return null;
    const idx = this.parent.children.indexOf(this);
    return idx > 0 ? this.parent.children[idx - 1] : null;
  }

  /** O'ng aka-uka */
  get rightSibling() {
    if (!this.parent) return null;
    const idx = this.parent.children.indexOf(this);
    return idx < this.parent.children.length - 1
      ? this.parent.children[idx + 1] : null;
  }

  /** Eng o'ng thread yoki farzand */
  get rightmost() {
    return this.thread || this.lastChild;
  }

  /** Eng chap thread yoki farzand */
  get leftmost() {
    return this.thread || this.firstChild;
  }
}

// ─── Asosiy Layout klassi ──────────────────────────────────────────────────

export class TreeLayout {
  /**
   * @param {object} cfg - layout konfiguratsiyasi
   * @param {number} cfg.levelSeparation
   * @param {number} cfg.siblingSeparation
   * @param {number} cfg.subtreeSeparation
   * @param {number} cfg.partnerNodeSeparation
   * @param {number} cfg.orientation
   * @param {number} cfg.layout
   * @param {number} nodeWidth
   * @param {number} nodeHeight
   */
  constructor(cfg, nodeWidth = 140, nodeHeight = 60) {
    this.levelSep   = cfg.levelSeparation   ?? 60;
    this.siblingSep = cfg.siblingSeparation ?? 35;
    this.subtreeSep = cfg.subtreeSeparation ?? 40;
    this.partnerSep = cfg.partnerNodeSeparation ?? 15;
    this.orientation = cfg.orientation      ?? ORIENTATION.top;
    this.layout      = cfg.layout           ?? LAYOUT.normal;
    this.nodeW       = nodeWidth;
    this.nodeH       = nodeHeight;
  }

  /**
   * Berilgan tugun ro'yxatini layout qiladi
   * @param {object[]} nodesData - { id, fid?, mid?, pids?, tags?, ... }
   * @param {object}   sizes     - { [id]: {w, h} } — har node uchun o'lcham
   * @returns {Map<id, {x,y,w,h,level}>}
   */
  compute(nodesData, sizes = {}) {
    if (!nodesData || nodesData.length === 0) return new Map();

    // 1. Dastlabki nishonlar va tugunlarni yaratish
    const nodeMap = new Map();
    for (const d of nodesData) {
      const ln = new LayoutNode(d);
      const sz = sizes[d.id] || { w: this.nodeW, h: this.nodeH };
      ln.w = sz.w;
      ln.h = sz.h;
      nodeMap.set(d.id, ln);
    }

    // Ota va Onani avtomat "Spouse" (Juft) deb e'lon qilish, agar DB unutgan bo'lsa
    for (const d of nodesData) {
       if (d.fid && d.mid) {
          const fLn = nodeMap.get(d.fid);
          const mLn = nodeMap.get(d.mid);
          if (fLn && mLn) {
             fLn.data.pids = fLn.data.pids || [];
             mLn.data.pids = mLn.data.pids || [];
             if (!fLn.data.pids.includes(d.mid)) fLn.data.pids.push(d.mid);
             if (!mLn.data.pids.includes(d.fid)) mLn.data.pids.push(d.fid);
          }
       }
    }

    const isBloodline = (d) => !!(d.fid || d.mid);

    // 2. Partnerlarni guruhlash: Qon-qarindoshni Asosiy (primaryNode) qilib olish
    const processedPartners = new Set();
    const crossTreeMarriages = [];

    for (const [id, ln] of nodeMap) {
      if (ln.isPartner) continue;

      const pids = ln.data.pids;
      if (pids && pids.length > 0) {
         let primaryNode = ln;
         for (const pid of pids) {
            const pNode = nodeMap.get(pid);
            if (!pNode) continue;
            // Agar pNode ning ota-onasi bo'lsa, u aniq qon-qarindosh
            if (isBloodline(pNode.data) && !isBloodline(primaryNode.data)) {
               primaryNode = pNode;
            }
         }

         if (primaryNode !== ln && !primaryNode.isPartner) {
            continue; // Boshqa siklda o'z o'rnini topadi
         }

         let effectiveW = primaryNode.w;
         for (const pid of primaryNode.data.pids) {
             const partner = nodeMap.get(pid);
             if (!partner || processedPartners.has(partner.id) || partner === primaryNode) continue;
             
             // Juda Muhim Yechim: Ikkala juftlikning ham ota-onasi bo'lsa, ularni majburiy
             // yopishtirmaymiz. Ular mustaqil root bo'lib o'sgandan so'ng, post-processing orqali birlashtiramiz.
             if (isBloodline(primaryNode.data) && isBloodline(partner.data)) {
                 crossTreeMarriages.push({ left: partner, right: primaryNode });
                 processedPartners.add(partner.id);
                 continue; 
             }
             
             partner.isPartner = true;
             partner.partnerOf = primaryNode;
             
             effectiveW += partner.w + this.partnerSep;
             processedPartners.add(partner.id);
         }
         primaryNode.effectiveW = effectiveW;
         primaryNode.originalW = primaryNode.w;
         primaryNode.w = effectiveW; // RT uchun keng blok vujudga keldi
      } else {
         ln.effectiveW = ln.w;
         ln.originalW = ln.w;
      }
    }

    // 3. Farzandlarni faqat bitta (Asosiy) tugunga ulash algoritmini xavfsiz qilish
    for (const d of nodesData) {
      const childLn = nodeMap.get(d.id);
      
      let parentId = d.fid;
      if (!parentId || !nodeMap.has(parentId)) {
          parentId = d.mid;
      }
      
      if (parentId && nodeMap.has(parentId)) {
          let parentLn = nodeMap.get(parentId);
          // Faqat isPartner larni primary ga yo'naltiramiz. 
          // Mustaqil er-xotinlarni o'z holiga qo'yamiz.
          if (parentLn.isPartner && parentLn.partnerOf) {
              parentLn = parentLn.partnerOf; 
          }
          
          if (!parentLn.children.includes(childLn)) {
              parentLn.children.push(childLn);
              if (!childLn.parent) childLn.parent = parentLn;
          }
      }
    }

    // Level belgilash: Global generation synchronization (Huddi FamilyTreeJS kabi)
    const visitedLevels = new Map();
    const processQueue = [];

    for (const [id, startNode] of nodeMap) {
       if (visitedLevels.has(id)) continue;
       
       visitedLevels.set(id, 0);
       processQueue.push(startNode);
       
       while (processQueue.length > 0) {
           const curr = processQueue.shift();
           const currLvl = visitedLevels.get(curr.id);
           
           // UP: parents
           const parents = [curr.data.fid, curr.data.mid].filter(Boolean).map(pid => nodeMap.get(pid)).filter(Boolean);
           for (const p of parents) {
               if (!visitedLevels.has(p.id)) {
                   visitedLevels.set(p.id, currLvl - 1);
                   processQueue.push(p);
               }
           }
           
           // DOWN: children
           for (const [cid, cNode] of nodeMap) {
               if ((cNode.data.fid === curr.id || cNode.data.mid === curr.id) && !visitedLevels.has(cid)) {
                   visitedLevels.set(cid, currLvl + 1);
                   processQueue.push(cNode);
               }
           }
           
           // SAME: spouses
           if (curr.data.pids) {
               for (const pid of curr.data.pids) {
                   const pNode = nodeMap.get(pid);
                   if (pNode && !visitedLevels.has(pid)) {
                       visitedLevels.set(pid, currLvl);
                       processQueue.push(pNode);
                   }
               }
           }
       }
    }

    // Min levelni 0 ga tenglash
    let minLvl = Infinity;
    for (const lvl of visitedLevels.values()) {
        if (lvl < minLvl) minLvl = lvl;
    }
    
    const roots = [];
    for (const [id, n] of nodeMap) {
        n.level = visitedLevels.get(id) - minLvl;
        if (!n.parent && !n.isPartner) {
            roots.push(n);
        }
    }

    // Qolgan bosqichlar...
    const result = new Map();
    if (roots.length > 0) {
      let currentOffsetX = 0;
      for (let i = 0; i < roots.length; i++) {
        const root = roots[i];
        this._firstWalk(root);
        this._secondWalk(root, -root.prelim);

        const bounds = this._bounds(root);
        this._shift(root, currentOffsetX - bounds.minX);
        currentOffsetX += (bounds.maxX - bounds.minX) + this.subtreeSep;
      }

      // 4. Parter tugunlarni o'zining asosiy nodining o'ng tomoniga chizish
      for (const [id, ln] of nodeMap) {
        if (ln.isPartner) continue;

        // Asosiy node ning kengligini asl qiymatiga qaytaramiz
        if (ln.originalW) {
            ln.w = ln.originalW;
        }

        const pids = ln.data.pids;
        if (!pids || pids.length === 0) continue;

        let currentXOffset = ln.w + this.partnerSep;

        for (const pid of pids) {
          const partner = nodeMap.get(pid);
          if (!partner || partner.partnerOf !== ln) continue;

          // Partner ni asosiy nodning o'ng tomoniga qo'y
          partner.y = ln.y;
          partner.x = ln.x + currentXOffset;

          currentXOffset += partner.w + this.partnerSep;
        }
      }
      
      // 5. Cross-tree Marriages ni to'g'ri joylashtirish
      for (const cross of crossTreeMarriages) {
         let { left, right } = cross;
         
         // Left har doim chapda turishini ta'minlash
         if (left.x > right.x) {
             const temp = left;
             left = right;
             right = temp;
         }

         const targetRightX = left.x + left.w + this.partnerSep;
         const shiftAmount = targetRightX - right.x;

         let rightRoot = right;
         while (rightRoot.parent) rightRoot = rightRoot.parent;

         // RightRoot va undan o'ngdagi barcha daraxtlarni surib ochamiz
         for (const root of roots) {
             if (root.x >= rightRoot.x) {
                 this._shift(root, shiftAmount);
             }
         }
         
         // Ular orasidagi farzandlarni (Shared children) O'rtaga markazlash
         const leftChildren = left.children.filter(c => c.data.fid === right.id || c.data.mid === right.id);
         for (const c of leftChildren) {
             this._shift(c, Math.abs(right.w + this.partnerSep) / 2);
         }
         const rightChildren = right.children.filter(c => c.data.fid === left.id || c.data.mid === left.id);
         for (const c of rightChildren) {
             this._shift(c, -Math.abs(left.w + this.partnerSep) / 2);
         }
      }

      // 5. Yo'nalishga qarab aylantirish
      this._applyOrientation(nodeMap);

      // 6. Natijani Map formatida qaytarish
      for (const [id, ln] of nodeMap) {
        result.set(id, {
          data:    ln.data,
          partnerOf: ln.partnerOf ? ln.partnerOf.id : null, // ID ni saqlash
          x:       Math.round(ln.x),
          y:       Math.round(ln.y),
          w:       ln.w,
          h:       ln.h,
          level:   ln.level,
          isPartner: ln.isPartner,
        });
      }
    }

    return result;
  }



  _findRoots(nodeMap) {
    return [...nodeMap.values()].filter(n => !n.parent && !n.isPartner);
  }

  // ─── 1-bosqich: firstWalk (pastdan yuqoriga) ──────────────────────────

  /**
   * Reingold-Tilford firstWalk
   * Har bir tugun uchun prelim (dastlabki x) hisoblaydi
   */
  _firstWalk(v) {
    if (v.children.length === 0) {
      // Barg tugun
      const ls = v.leftSibling;
      if (ls) {
        v.prelim = ls.prelim + ls.w + this.siblingSep;
      } else {
        v.prelim = 0;
      }
      return;
    }

    // Ichki tugun: avval farzandlarni hisoblash
    let defaultAncestor = v.firstChild;
    for (const child of v.children) {
      this._firstWalk(child);
      defaultAncestor = this._apportion(child, defaultAncestor);
    }
    this._executeShifts(v);

    const midpoint = (v.firstChild.prelim + v.lastChild.prelim + v.lastChild.w) / 2 - v.w / 2;

    const ls = v.leftSibling;
    if (ls) {
      v.prelim = ls.prelim + ls.w + this.siblingSep;
      v.modifier = v.prelim - midpoint;
    } else {
      v.prelim = midpoint;
    }
  }

  /**
   * apportion — aka-ukalar orasidagi masofani tenglashtirish
   * Bu Reingold-Tilford algoritmining eng muhim qismi
   */
  _apportion(v, defaultAncestor) {
    const w = v.leftSibling;
    if (!w) return defaultAncestor;

    // 4 pointer: inner/outer left/right
    let vip = v;         // v inner right
    let vop = v;         // v outer right
    let vim = w;         // w inner left
    let vom = v.parent.firstChild; // w outer left

    let sip = vip.modifier;
    let sop = vop.modifier;
    let sim = vim.modifier;
    let som = vom.modifier;

    let nextRight = this._nextRight(vim);
    let nextLeft  = this._nextLeft(vip);

    while (nextRight && nextLeft) {
      vim = nextRight;
      vip = nextLeft;
      vom = this._nextLeft(vom);
      vop = this._nextRight(vop);

      vop.ancestor = v;

      const shift = (vim.prelim + sim) - (vip.prelim + sip) + vim.w + this.siblingSep;
      if (shift > 0) {
        const anc = this._ancestor(vim, v, defaultAncestor);
        this._moveSubtree(anc, v, shift);
        sip += shift;
        sop += shift;
      }

      sim += vim.modifier;
      sip += vip.modifier;
      som += vom ? vom.modifier : 0;
      sop += vop.modifier;

      nextRight = this._nextRight(vim);
      nextLeft  = this._nextLeft(vip);
    }

    if (nextRight && !this._nextRight(vop)) {
      vop.thread   = nextRight;
      vop.modifier += sim - sop;
    }

    if (nextLeft && !this._nextLeft(vom)) {
      if (vom) {
        vom.thread   = nextLeft;
        vom.modifier += sip - som;
      }
      defaultAncestor = v;
    }

    return defaultAncestor;
  }

  _nextLeft(v) {
    return v.children.length > 0 ? v.firstChild : v.thread;
  }

  _nextRight(v) {
    return v.children.length > 0 ? v.lastChild : v.thread;
  }

  _ancestor(vim, v, defaultAncestor) {
    if (v.parent && v.parent.children.includes(vim.ancestor)) {
      return vim.ancestor;
    }
    return defaultAncestor;
  }

  /**
   * moveSubtree — subtreeni o'ngga shift qilish
   * Bu O(n) ga erishishga yordam beradi
   */
  _moveSubtree(wm, wp, shift) {
    const parent = wp.parent;
    if (!parent) return;
    const subtrees = parent.children.indexOf(wp) - parent.children.indexOf(wm);
    if (subtrees === 0) return;
    wp.change   -= shift / subtrees;
    wp.shift    += shift;
    wm.change   += shift / subtrees;
    wp.prelim   += shift;
    wp.modifier += shift;
  }

  /**
   * executeShifts — yig'ilgan shift va change larni qo'llash
   */
  _executeShifts(v) {
    let shift  = 0;
    let change = 0;
    for (let i = v.children.length - 1; i >= 0; i--) {
      const w = v.children[i];
      w.prelim   += shift;
      w.modifier += shift;
      change     += w.change;
      shift      += w.shift + change;
    }
  }

  // ─── 2-bosqich: secondWalk (yuqoridan pastga) ─────────────────────────

  /**
   * Haqiqiy x, y koordinatalarni belgilash
   * m = yig'ilgan modifier qiymati
   */
  _secondWalk(v, m) {
    v.x = v.prelim + m;
    
    // Yechim: FamilyTree JS kabi sof level asosida qat'iy qavat berish
    // Dastlabki depth arg da ishlayotganda levelni buzmagan
    v.y = v.level * (this.nodeH + this.levelSep);
    
    v.modifier = v.modifier || 0;

    for (const child of v.children) {
      this._secondWalk(child, m + v.modifier);
    }
  }

  // ─── Partner joylashuvi ────────────────────────────────────────────────

  /**
   * pids[] bo'yicha turmush o'rtoqlarni yonga joylashtirish
   * Juft node → asosiy nodning o'ng tomoniga qo'yiladi
   */
  _placePartners(nodeMap) {
    const processed = new Set();

    for (const [id, ln] of nodeMap) {
      const pids = ln.data.pids;
      if (!pids || pids.length === 0) continue;

      for (const pid of pids) {
        const pairKey = [id, pid].sort().join('-');
        if (processed.has(pairKey)) continue;
        processed.add(pairKey);

        const partner = nodeMap.get(pid);
        if (!partner) continue;

        // Partner ni asosiy nodning o'ng tomoniga qo'y
        partner.isPartner = true;
        partner.partnerOf = ln;
        partner.y = ln.y;
        partner.x = ln.x + ln.w + this.partnerSep;

        // Asosiy nodning o'ng tomonidagi barcha nodelarni surib chiqarish
        this._shiftRightOf(nodeMap, ln, partner.w + this.partnerSep);
      }
    }
  }

  /**
   * Berilgan noddan o'ngdagi barcha nodelarni siljitish
   */
  _shiftRightOf(nodeMap, anchor, amount) {
    for (const [, ln] of nodeMap) {
      if (ln === anchor || ln.isPartner) continue;
      if (ln.x > anchor.x && Math.abs(ln.y - anchor.y) < this.levelSep) {
        ln.x += amount;
        // Farzandlarni ham siljitish
        this._shiftSubtree(ln, amount);
      }
    }
  }

  _shiftSubtree(node, amount) {
    for (const child of node.children) {
      child.x += amount;
      this._shiftSubtree(child, amount);
    }
  }

  // ─── Yo'nalish ────────────────────────────────────────────────────────

  /**
   * Yo'nalish asosida x,y larni aylantirish
   */
  _applyOrientation(nodeMap) {
    if (this.orientation === ORIENTATION.top) return; // default

    // Barcha nodelarning markazini topish
    let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
    for (const [, ln] of nodeMap) {
      minX = Math.min(minX, ln.x);
      minY = Math.min(minY, ln.y);
      maxX = Math.max(maxX, ln.x + ln.w);
      maxY = Math.max(maxY, ln.y + ln.h);
    }
    const cx = (minX + maxX) / 2;
    const cy = (minY + maxY) / 2;

    for (const [, ln] of nodeMap) {
      const dx = ln.x - cx;
      const dy = ln.y - cy;

      switch (this.orientation) {
        case ORIENTATION.bottom:
          ln.y = cy - dy;
          break;
        case ORIENTATION.right:
          [ln.x, ln.y] = [cy + dy, cx - dx];
          [ln.w, ln.h] = [ln.h, ln.w];
          break;
        case ORIENTATION.left:
          [ln.x, ln.y] = [cy - dy, cx + dx];
          [ln.w, ln.h] = [ln.h, ln.w];
          break;
      }
    }
  }

  // ─── Yordamchi ──────────────────────────────────────────────────────

  _bounds(node) {
    let minX = node.x, maxX = node.x + node.w;
    const visit = (n) => {
      minX = Math.min(minX, n.x);
      maxX = Math.max(maxX, n.x + n.w);
      n.children.forEach(visit);
    };
    visit(node);
    return { minX, maxX };
  }

  _shift(node, amount) {
    node.x += amount;
    node.children.forEach(c => this._shift(c, amount));
  }
}

// ─── Grid Layout ──────────────────────────────────────────────────────────

/**
 * Grid layout — nodelarni setka shaklida joylashtirish
 * FamilyTreeJS layout.grid = -1 ga mos
 */
export class GridLayout {
  constructor(cfg, nodeWidth = 140, nodeHeight = 60) {
    this.columns = cfg.columns ?? 4;
    this.padH    = cfg.siblingSeparation ?? 35;
    this.padV    = cfg.levelSeparation   ?? 60;
    this.nodeW   = nodeWidth;
    this.nodeH   = nodeHeight;
  }

  compute(nodesData, sizes = {}) {
    const result = new Map();
    const cols = this.columns;

    nodesData.forEach((d, i) => {
      const sz = sizes[d.id] || { w: this.nodeW, h: this.nodeH };
      const row = Math.floor(i / cols);
      const col = i % cols;
      result.set(d.id, {
        data:  d,
        partnerOf: null,
        x:     col * (sz.w + this.padH),
        y:     row * (sz.h + this.padV),
        w:     sz.w,
        h:     sz.h,
        level: row,
        isPartner: false,
      });
    });

    return result;
  }
}

// ─── Layout Factory ───────────────────────────────────────────────────────

/**
 * Config asosida to'g'ri layout ni tanlash
 * @param {object} cfg
 * @param {number} nodeW
 * @param {number} nodeH
 */
export function createLayout(cfg, nodeW, nodeH) {
  if (cfg.layout === LAYOUT.grid || cfg.layout === -1) {
    return new GridLayout(cfg, nodeW, nodeH);
  }
  return new TreeLayout(cfg, nodeW, nodeH);
}
