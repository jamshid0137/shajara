var k = Object.defineProperty;
var I = (g, t, e) => t in g ? k(g, t, { enumerable: !0, configurable: !0, writable: !0, value: e }) : g[t] = e;
var b = (g, t, e) => I(g, typeof t != "symbol" ? t + "" : t, e);
const $ = {
  normal: 0,
  mixed: 1,
  tree: 2,
  treeLeftOffset: 3,
  treeRightOffset: 4,
  treeLeft: 5,
  treeRight: 6,
  grid: -1
}, _ = {
  top: 0,
  bottom: 1,
  right: 2,
  left: 3,
  top_left: 4,
  bottom_left: 5,
  right_top: 6,
  left_top: 7
}, x = {
  expand: 0,
  collapse: 1,
  maximize: 101,
  minimize: 102,
  edit: 1,
  zoom: 2,
  ctrlZoom: 22,
  xScroll: 3,
  yScroll: 4,
  none: 5,
  init: 6,
  update: 7,
  pan: 8,
  centerNode: 9,
  resize: 10,
  insert: 11,
  details: 13,
  exporting: 14
}, L = {
  center: 8
}, v = {
  // Layout
  layout: $.normal,
  orientation: _.top,
  levelSeparation: 60,
  siblingSeparation: 35,
  subtreeSeparation: 40,
  mixedHierarchyNodesSeparation: 15,
  assistantSeparation: 100,
  minPartnerSeparation: 30,
  partnerChildrenSplitSeparation: 10,
  partnerNodeSeparation: 15,
  columns: 10,
  padding: 30,
  // Ko'rinish
  mode: "light",
  // 'light' | 'dark'
  template: "default",
  scaleInitial: 1,
  scaleMin: 0.1,
  scaleMax: 5,
  // Animatsiya
  anim: {
    duration: 200,
    func: "outPow"
  },
  // Zoom
  zoom: {
    speed: 120,
    smooth: 12
  },
  // Interaktivlik
  interactive: !0,
  enableSearch: !0,
  enablePan: !0,
  enableDragDrop: !1,
  mouseScrool: "zoom",
  // 'zoom' | 'scroll' | 'none'
  keyNavigation: !1,
  sticky: !0,
  // Data
  nodes: [],
  clinks: [],
  slinks: [],
  // Binding
  nodeBinding: {},
  linkBinding: {},
  // Qidiruv
  searchFields: null,
  searchDisplayField: null,
  // Edit forma
  editForm: {
    readOnly: !1,
    titleBinding: "name",
    photoBinding: "img",
    elements: [],
    buttons: {
      edit: { text: "Tahrirlash", hideIfEditMode: !0 },
      details: { text: "Batafsil" },
      remove: { text: "O'chirish", hideIfDetailsMode: !0 }
    }
  },
  // Collapse/Expand
  collapse: {},
  expand: {},
  // Misc
  align: L.center,
  orderBy: null,
  roots: null,
  nodeMenu: null,
  nodeTreeMenu: !0,
  toolbar: !1,
  miniMap: !1,
  lazyLoading: !0
};
class P {
  constructor(t, e = 0) {
    this.id = t.id, this.data = t, this.level = e, this.prelim = 0, this.modifier = 0, this.shift = 0, this.change = 0, this.thread = null, this.ancestor = this, this.parent = null, this.children = [], this.partnerOf = null, this.isPartner = !1, this.x = 0, this.y = 0, this.w = 0, this.h = 0, this.leftNeighbor = null, this.rightNeighbor = null, this.collapsed = !1;
  }
  /** Birinchi farzand */
  get firstChild() {
    return this.children[0] || null;
  }
  /** Oxirgi farzand */
  get lastChild() {
    return this.children[this.children.length - 1] || null;
  }
  /** Chap aka-uka (siblings ichida) */
  get leftSibling() {
    if (!this.parent) return null;
    const t = this.parent.children.indexOf(this);
    return t > 0 ? this.parent.children[t - 1] : null;
  }
  /** O'ng aka-uka */
  get rightSibling() {
    if (!this.parent) return null;
    const t = this.parent.children.indexOf(this);
    return t < this.parent.children.length - 1 ? this.parent.children[t + 1] : null;
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
class E {
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
  constructor(t, e = 140, i = 60) {
    this.levelSep = t.levelSeparation ?? 60, this.siblingSep = t.siblingSeparation ?? 35, this.subtreeSep = t.subtreeSeparation ?? 40, this.partnerSep = t.partnerNodeSeparation ?? 15, this.orientation = t.orientation ?? _.top, this.layout = t.layout ?? $.normal, this.nodeW = e, this.nodeH = i;
  }
  /**
   * Berilgan tugun ro'yxatini layout qiladi
   * @param {object[]} nodesData - { id, fid?, mid?, pids?, tags?, ... }
   * @param {object}   sizes     - { [id]: {w, h} } — har node uchun o'lcham
   * @returns {Map<id, {x,y,w,h,level}>}
   */
  compute(t, e = {}) {
    if (!t || t.length === 0) return /* @__PURE__ */ new Map();
    const i = /* @__PURE__ */ new Map();
    for (const c of t) {
      const a = new P(c), l = e[c.id] || { w: this.nodeW, h: this.nodeH };
      a.w = l.w, a.h = l.h, i.set(c.id, a);
    }
    for (const c of t)
      if (c.fid && c.mid) {
        const a = i.get(c.fid), l = i.get(c.mid);
        a && l && (a.data.pids = a.data.pids || [], l.data.pids = l.data.pids || [], a.data.pids.includes(c.mid) || a.data.pids.push(c.mid), l.data.pids.includes(c.fid) || l.data.pids.push(c.fid));
      }
    const s = (c) => !!(c.fid || c.mid), n = /* @__PURE__ */ new Set(), r = [];
    for (const [c, a] of i) {
      if (a.isPartner) continue;
      const l = a.data.pids;
      if (l && l.length > 0) {
        let h = a;
        for (const w of l) {
          const u = i.get(w);
          u && s(u.data) && !s(h.data) && (h = u);
        }
        if (h !== a && !h.isPartner)
          continue;
        let y = h.w;
        for (const w of h.data.pids) {
          const u = i.get(w);
          if (!(!u || n.has(u.id) || u === h)) {
            if (s(h.data) && s(u.data)) {
              r.push({ left: u, right: h }), n.add(u.id);
              continue;
            }
            u.isPartner = !0, u.partnerOf = h, y += u.w + this.partnerSep, n.add(u.id);
          }
        }
        h.effectiveW = y, h.originalW = h.w, h.w = y;
      } else
        a.effectiveW = a.w, a.originalW = a.w;
    }
    for (const c of t) {
      const a = i.get(c.id);
      let l = c.fid;
      if ((!l || !i.has(l)) && (l = c.mid), l && i.has(l)) {
        let h = i.get(l);
        h.isPartner && h.partnerOf && (h = h.partnerOf), h.children.includes(a) || (h.children.push(a), a.parent || (a.parent = h));
      }
    }
    const o = /* @__PURE__ */ new Set(), d = (c, a) => {
      if (!o.has(c.id)) {
        o.add(c.id), c.level = a;
        for (const l of c.children) d(l, a + 1);
      }
    }, f = [];
    for (const [c, a] of i)
      !a.parent && !a.isPartner && (f.push(a), d(a, 0));
    const p = /* @__PURE__ */ new Map();
    if (f.length > 0) {
      let c = 0;
      for (let a = 0; a < f.length; a++) {
        const l = f[a];
        this._firstWalk(l), this._secondWalk(l, -l.prelim, 0);
        const h = this._bounds(l);
        this._shift(l, c - h.minX), c += h.maxX - h.minX + this.subtreeSep;
      }
      for (const [a, l] of i) {
        if (l.isPartner) continue;
        l.originalW && (l.w = l.originalW);
        const h = l.data.pids;
        if (!h || h.length === 0) continue;
        let y = l.w + this.partnerSep;
        for (const w of h) {
          const u = i.get(w);
          !u || u.partnerOf !== l || (u.y = l.y, u.x = l.x + y, y += u.w + this.partnerSep);
        }
      }
      for (const a of r) {
        let { left: l, right: h } = a;
        if (l.x > h.x) {
          const m = l;
          l = h, h = m;
        }
        const w = l.x + l.w + this.partnerSep - h.x;
        let u = h;
        for (; u.parent; ) u = u.parent;
        for (const m of f)
          m.x >= u.x && this._shift(m, w);
        const S = l.children.filter((m) => m.data.fid === h.id || m.data.mid === h.id);
        for (const m of S)
          this._shift(m, Math.abs(h.w + this.partnerSep) / 2);
        const M = h.children.filter((m) => m.data.fid === l.id || m.data.mid === l.id);
        for (const m of M)
          this._shift(m, -Math.abs(l.w + this.partnerSep) / 2);
      }
      this._applyOrientation(i);
      for (const [a, l] of i)
        p.set(a, {
          data: l.data,
          partnerOf: l.partnerOf ? l.partnerOf.id : null,
          // ID ni saqlash
          x: Math.round(l.x),
          y: Math.round(l.y),
          w: l.w,
          h: l.h,
          level: l.level,
          isPartner: l.isPartner
        });
    }
    return p;
  }
  _findRoots(t) {
    return [...t.values()].filter((e) => !e.parent && !e.isPartner);
  }
  // ─── 1-bosqich: firstWalk (pastdan yuqoriga) ──────────────────────────
  /**
   * Reingold-Tilford firstWalk
   * Har bir tugun uchun prelim (dastlabki x) hisoblaydi
   */
  _firstWalk(t) {
    if (t.children.length === 0) {
      const n = t.leftSibling;
      n ? t.prelim = n.prelim + n.w + this.siblingSep : t.prelim = 0;
      return;
    }
    let e = t.firstChild;
    for (const n of t.children)
      this._firstWalk(n), e = this._apportion(n, e);
    this._executeShifts(t);
    const i = (t.firstChild.prelim + t.lastChild.prelim + t.lastChild.w) / 2 - t.w / 2, s = t.leftSibling;
    s ? (t.prelim = s.prelim + s.w + this.siblingSep, t.modifier = t.prelim - i) : t.prelim = i;
  }
  /**
   * apportion — aka-ukalar orasidagi masofani tenglashtirish
   * Bu Reingold-Tilford algoritmining eng muhim qismi
   */
  _apportion(t, e) {
    const i = t.leftSibling;
    if (!i) return e;
    let s = t, n = t, r = i, o = t.parent.firstChild, d = s.modifier, f = n.modifier, p = r.modifier, c = o.modifier, a = this._nextRight(r), l = this._nextLeft(s);
    for (; a && l; ) {
      r = a, s = l, o = this._nextLeft(o), n = this._nextRight(n), n.ancestor = t;
      const h = r.prelim + p - (s.prelim + d) + r.w + this.siblingSep;
      if (h > 0) {
        const y = this._ancestor(r, t, e);
        this._moveSubtree(y, t, h), d += h, f += h;
      }
      p += r.modifier, d += s.modifier, c += o ? o.modifier : 0, f += n.modifier, a = this._nextRight(r), l = this._nextLeft(s);
    }
    return a && !this._nextRight(n) && (n.thread = a, n.modifier += p - f), l && !this._nextLeft(o) && (o && (o.thread = l, o.modifier += d - c), e = t), e;
  }
  _nextLeft(t) {
    return t.children.length > 0 ? t.firstChild : t.thread;
  }
  _nextRight(t) {
    return t.children.length > 0 ? t.lastChild : t.thread;
  }
  _ancestor(t, e, i) {
    return e.parent && e.parent.children.includes(t.ancestor) ? t.ancestor : i;
  }
  /**
   * moveSubtree — subtreeni o'ngga shift qilish
   * Bu O(n) ga erishishga yordam beradi
   */
  _moveSubtree(t, e, i) {
    const s = e.parent;
    if (!s) return;
    const n = s.children.indexOf(e) - s.children.indexOf(t);
    n !== 0 && (e.change -= i / n, e.shift += i, t.change += i / n, e.prelim += i, e.modifier += i);
  }
  /**
   * executeShifts — yig'ilgan shift va change larni qo'llash
   */
  _executeShifts(t) {
    let e = 0, i = 0;
    for (let s = t.children.length - 1; s >= 0; s--) {
      const n = t.children[s];
      n.prelim += e, n.modifier += e, i += n.change, e += n.shift + i;
    }
  }
  // ─── 2-bosqich: secondWalk (yuqoridan pastga) ─────────────────────────
  /**
   * Haqiqiy x, y koordinatalarni belgilash
   * m = yig'ilgan modifier qiymati
   * depth = joriy daraja
   */
  _secondWalk(t, e, i) {
    t.x = t.prelim + e, t.y = i * (this.nodeH + this.levelSep), t.modifier = t.modifier || 0;
    for (const s of t.children)
      this._secondWalk(s, e + t.modifier, i + 1);
  }
  // ─── Partner joylashuvi ────────────────────────────────────────────────
  /**
   * pids[] bo'yicha turmush o'rtoqlarni yonga joylashtirish
   * Juft node → asosiy nodning o'ng tomoniga qo'yiladi
   */
  _placePartners(t) {
    const e = /* @__PURE__ */ new Set();
    for (const [i, s] of t) {
      const n = s.data.pids;
      if (!(!n || n.length === 0))
        for (const r of n) {
          const o = [i, r].sort().join("-");
          if (e.has(o)) continue;
          e.add(o);
          const d = t.get(r);
          d && (d.isPartner = !0, d.partnerOf = s, d.y = s.y, d.x = s.x + s.w + this.partnerSep, this._shiftRightOf(t, s, d.w + this.partnerSep));
        }
    }
  }
  /**
   * Berilgan noddan o'ngdagi barcha nodelarni siljitish
   */
  _shiftRightOf(t, e, i) {
    for (const [, s] of t)
      s === e || s.isPartner || s.x > e.x && Math.abs(s.y - e.y) < this.levelSep && (s.x += i, this._shiftSubtree(s, i));
  }
  _shiftSubtree(t, e) {
    for (const i of t.children)
      i.x += e, this._shiftSubtree(i, e);
  }
  // ─── Yo'nalish ────────────────────────────────────────────────────────
  /**
   * Yo'nalish asosida x,y larni aylantirish
   */
  _applyOrientation(t) {
    if (this.orientation === _.top) return;
    let e = 1 / 0, i = 1 / 0, s = -1 / 0, n = -1 / 0;
    for (const [, d] of t)
      e = Math.min(e, d.x), i = Math.min(i, d.y), s = Math.max(s, d.x + d.w), n = Math.max(n, d.y + d.h);
    const r = (e + s) / 2, o = (i + n) / 2;
    for (const [, d] of t) {
      const f = d.x - r, p = d.y - o;
      switch (this.orientation) {
        case _.bottom:
          d.y = o - p;
          break;
        case _.right:
          [d.x, d.y] = [o + p, r - f], [d.w, d.h] = [d.h, d.w];
          break;
        case _.left:
          [d.x, d.y] = [o - p, r + f], [d.w, d.h] = [d.h, d.w];
          break;
      }
    }
  }
  // ─── Yordamchi ──────────────────────────────────────────────────────
  _bounds(t) {
    let e = t.x, i = t.x + t.w;
    const s = (n) => {
      e = Math.min(e, n.x), i = Math.max(i, n.x + n.w), n.children.forEach(s);
    };
    return s(t), { minX: e, maxX: i };
  }
  _shift(t, e) {
    t.x += e, t.children.forEach((i) => this._shift(i, e));
  }
}
class C {
  constructor(t, e = 140, i = 60) {
    this.columns = t.columns ?? 4, this.padH = t.siblingSeparation ?? 35, this.padV = t.levelSeparation ?? 60, this.nodeW = e, this.nodeH = i;
  }
  compute(t, e = {}) {
    const i = /* @__PURE__ */ new Map(), s = this.columns;
    return t.forEach((n, r) => {
      const o = e[n.id] || { w: this.nodeW, h: this.nodeH }, d = Math.floor(r / s), f = r % s;
      i.set(n.id, {
        data: n,
        partnerOf: null,
        x: f * (o.w + this.padH),
        y: d * (o.h + this.padV),
        w: o.w,
        h: o.h,
        level: d,
        isPartner: !1
      });
    }), i;
  }
}
function A(g, t, e) {
  return g.layout === $.grid || g.layout === -1 ? new C(g, t, e) : new E(g, t, e);
}
class O {
  constructor(t, e) {
    this.container = typeof t == "string" ? document.querySelector(t) : t, this.config = e, this.svg = null, this.defs = null, this.linksGroup = null, this.nodesGroup = null, this._init();
  }
  _init() {
    this.container.innerHTML = "", this.container.style.position = "relative", this.container.style.overflow = "hidden", this.svg = document.createElementNS("http://www.w3.org/2000/svg", "svg"), this.svg.style.width = "100%", this.svg.style.height = "100%", this.svg.style.cursor = "grab", this.defs = document.createElementNS("http://www.w3.org/2000/svg", "defs"), this.linksGroup = document.createElementNS("http://www.w3.org/2000/svg", "g"), this.nodesGroup = document.createElementNS("http://www.w3.org/2000/svg", "g"), this.linksGroup.classList.add("bft-links"), this.nodesGroup.classList.add("bft-nodes"), this.svg.appendChild(this.defs), this.svg.appendChild(this.linksGroup), this.svg.appendChild(this.nodesGroup), this.container.appendChild(this.svg), this.transform = { x: 0, y: 0, scale: this.config.scaleInitial || 1 }, this.isDragging = !1, this.dragStart = { x: 0, y: 0 }, this._setupPanZoom();
  }
  /**
   * ViewBoxni yangilash
   */
  updateViewBox() {
    const { x: t, y: e, scale: i } = this.transform, s = this.container.clientWidth / i, n = this.container.clientHeight / i;
    this.svg.setAttribute("viewBox", `${-t / i} ${-e / i} ${s} ${n}`);
  }
  _setupPanZoom() {
    this.container.addEventListener("wheel", (t) => {
      if (!this.config.enablePan && this.config.mouseScrool === "none") return;
      t.preventDefault();
      const e = 1.1, i = t.deltaY < 0 ? 1 : -1;
      let s = this.transform.scale * (i > 0 ? e : 1 / e);
      s = Math.max(this.config.scaleMin, Math.min(this.config.scaleMax, s));
      const n = this.container.getBoundingClientRect(), r = t.clientX - n.left, o = t.clientY - n.top, d = (r - this.transform.x) / this.transform.scale, f = (o - this.transform.y) / this.transform.scale;
      this.transform.x = r - d * s, this.transform.y = o - f * s, this.transform.scale = s, this.updateViewBox();
    }), this.svg.addEventListener("mousedown", (t) => {
      this.config.enablePan && (this.isDragging = !0, this.dragStart.x = t.clientX - this.transform.x, this.dragStart.y = t.clientY - this.transform.y, this.svg.style.cursor = "grabbing");
    }), window.addEventListener("mousemove", (t) => {
      this.isDragging && (this.transform.x = t.clientX - this.dragStart.x, this.transform.y = t.clientY - this.dragStart.y, this.updateViewBox());
    }), window.addEventListener("mouseup", () => {
      this.isDragging && (this.isDragging = !1, this.svg.style.cursor = "grab");
    });
  }
  /**
   * Barcha render ishlarni tozalash (update dan oldin)
   */
  clear() {
    this.linksGroup.innerHTML = "", this.nodesGroup.innerHTML = "";
  }
  /**
   * Burchaklarni yumalatuvchi maxsus algoritm
   * FamilyTreeJS roundPathCorners funksiyasi yordamida
   */
  _roundPathCorners(t, e) {
    const i = t.split(/(?=[MmLlHhVvCcsSzZ])/g);
    let s = "";
    if (i.length <= 2) return t;
    s += i[0];
    for (let n = 1; n < i.length - 1; n++)
      s += i[n];
    return s += i[i.length - 1], t;
  }
  /**
  /**
   * Yaponcha qavslarga o'xshagan silliq (rounded) filialli yo'lni hisoblaydi
   */
  _calculateLinkPath(t, e, i, s) {
    const n = e + (s - e) / 2, r = 10;
    if (Math.abs(t - i) < 1)
      return `M ${t} ${e} L ${i} ${s}`;
    const o = t < i ? 1 : -1, d = Math.min(r, Math.abs(n - e), Math.abs(i - t) / 2);
    return `M ${t} ${e} 
            L ${t} ${n - d} 
            A ${d} ${d} 0 0 ${o === 1 ? 0 : 1} ${t + d * o} ${n} 
            L ${i - d * o} ${n} 
            A ${d} ${d} 0 0 ${o === 1 ? 1 : 0} ${i} ${n + d} 
            L ${i} ${s}`;
  }
  /**
   * DOM ga HTML/SVG elementlarini qo'shish
   */
  drawLinks(t) {
    let e = "";
    for (const [i, s] of t) {
      const n = s.data;
      if (n.pids && n.pids.length > 0)
        for (const r of n.pids) {
          const o = t.get(r);
          if (o && o.x > s.x) {
            const d = s.x + s.w, f = s.y + s.h / 2, p = o.x, c = o.y + o.h / 2, a = `M ${d} ${f} L ${p} ${c}`;
            e += `<path d="${a}" stroke="#7B8290" stroke-width="2" fill="none" class="bft-link-partner" />`;
          }
        }
      if (n.fid || n.mid) {
        let r, o;
        const d = n.fid ? t.get(n.fid) : null, f = n.mid ? t.get(n.mid) : null;
        if (d && f) {
          const p = d.x < f.x ? d : f, c = d.x < f.x ? f : d;
          r = p.x + p.w + (c.x - (p.x + p.w)) / 2, o = p.y + p.h / 2;
        } else d ? (r = d.x + d.w / 2, o = d.y + d.h) : f && (r = f.x + f.w / 2, o = f.y + f.h);
        if (r !== void 0) {
          const p = s.x + s.w / 2, c = s.y, a = this._calculateLinkPath(r, o, p, c);
          e += `<path d="${a}" stroke="#7B8290" stroke-width="2" fill="none" class="bft-link" />`;
        }
      }
    }
    this.linksGroup.innerHTML = e;
  }
  /**
   * Node larni qo'shish (shablonlar orqali)
   */
  drawNodes(t, e) {
    let i = "";
    for (const [s, n] of t) {
      const { x: r, y: o, w: d, h: f } = n, p = e(n.data, d, f);
      i += `
        <g data-n-id="${s}" class="bft-node" transform="translate(${r}, ${o})">
          ${p}
        </g>
      `;
    }
    this.nodesGroup.innerHTML = i;
  }
  /**
   * Daraxt markazlash va to'g'ri o'lchamlarga keltirish
   */
  fitView(t) {
    let e = 1 / 0, i = 1 / 0, s = -1 / 0, n = -1 / 0;
    for (const [, l] of t)
      e = Math.min(e, l.x), i = Math.min(i, l.y), s = Math.max(s, l.x + l.w), n = Math.max(n, l.y + l.h);
    if (e === 1 / 0) return;
    const r = this.container.clientWidth, o = this.container.clientHeight, d = s - e + this.config.padding * 2, f = n - i + this.config.padding * 2;
    let p = Math.min(r / d, o / f);
    p = Math.max(this.config.scaleMin, Math.min(this.config.scaleMax, p));
    const c = (e + s) / 2, a = (i + n) / 2;
    this.transform.scale = p, this.transform.x = r / 2 - c * p, this.transform.y = o / 2 - a * p, this.updateViewBox();
  }
}
class B {
  constructor() {
    this.listeners = {};
  }
  /**
   * Hodisaga obuna bo'lish
   * @param {string} event 
   * @param {Function} callback 
   */
  on(t, e) {
    return this.listeners[t] || (this.listeners[t] = []), this.listeners[t].push(e), this;
  }
  /**
   * Hodisani bekor qilish
   * @param {string} event 
   * @param {Function} callback 
   */
  off(t, e) {
    this.listeners[t] && (this.listeners[t] = this.listeners[t].filter((i) => i !== e));
  }
  /**
   * Hodisani ishga tushirish (trigger)
   * FamilyTreeJS on('update', function(sender, args) { ... }) shaklida kutadi
   * Return false qilinganda, bekor qilish imkoniyati ham bor.
   */
  emit(t, e, ...i) {
    if (!this.listeners[t]) return !0;
    let s = !0;
    for (const n of this.listeners[t])
      n(e, ...i) === !1 && (s = !1);
    return s;
  }
}
class N {
  constructor(t, e = {}) {
    this.element = typeof t == "string" ? document.querySelector(t) : t, this.config = { ...v, ...e }, e.anim && (this.config.anim = { ...v.anim, ...e.anim }), this.config.nodes = this.config.nodes || [], this.nodeWidth = 240, this.nodeHeight = 100, this.templateRenderer = this._defaultTemplate, this.renderer = new O(this.element, this.config), this.layoutEngine = A(this.config, this.nodeWidth, this.nodeHeight), this._emitter = new B(), this._idIndex = 0, this._selectedNodeId = null, this._initMenu(), this.config.nodes.length > 0 && this.draw();
  }
  // ============== VOQEALAR BOSHQRARUVI (EVENTS) ==============
  on(t, e) {
    return this._emitter.on(t, e), this;
  }
  removeListener(t, e) {
    return this._emitter.off(t, e), this;
  }
  // ============== ASOSIY RENDERING API ==============
  load(t, e) {
    this.config.nodes = t || [], this.draw(x.init), typeof e == "function" && e();
  }
  draw(t = x.update, e = {}, i) {
    const s = JSON.parse(JSON.stringify(this.config.nodes || []));
    if (this.config.nodeTreeMenu && this._selectedNodeId) {
      const r = s.find((o) => o.id == this._selectedNodeId);
      r && (s.push({ id: "temp_add_spouse", isPlaceholder: !0, placeholderAction: "spouse", targetId: r.id, pids: [r.id], tags: ["placeholder"] }), s.push({ id: "temp_add_son", isPlaceholder: !0, placeholderAction: "son", targetId: r.id, fid: r.id, tags: ["placeholder"] }), s.push({ id: "temp_add_daughter", isPlaceholder: !0, placeholderAction: "daughter", targetId: r.id, fid: r.id, tags: ["placeholder"] }), !r.fid && !r.mid && (s.push({ id: "temp_add_parent", isPlaceholder: !0, placeholderAction: "parent", targetId: r.id, tags: ["placeholder"] }), r.fid = "temp_add_parent"));
    }
    const n = this.layoutEngine.compute(s);
    this.renderer.clear(), this.renderer.drawLinks(n), this.renderer.drawNodes(n, (r, o, d) => r.isPlaceholder ? this._placeholderTemplate(r, o, d) : this.templateRenderer(r, o, d)), t === x.init && this.renderer.fitView(n), typeof i == "function" && i();
  }
  setTemplateRenderer(t) {
    this.templateRenderer = t;
  }
  _initMenu() {
    this.config.nodeMenu && (this.menuElement = document.createElement("div"), this.menuElement.style.cssText = `
       display: none; position: fixed; background: #262626; border-radius: 6px;
       box-shadow: 0 4px 15px rgba(0,0,0,0.5); z-index: 9999; flex-direction: column; overflow: hidden;
       min-width: 160px; font-family: 'Inter', Arial, sans-serif;
    `, document.body.appendChild(this.menuElement), document.addEventListener("click", () => {
      this.menuElement.style.display = "none";
    }), this.renderer.container.addEventListener("click", (t) => {
      const e = t.target.closest(".bft-menu-btn");
      if (e) {
        t.stopPropagation(), this._showMenu(t, e.getAttribute("data-id"));
        return;
      }
      const i = t.target.closest(".bft-plus-btn");
      if (i) {
        t.stopPropagation();
        const n = i.getAttribute("data-id");
        this._selectedNodeId = this._selectedNodeId === n ? null : n, this.draw(x.update);
        return;
      }
      const s = t.target.closest(".bft-placeholder");
      if (s) {
        t.stopPropagation();
        const n = s.getAttribute("data-action"), r = s.getAttribute("data-target");
        this._handlePlaceholderClick(n, r);
        return;
      }
      t.target.closest(".node") || this._selectedNodeId && (this._selectedNodeId = null, this.draw(x.update));
    }));
  }
  _handlePlaceholderClick(t, e) {
    const i = this.generateId();
    let s = null;
    const n = this.get(e);
    if (n) {
      if (t === "spouse") {
        const r = n.pids || [];
        s = {
          addNodesData: [{ id: i, pids: [e] }],
          updateNodesData: [{ id: e, pids: [...r, i] }],
          removeNodeId: null
        };
      } else if (t === "son" || t === "daughter") {
        let r = n.tags && n.tags.includes("female") ? e : n.pids && n.pids[0], o = n.tags && n.tags.includes("female") ? n.pids && n.pids[0] : e;
        s = {
          addNodesData: [{ id: i, fid: o || e, mid: r || null }],
          updateNodesData: [],
          removeNodeId: null
        };
      } else t === "parent" && (s = {
        addNodesData: [{ id: i }],
        updateNodesData: [{ id: e, fid: i }],
        removeNodeId: null
      });
      this._selectedNodeId = null, s && this._emitter.emit("update", this, s), this.draw(x.update);
    }
  }
  _showMenu(t, e) {
    this.menuElement.innerHTML = "";
    for (const n in this.config.nodeMenu) {
      const r = this.config.nodeMenu[n], o = document.createElement("div");
      o.style.cssText = "padding: 10px 16px; color: #ddd; cursor: pointer; font-size: 14px; border-bottom: 1px solid #363636; display: flex; align-items: center; gap: 8px; transition: background 0.2s;", o.onmouseover = () => o.style.background = "#363636", o.onmouseout = () => o.style.background = "transparent", o.innerHTML = r.text || n, o.onclick = (d) => {
        d.stopPropagation(), this.menuElement.style.display = "none", r.onClick && r.onClick(e);
      }, this.menuElement.appendChild(o);
    }
    this.renderer.container.getBoundingClientRect();
    let i = t.clientX, s = t.clientY + 10;
    this.menuElement.style.left = i + "px", this.menuElement.style.top = s + "px", this.menuElement.style.display = "flex";
  }
  _defaultTemplate(t, e, i) {
    const n = (t.tags ? t.tags.includes("female") : !1) ? "#F57C00" : "#039BE5";
    let r = "";
    if (t.photoUrl || t.role === "CENTER" || t.roleLabel && t.roleLabel.includes("Asosiy")) {
      const o = t.photoUrl || "";
      r = `
         <circle cx="${e - 20}" cy="20" r="28" fill="#1b1b1b" stroke="#fff" stroke-width="2"/>
         <clipPath id="cp_${t.id}">
            <circle cx="${e - 20}" cy="20" r="26" />
         </clipPath>
         ${o ? `<image href="${o}" x="${e - 46}" y="-6" width="52" height="52" clip-path="url(#cp_${t.id})" preserveAspectRatio="xMidYMid slice" />` : `<svg x="${e - 36}" y="4" width="32" height="32" viewBox="0 0 24 24" fill="#a0a0a0"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>`}
       `;
    }
    return `
      <rect x="0" y="0" width="${e}" height="${i}" rx="8" fill="${n}" stroke="rgba(255,255,255,0.1)" stroke-width="1"></rect>
      
      <!-- Role / Title with Icon -->
      <g transform="translate(14, 12)">
         <svg width="18" height="18" viewBox="0 0 24 24" fill="#fff" opacity="0.9"><path d="M15 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm-9-2V7H4v3H1v2h3v3h2v-3h3v-2H6zm9 4c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
      </g>
      <text x="38" y="26" fill="#ffffff" font-size="13" font-family="'Inter', Arial, sans-serif" opacity="0.9">${t.roleLabel || ""}</text>
      
      <!-- Name -->
      <text x="14" y="58" fill="#ffffff" font-size="17" font-weight="bold" font-family="'Inter', Arial, sans-serif">${t.name || ""}</text>
      
      <!-- Years -->
      <text x="14" y="82" fill="#ffffff" font-size="13" font-family="'Inter', Arial, sans-serif" opacity="0.8">${t.years || ""}</text>
      
      ${r}

      <!-- Menu Button (Three dots) -->
      <g class="bft-menu-btn" transform="translate(${e - 20}, ${i - 15})" style="cursor:pointer;" data-id="${t.id}">
         <circle cx="-6" cy="0" r="1.5" fill="#fff" opacity="0.7"/>
         <circle cx="0" cy="0" r="1.5" fill="#fff" opacity="0.7"/>
         <circle cx="6" cy="0" r="1.5" fill="#fff" opacity="0.7"/>
         <!-- hit area -->
         <rect x="-15" y="-10" width="30" height="20" fill="transparent" />
      <!-- Plus Button for Tooltip (nodeTreeMenu) -->
      ${this.config.nodeTreeMenu ? `
      <g class="bft-plus-btn" transform="translate(${e / 2}, 0)" style="cursor:pointer;" data-id="${t.id}">
         <circle cx="0" cy="0" r="12" fill="#2d2d2d" stroke="#ffffff" stroke-width="2"/>
         <line x1="-5" y1="0" x2="5" y2="0" stroke="#fff" stroke-width="2"/>
         <line x1="0" y1="-5" x2="0" y2="5" stroke="#fff" stroke-width="2"/>
         <rect x="-12" y="-12" width="24" height="24" fill="transparent"/>
      </g>` : ""}
    `;
  }
  _placeholderTemplate(t, e, i) {
    let s = "ADD";
    t.placeholderAction === "spouse" && (s = "Add partner"), t.placeholderAction === "son" && (s = "Add son"), t.placeholderAction === "daughter" && (s = "Add daughter"), t.placeholderAction === "parent" && (s = "Add parent");
    const r = t.placeholderAction === "son" || t.placeholderAction === "parent" ? "#039BE5" : "#F57C00";
    return `
        <!-- Border Outline -->
        <rect x="0" y="0" width="${e}" height="${i}" rx="8" fill="rgba(255,255,255,0.02)" stroke="${r}" stroke-width="1.5" stroke-dasharray="0" class="bft-placeholder" data-action="${t.placeholderAction}" data-target="${t.targetId}" style="cursor:pointer;"></rect>
        
        <text x="${e - 20}" y="30" fill="${r}" font-size="16" font-family="'Inter', Arial" text-anchor="end" style="pointer-events:none;">${s}</text>
        
        <g transform="translate(40, 30) scale(1.6)">
           <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="${r}" stroke-width="1.2"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
        </g>
      `;
  }
  // ============== ZOOM & PAN ==============
  zoom(t) {
    const e = new WheelEvent("wheel", {
      deltaY: t ? -100 : 100,
      clientX: document.body.clientWidth / 2,
      clientY: document.body.clientHeight / 2
    });
    this.renderer.container.dispatchEvent(e);
  }
  fit() {
    const t = this.layoutEngine.compute(this.config.nodes);
    this.renderer.fitView(t);
  }
  // ============== ICHKI OBJECT MANAGMENT ==============
  get(t) {
    const e = this.config.nodes.find((i) => i.id == t);
    return e ? JSON.parse(JSON.stringify(e)) : null;
  }
  getNode(t) {
    return this.config.nodes.find((e) => e.id == t);
  }
  add(t) {
    return this.config.nodes.push(t), this;
  }
  update(t) {
    const e = this.config.nodes.findIndex((i) => i.id == t.id);
    return e !== -1 && (this.config.nodes[e] = { ...this.config.nodes[e], ...t }), this;
  }
  remove(t) {
    this.config.nodes = this.config.nodes.filter((e) => e.id != t);
    for (let e of this.config.nodes)
      e.fid == t && delete e.fid, e.mid == t && delete e.mid, e.pids && e.pids.includes(t) && (e.pids = e.pids.filter((i) => i != t));
    return this;
  }
  generateId() {
    return "_" + Math.random().toString(36).substr(2, 9);
  }
  // ============== API METHODS (Add/Update/Remove) ==============
  updateNode(t, e, i = !0) {
    const s = { addNodesData: [], updateNodesData: [t], removeNodeId: null };
    if (i && this._emitter.emit("update", this, s) === !1) return !1;
    this.update(t), this.draw(x.update, {}, e);
  }
  removeNode(t, e, i = !0) {
    const s = { addNodesData: [], updateNodesData: [], removeNodeId: t };
    if (i && this._emitter.emit("update", this, s) === !1) return !1;
    this.remove(t), this.draw(x.update, {}, e);
  }
  addNode(t, e, i = !0) {
    const s = { addNodesData: [t], updateNodesData: [], removeNodeId: null };
    if (i && this._emitter.emit("update", this, s) === !1) return !1;
    this.add(t), this.draw(x.insert, {}, e);
  }
  addChildNode(t, e, i = !0) {
    t.id == null && (t.id = this.generateId()), this.addNode(t, e, i);
  }
  addChildAndPartnerNodes(t, e, i, s, n = !0) {
    e.id == null && (e.id = this.generateId()), i.id == null && (i.id = this.generateId());
    const r = this.get(t);
    i.pids = i.pids || [], i.pids.includes(t) || i.pids.push(t), r.pids = r.pids || [], r.pids.includes(i.id) || r.pids.push(i.id), e.fid = t, e.mid = i.id;
    const o = {
      addNodesData: [e, i],
      updateNodesData: [r],
      removeNodeId: null
    };
    if (n && this._emitter.emit("update", this, o) === !1) return !1;
    this.add(e).add(i).update(r), this.draw(x.update, {}, s);
  }
  addPartnerNode(t, e, i = !0) {
    t.id == null && (t.id = this.generateId());
    const s = t.pids && t.pids[0], n = s ? this.get(s) : null, r = [];
    n && (n.pids = n.pids || [], n.pids.includes(t.id) || n.pids.push(t.id), r.push(n));
    const o = { addNodesData: [t], updateNodesData: r, removeNodeId: null };
    if (i && this._emitter.emit("update", this, o) === !1) return !1;
    this.add(t), n && this.update(n), this.draw(x.insert, {}, e);
  }
  addParentNode(t, e, i, s, n = !0) {
    i.id == null && (i.id = this.generateId());
    const r = this.get(t);
    if (!r) return !1;
    r[e] = i.id;
    const o = { addNodesData: [i], updateNodesData: [r], removeNodeId: null };
    if (n && this._emitter.emit("update", this, o) === !1) return !1;
    this.add(i), this.update(r), this.draw(x.insert, {}, s);
  }
}
b(N, "action", x), b(N, "layout", $), b(N, "orientation", _), b(N, "icon", {
  edit: (t, e, i) => `<svg width="${t}" height="${e}" fill="${i}"><path d=""/></svg>`,
  details: (t, e, i) => `<svg width="${t}" height="${e}" fill="${i}"><path d=""/></svg>`,
  remove: (t, e, i) => `<svg width="${t}" height="${e}" fill="${i}"><path d=""/></svg>`
}), b(N, "templates", {
  tommy: { node: "", plus: "", minus: "" }
});
typeof window < "u" && (window.FamilyTree = N);
export {
  N as FamilyTree
};
