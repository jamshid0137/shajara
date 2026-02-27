/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║  ShTree.js  v1.0                                                ║
 * ║  Shajara loyihasiga maxsus yozilgan oila daraxti kutubxonasi    ║
 * ║  Muallif: loyiha uchun moslashtirilgan (FamilyTree JS arxitektu-║
 * ║  rasidan ilhomlangan, lekin to'liq mustaqil kod)                ║
 * ╚══════════════════════════════════════════════════════════════════╝
 *
 * ISHLATISH:
 *   const tree = new ShTree('#tree', config);
 *   tree.load(apiResponseJson);       // backend JSON → render
 *   tree.on('nodeClick', fn);
 *   tree.fit();
 *   tree.center(personId);
 */

(function (global) {
    'use strict';

    // ════════════════════════════════════════════════════
    //  UTILS
    // ════════════════════════════════════════════════════
    var ShUtil = {
        /** Matnni qisqartirish */
        trunc: function (str, max) {
            if (!str) return '—';
            return str.length > max ? str.slice(0, max) + '…' : str;
        },
        /** LocalDate massivini formatlash: [2000,1,5] → "05.01.2000" */
        fmtDate: function (d) {
            if (!d) return '';
            if (typeof d === 'string') {
                try { d = new Date(d); } catch (e) { return d; }
                return d.toLocaleDateString('uz-UZ');
            }
            // Java LocalDate → [year, month, day]
            if (Array.isArray(d)) {
                var y = d[0], m = String(d[1]).padStart(2, '0'), day = String(d[2]).padStart(2, '0');
                return day + '.' + m + '.' + y;
            }
            return '';
        },
        /** SVG namespace element yaratish */
        svgEl: function (tag, attrs) {
            var el = document.createElementNS('http://www.w3.org/2000/svg', tag);
            for (var k in attrs) el.setAttribute(k, attrs[k]);
            return el;
        },
        /** HTML element yaratish */
        div: function (cls, html) {
            var el = document.createElement('div');
            if (cls) el.className = cls;
            if (html !== undefined) el.innerHTML = html;
            return el;
        }
    };

    // ════════════════════════════════════════════════════
    //  HODISALAR TIZIMI (EventSystem)
    // ════════════════════════════════════════════════════
    function EventBus() {
        this._map = {};
    }
    EventBus.prototype.on = function (type, fn) {
        if (!this._map[type]) this._map[type] = [];
        this._map[type].push(fn);
        return this;
    };
    EventBus.prototype.off = function (type, fn) {
        if (!this._map[type]) return;
        this._map[type] = this._map[type].filter(function (f) { return f !== fn; });
    };
    EventBus.prototype.emit = function (type, data) {
        var fns = this._map[type] || [];
        for (var i = 0; i < fns.length; i++) fns[i](data);
    };

    // ════════════════════════════════════════════════════
    //  RENDERER  — SVG liniyalar + HTML tugunlar
    // ════════════════════════════════════════════════════
    function Renderer(config) {
        this.cfg = config;
    }

    /** Backend JSON → pozitsiya xaritasi */
    Renderer.prototype.buildPosMap = function (nodes) {
        var cfg = this.cfg;
        var pos = {};
        nodes.forEach(function (n) {
            var w = n.role === 'CENTER' ? cfg.centerNodeW : cfg.nodeW;
            var h = n.role === 'CENTER' ? cfg.centerNodeH : cfg.nodeH;
            var sx = n.x + cfg.offsetX;
            var sy = n.y + cfg.offsetY;
            pos[n.id] = { x: sx, y: sy, w: w, h: h, cx: sx + w / 2, cy: sy + h / 2 };
        });
        return pos;
    };

    /** SVG o'lchamini belgilash */
    Renderer.prototype.sizeSvg = function (svg, data) {
        var cfg = this.cfg;
        var W = Math.abs(data.maxX - data.minX) + cfg.offsetX * 2 + 800;
        var H = Math.abs(data.maxY - data.minY) + cfg.offsetY * 2 + 800;
        svg.setAttribute('width', W);
        svg.setAttribute('height', H);
        svg.style.width = W + 'px';
        svg.style.height = H + 'px';
    };

    /** SVG liniyalarini chizish */
    Renderer.prototype.drawConnections = function (svg, connections, pos) {
        if (!connections) return;
        connections.forEach(function (c, i) {
            var f = pos[c.fromId], t = pos[c.toId];
            if (!f || !t) return;

            var path = ShUtil.svgEl('path', {
                d: Renderer._buildPath(f, t, c.type),
                class: 'sh-conn ' + (c.type === 'SPOUSE' || c.type === 'PARTNER' ? 'sh-conn-spouse' : 'sh-conn-parent')
            });
            path.style.animationDelay = (i * 0.04) + 's';
            svg.appendChild(path);
        });
    };

    /** Bezier yoki to'g'ri chiziq */
    Renderer._buildPath = function (from, to, type) {
        if (type === 'SPOUSE' || type === 'PARTNER') {
            return 'M ' + from.cx + ' ' + from.cy + ' L ' + to.cx + ' ' + to.cy;
        }
        var midY = (from.cy + to.cy) / 2;
        return 'M ' + from.cx + ' ' + from.cy +
            ' C ' + from.cx + ' ' + midY + ',' +
            to.cx + ' ' + midY + ',' +
            to.cx + ' ' + to.cy;
    };

    /** Bir tugun yaratish (HTML div) */
    Renderer.prototype.createNodeEl = function (n, pos, bus, cfg) {
        var p = pos[n.id];
        var el = document.createElement('div');
        el.id = 'sh-node-' + n.id;
        el.className = 'sh-node sh-role-' + n.role +
            (n.gender ? ' sh-gender-' + n.gender : '');
        el.style.left = p.x + 'px';
        el.style.top = p.y + 'px';
        el.style.width = p.w + 'px';
        el.style.height = p.h + 'px';

        // Avatar
        var initial = (n.name || '?').charAt(0).toUpperCase();
        var avatarHtml = n.photoUrl
            ? '<img src="' + n.photoUrl + '" alt="' + (n.name || '') +
            '" onerror="this.parentElement.textContent=\'' + initial + '\'">'
            : initial;

        // Sana
        var dateStr = ShUtil.fmtDate(n.birthDate);
        var diedStr = n.diedDate ? ShUtil.fmtDate(n.diedDate) : '';

        // Node ichki HTML
        el.innerHTML =
            '<div class="sh-avatar">' + avatarHtml + '</div>' +
            '<div class="sh-info">' +
            '  <div class="sh-name">' + ShUtil.trunc(n.name, 18) + '</div>' +
            '  <div class="sh-lbl">' + (cfg.roleLbls[n.role] || n.role) + '</div>' +
            (dateStr ? '  <div class="sh-date">' + dateStr + (diedStr ? ' → ' + diedStr : '') + '</div>' : '') +
            '</div>';

        // Node click → event
        el.addEventListener('click', function (e) {
            if (e.target.closest('.sh-action-btn')) return; // action btn bosilsa o'tkazib yubor
            bus.emit('nodeClick', { node: n, event: e });
        });

        // Tooltip
        el.addEventListener('mouseenter', function (e) { bus.emit('nodeEnter', { node: n, event: e }); });
        el.addEventListener('mousemove', function (e) { bus.emit('nodeMove', { event: e }); });
        el.addEventListener('mouseleave', function () { bus.emit('nodeLeave', {}); });

        // Action tugmalar
        if (cfg.showActionBtns) {
            Renderer._appendActionBtns(el, n, cfg, bus);
        }

        return el;
    };

    /** Hover tugmalari: yuqori(ota qo'sh), chap/o'ng(er-xotin), pastki(bola) */
    Renderer._appendActionBtns = function (el, n, cfg, bus) {
        var btns = [
            { cls: 'top', label: '↑', event: 'addParent', title: "Ota-ona qo'sh" },
            { cls: 'left', label: '+', event: 'addSpouse', title: "Er/Xotin qo'sh (chap)" },
            { cls: 'right', label: '+', event: 'addSpouse', title: "Er/Xotin qo'sh (o'ng)" },
            { cls: 'bottom', label: '↓', event: 'addChild', title: "Farzand qo'sh" }
        ];
        btns.forEach(function (b) {
            var btn = document.createElement('button');
            btn.className = 'sh-action-btn sh-action-' + b.cls;
            btn.textContent = b.label;
            btn.title = b.title;
            btn.addEventListener('click', function (e) {
                e.stopPropagation();
                bus.emit(b.event, { node: n, side: b.cls, event: e });
            });
            el.appendChild(btn);
        });
    };

    // ════════════════════════════════════════════════════
    //  PAN / ZOOM ENGINE
    // ════════════════════════════════════════════════════
    function PanZoom(wrap, viewport, opts) {
        this.wrap = wrap;
        this.vp = viewport;
        this.scale = opts.initScale || 1;
        this.panX = opts.initX || 0;
        this.panY = opts.initY || 0;
        this.minS = opts.minScale || 0.05;
        this.maxS = opts.maxScale || 4;
        this._isDrag = false;
        this._sx = 0; this._sy = 0; this._px = 0; this._py = 0;

        this._bindEvents();
    }

    PanZoom.prototype._bindEvents = function () {
        var self = this;
        var wrap = this.wrap;

        // Mouse drag
        wrap.addEventListener('mousedown', function (e) {
            if (e.button !== 0 || e.target.closest('.sh-action-btn,.sh-node')) return;
            self._isDrag = true;
            self._sx = e.clientX; self._sy = e.clientY;
            self._px = self.panX; self._py = self.panY;
            wrap.style.cursor = 'grabbing';
        });
        window.addEventListener('mousemove', function (e) {
            if (!self._isDrag) return;
            self.panX = self._px + e.clientX - self._sx;
            self.panY = self._py + e.clientY - self._sy;
            self._apply();
        });
        window.addEventListener('mouseup', function () {
            self._isDrag = false;
            wrap.style.cursor = 'grab';
        });

        // Touch drag
        wrap.addEventListener('touchstart', function (e) {
            if (e.touches.length === 1) {
                self._isDrag = true;
                self._sx = e.touches[0].clientX; self._sy = e.touches[0].clientY;
                self._px = self.panX; self._py = self.panY;
            }
        }, { passive: true });
        window.addEventListener('touchmove', function (e) {
            if (!self._isDrag || e.touches.length !== 1) return;
            self.panX = self._px + e.touches[0].clientX - self._sx;
            self.panY = self._py + e.touches[0].clientY - self._sy;
            self._apply();
        }, { passive: true });
        window.addEventListener('touchend', function () { self._isDrag = false; });

        // Wheel zoom
        wrap.addEventListener('wheel', function (e) {
            e.preventDefault();
            var delta = e.deltaY < 0 ? 0.12 : -0.12;
            var rect = wrap.getBoundingClientRect();
            var mx = e.clientX - rect.left;
            var my = e.clientY - rect.top;
            self.zoomAt(mx, my, delta);
        }, { passive: false });

        wrap.style.cursor = 'grab';
    };

    PanZoom.prototype.zoomAt = function (mx, my, delta) {
        var ns = Math.min(this.maxS, Math.max(this.minS, this.scale + delta));
        this.panX = mx - (mx - this.panX) * (ns / this.scale);
        this.panY = my - (my - this.panY) * (ns / this.scale);
        this.scale = ns;
        this._apply();
    };

    PanZoom.prototype.zoomCenter = function (delta) {
        var mx = this.wrap.clientWidth / 2;
        var my = this.wrap.clientHeight / 2;
        this.zoomAt(mx, my, delta);
    };

    PanZoom.prototype.setTransform = function (x, y, s) {
        this.panX = x; this.panY = y; this.scale = s;
        this._apply();
    };

    PanZoom.prototype._apply = function () {
        this.vp.style.transform =
            'translate(' + this.panX + 'px,' + this.panY + 'px) scale(' + this.scale + ')';
        if (this.onUpdate) this.onUpdate(this.scale);
    };

    // ════════════════════════════════════════════════════
    //  TOOLTIP
    // ════════════════════════════════════════════════════
    function Tooltip(cfg) {
        this.el = ShUtil.div('sh-tooltip');
        document.body.appendChild(this.el);
        this.roleLbls = cfg.roleLbls || {};
    }
    Tooltip.prototype.show = function (node, e) {
        var birth = ShUtil.fmtDate(node.birthDate);
        var died = ShUtil.fmtDate(node.diedDate);
        this.el.innerHTML =
            '<div class="sh-tt-name">' + (node.name || '—') + '</div>' +
            '<div class="sh-tt-info">' +
            'ID: ' + node.id + '<br>' +
            "Ro'li: " + (this.roleLbls[node.role] || node.role) +
            (birth ? '<br>Tug\'ilgan: ' + birth : '') +
            (died ? '<br>Vafot etgan: ' + died : '') +
            '</div>';
        this.el.classList.add('sh-tt-show');
        this.move(e);
    };
    Tooltip.prototype.move = function (e) {
        this.el.style.left = (e.clientX + 16) + 'px';
        this.el.style.top = (e.clientY - 10) + 'px';
    };
    Tooltip.prototype.hide = function () {
        this.el.classList.remove('sh-tt-show');
    };

    // ════════════════════════════════════════════════════
    //  ASOSIY SINF — ShTree
    // ════════════════════════════════════════════════════
    function ShTree(selector, options) {
        // ── Element ──
        this.el = typeof selector === 'string'
            ? document.querySelector(selector)
            : selector;
        if (!this.el) throw new Error('ShTree: element topilmadi: ' + selector);

        // ── Default konfiguratsiya ──
        this.cfg = Object.assign({
            /* Node o'lchamlari */
            nodeW: 200,
            nodeH: 80,
            centerNodeW: 210,
            centerNodeH: 88,
            /* Offset (backend koordinatalarini viewport ga ko'chirish) */
            offsetX: 2000,
            offsetY: 1500,
            /* Pan/Zoom */
            minScale: 0.05,
            maxScale: 4.0,
            /* Role yorliqlari */
            roleLbls: {
                CENTER: 'Markaz',
                FATHER: 'Ota',
                MOTHER: 'Ona',
                SPOUSE: "Turmush o'rtog'i",
                CHILD: 'Farzand',
                SIBLING: 'Aka-uka/Opa-singil'
            },
            /* Hover action tugmalar */
            showActionBtns: true,
            /* Tooltip */
            showTooltip: true,
            /* Statistika elementlari (id lar) */
            statsIds: {
                total: null,
                spouse: null,
                child: null,
                conn: null
            },
            /* Zoom % label element id */
            zoomLabelId: null
        }, options);

        // ── Hodisalar ──
        this.bus = new EventBus();

        // ── DOM tuzilmasi ──
        this._buildDOM();

        // ── Renderer ──
        this.renderer = new Renderer(this.cfg);

        // ── PanZoom ──
        var self = this;
        this.panZoom = new PanZoom(this.wrapEl, this.vpEl, {
            minScale: this.cfg.minScale,
            maxScale: this.cfg.maxScale
        });
        this.panZoom.onUpdate = function (s) { self._updateZoomLabel(s); };

        // ── Tooltip ──
        if (this.cfg.showTooltip) {
            this.tooltip = new Tooltip(this.cfg);
            this.bus.on('nodeEnter', function (d) { self.tooltip.show(d.node, d.event); });
            this.bus.on('nodeMove', function (d) { self.tooltip.move(d.event); });
            this.bus.on('nodeLeave', function () { self.tooltip.hide(); });
        }

        // ── Joriy ma'lumot ──
        this._data = null;
        this._pos = {};
    }

    /** DOM tuzilmasini yaratish (agar mavjud bo'lmasa) */
    ShTree.prototype._buildDOM = function () {
        // canvas-wrap → viewport → [svgLayer, nodesLayer]
        // Agar tashqi wrap allaqachon berilgan bo'lsa (masalan: canvasWrap) — uni ishlatamiz
        var existing = this.el.querySelector('[data-sh-viewport]');
        if (existing) {
            this.vpEl = existing;
            this.svgEl = this.el.querySelector('[data-sh-svg]');
            this.nodeEl = this.el.querySelector('[data-sh-nodes]');
            this.wrapEl = this.el;
            return;
        }

        this.wrapEl = this.el; // wrap = berilgan element

        // Viewport
        this.vpEl = ShUtil.div('sh-viewport');
        this.vpEl.setAttribute('data-sh-viewport', '1');
        this.vpEl.style.position = 'absolute';
        this.vpEl.style.transformOrigin = '0 0';
        this.vpEl.style.willChange = 'transform';

        // SVG qatlami (liniyalar)
        this.svgEl = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        this.svgEl.setAttribute('data-sh-svg', '1');
        this.svgEl.setAttribute('class', 'sh-svg-layer');
        this.svgEl.style.position = 'absolute';
        this.svgEl.style.top = '0';
        this.svgEl.style.left = '0';
        this.svgEl.style.pointerEvents = 'none';
        this.svgEl.style.overflow = 'visible';

        // SVG defs
        this.svgEl.innerHTML =
            '<defs>' +
            '<linearGradient id="sh-grad-line" x1="0%" y1="0%" x2="100%" y2="100%">' +
            '  <stop offset="0%"   stop-color="#6366f1" stop-opacity=".7"/>' +
            '  <stop offset="100%" stop-color="#22d3ee" stop-opacity=".5"/>' +
            '</linearGradient>' +
            '</defs>';

        // Nodelar qatlami (div lar)
        this.nodeEl = ShUtil.div('sh-nodes-layer');
        this.nodeEl.setAttribute('data-sh-nodes', '1');
        this.nodeEl.style.position = 'absolute';
        this.nodeEl.style.top = '0';
        this.nodeEl.style.left = '0';

        this.vpEl.appendChild(this.svgEl);
        this.vpEl.appendChild(this.nodeEl);
        this.wrapEl.appendChild(this.vpEl);
    };

    // ────────────────────────────────────────────────────
    //  ASOSIY METOD: load(data)
    //  Backend JSON ni qabul qilib, daraxtni chizadi
    // ────────────────────────────────────────────────────
    ShTree.prototype.load = function (data) {
        if (!data || !data.nodes || !data.nodes.length) {
            this.bus.emit('empty', {});
            return this;
        }

        this._data = data;
        this._pos = this.renderer.buildPosMap(data.nodes);

        this._renderSvg();
        this._renderNodes();
        this._updateStats();

        // Avtomatik fit
        var self = this;
        setTimeout(function () { self.fit(); }, 50);

        this.bus.emit('loaded', { data: data });
        return this;
    };

    /** SVG qatlamini qayta chizish */
    ShTree.prototype._renderSvg = function () {
        var data = this._data;
        var svg = this.svgEl;
        var defs = svg.querySelector('defs');

        this.renderer.sizeSvg(svg, data);

        // Mavjud pathlarni o'chirish (defs ni saqlab)
        while (svg.lastChild && svg.lastChild !== defs) {
            svg.removeChild(svg.lastChild);
        }

        this.renderer.drawConnections(svg, data.connections, this._pos);
    };

    /** HTML node lar qatlamini qayta chizish */
    ShTree.prototype._renderNodes = function () {
        var self = this;
        var container = this.nodeEl;
        container.innerHTML = '';

        this._data.nodes.forEach(function (n, i) {
            var el = self.renderer.createNodeEl(n, self._pos, self.bus, self.cfg);
            el.style.animationDelay = (i * 0.05) + 's';
            container.appendChild(el);
        });
    };

    // ────────────────────────────────────────────────────
    //  VIEW METODLAR
    // ────────────────────────────────────────────────────

    /** Barcha daraxtni ekranga sig'dirish */
    ShTree.prototype.fit = function () {
        if (!this._data) return this;
        var d = this._data;
        var cfg = this.cfg;
        var svgW = parseFloat(this.svgEl.getAttribute('width') || 0);
        var svgH = parseFloat(this.svgEl.getAttribute('height') || 0);
        if (!svgW || !svgH) return this;

        var ww = this.wrapEl.clientWidth - 80;
        var wh = this.wrapEl.clientHeight - 80;
        var sc = Math.min(ww / svgW, wh / svgH, 1.2);
        var px = (this.wrapEl.clientWidth - svgW * sc) / 2;
        var py = (this.wrapEl.clientHeight - svgH * sc) / 2;

        this.panZoom.setTransform(px, py, sc);
        return this;
    };

    /** Bosh (reset) ko'rinish */
    ShTree.prototype.resetView = function () {
        var w = this.wrapEl.clientWidth;
        var h = this.wrapEl.clientHeight;
        this.panZoom.setTransform(
            w / 2 - this.cfg.offsetX,
            h / 2 - this.cfg.offsetY,
            1
        );
        return this;
    };

    /** Berilgan person ID ni ekran markaziga olib kelish */
    ShTree.prototype.center = function (personId) {
        var p = this._pos[personId];
        if (!p) return this;
        var wW = this.wrapEl.clientWidth;
        var wH = this.wrapEl.clientHeight;
        var s = this.panZoom.scale;
        this.panZoom.setTransform(
            wW / 2 - (p.x + p.w / 2) * s,
            wH / 2 - (p.y + p.h / 2) * s,
            s
        );
        return this;
    };

    /** Zoom qilish (+delta yoki -delta) */
    ShTree.prototype.zoom = function (delta) {
        this.panZoom.zoomCenter(delta);
        return this;
    };

    // ────────────────────────────────────────────────────
    //  HODISALAR
    // ────────────────────────────────────────────────────
    /**
     * Hodisaga obuna bo'lish.
     * Hodisalar:
     *   'nodeClick'  → { node, event }
     *   'addParent'  → { node, side, event }
     *   'addSpouse'  → { node, side, event }
     *   'addChild'   → { node, side, event }
     *   'loaded'     → { data }
     *   'empty'      → {}
     */
    ShTree.prototype.on = function (type, fn) {
        this.bus.on(type, fn);
        return this;
    };

    ShTree.prototype.off = function (type, fn) {
        this.bus.off(type, fn);
        return this;
    };

    // ────────────────────────────────────────────────────
    //  MA'LUMOT OLISH
    // ────────────────────────────────────────────────────
    ShTree.prototype.getData = function () { return this._data; };
    ShTree.prototype.getNode = function (id) {
        if (!this._data) return null;
        return this._data.nodes.find(function (n) { return n.id == id; }) || null;
    };

    // ────────────────────────────────────────────────────
    //  YORDAMCHI
    // ────────────────────────────────────────────────────
    ShTree.prototype._updateStats = function () {
        if (!this._data) return;
        var ids = this.cfg.statsIds;
        var d = this._data;
        if (ids.total && document.getElementById(ids.total))
            document.getElementById(ids.total).textContent = d.nodes.length;
        if (ids.spouse && document.getElementById(ids.spouse))
            document.getElementById(ids.spouse).textContent = d.nodes.filter(function (n) { return n.role === 'SPOUSE'; }).length;
        if (ids.child && document.getElementById(ids.child))
            document.getElementById(ids.child).textContent = d.nodes.filter(function (n) { return n.role === 'CHILD'; }).length;
        if (ids.conn && document.getElementById(ids.conn))
            document.getElementById(ids.conn).textContent = (d.connections || []).length;
    };

    ShTree.prototype._updateZoomLabel = function (s) {
        var id = this.cfg.zoomLabelId;
        if (id && document.getElementById(id))
            document.getElementById(id).textContent = Math.round(s * 100) + '%';
    };

    // ────────────────────────────────────────────────────
    //  CSS — avtomatik inject qilinadi
    // ────────────────────────────────────────────────────
    ShTree._injectCSS = function () {
        if (document.getElementById('sh-tree-styles')) return;
        var style = document.createElement('style');
        style.id = 'sh-tree-styles';
        style.textContent = [
            /* ── NODE ── */
            '.sh-node{position:absolute;border-radius:16px;display:flex;align-items:center;',
            'gap:10px;padding:10px 14px;cursor:pointer;transition:transform .3s cubic-bezier(.34,1.56,.64,1),box-shadow .3s;',
            'opacity:0;animation:sh-appear .45s ease-out forwards;user-select:none;overflow:visible;}',

            '.sh-node:hover{transform:scale(1.07)!important;z-index:80;}',

            '@keyframes sh-appear{from{opacity:0;transform:scale(.6) translateY(12px)}to{opacity:1;transform:scale(1) translateY(0)}}',

            /* ── ROLE COLORS ── */
            '.sh-role-CENTER{background:linear-gradient(135deg,#6366f1,#8b5cf6,#a855f7);',
            'box-shadow:0 4px 28px rgba(99,102,241,.5),0 0 60px rgba(99,102,241,.12);',
            'border:2px solid rgba(139,92,246,.5);}',

            '.sh-role-FATHER{background:linear-gradient(135deg,#059669,#10b981);',
            'box-shadow:0 4px 20px rgba(16,185,129,.35);border:1.5px solid rgba(5,150,105,.4);}',

            '.sh-role-MOTHER{background:linear-gradient(135deg,#0891b2,#22d3ee);',
            'box-shadow:0 4px 20px rgba(34,211,238,.35);border:1.5px solid rgba(8,145,178,.4);}',

            '.sh-role-SPOUSE{background:linear-gradient(135deg,#ec4899,#f43f5e);',
            'box-shadow:0 4px 20px rgba(236,72,153,.35);border:1.5px solid rgba(244,63,94,.4);}',

            '.sh-role-CHILD.sh-gender-MALE{background:linear-gradient(135deg,#3b82f6,#6366f1);',
            'box-shadow:0 4px 18px rgba(59,130,246,.3);border:1.5px solid rgba(99,102,241,.3);}',

            '.sh-role-CHILD.sh-gender-FEMALE{background:linear-gradient(135deg,#f97316,#ec4899);',
            'box-shadow:0 4px 18px rgba(249,115,22,.3);border:1.5px solid rgba(236,72,153,.3);}',

            '.sh-role-SIBLING{background:linear-gradient(135deg,#d97706,#f59e0b);',
            'box-shadow:0 4px 18px rgba(245,158,11,.35);border:1.5px solid rgba(217,119,6,.4);}',

            /* ── AVATAR ── */
            '.sh-avatar{width:46px;height:46px;min-width:46px;border-radius:50%;overflow:hidden;',
            'border:2px solid rgba(255,255,255,.35);background:rgba(255,255,255,.15);',
            'display:flex;align-items:center;justify-content:center;font-size:17px;font-weight:700;color:#fff;flex-shrink:0;}',
            '.sh-avatar img{width:100%;height:100%;object-fit:cover;}',

            /* ── NODE INFO ── */
            '.sh-info{overflow:hidden;}',
            '.sh-name{font-size:13px;font-weight:700;color:#fff;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;}',
            '.sh-lbl{font-size:10px;font-weight:500;text-transform:uppercase;letter-spacing:.5px;opacity:.75;color:rgba(255,255,255,.85);margin-top:2px;}',
            '.sh-date{font-size:10px;opacity:.55;color:#fff;margin-top:1px;}',

            /* ── CENTER node kattaroq avatar ── */
            '.sh-role-CENTER .sh-avatar{width:52px;height:52px;min-width:52px;}',

            /* ── SVG CONNECTIONS ── */
            '.sh-conn{fill:none;stroke-width:2;opacity:.65;}',
            '.sh-conn-parent{stroke:url(#sh-grad-line);}',
            '.sh-conn-spouse{stroke:#ec4899;stroke-dasharray:8,4;animation:sh-dash 14s linear infinite;}',
            '@keyframes sh-dash{to{stroke-dashoffset:-120}}',

            /* ── ACTION BUTTONS ── */
            '.sh-action-btn{position:absolute;width:26px;height:26px;border-radius:50%;border:none;cursor:pointer;',
            'font-size:13px;font-weight:700;display:flex;align-items:center;justify-content:center;',
            'opacity:0;pointer-events:none;transition:opacity .2s,transform .2s,box-shadow .2s;',
            'z-index:200;color:#fff;box-shadow:0 2px 8px rgba(0,0,0,.5);}',

            '.sh-node:hover .sh-action-btn{opacity:1;pointer-events:all;}',

            '.sh-action-top{background:linear-gradient(135deg,#10b981,#059669);top:-13px;left:50%;transform:translateX(-50%);}',
            '.sh-action-left{background:linear-gradient(135deg,#ec4899,#f43f5e);left:-13px;top:50%;transform:translateY(-50%);}',
            '.sh-action-right{background:linear-gradient(135deg,#ec4899,#f43f5e);right:-13px;top:50%;transform:translateY(-50%);}',
            '.sh-action-bottom{background:linear-gradient(135deg,#3b82f6,#6366f1);bottom:-13px;left:50%;transform:translateX(-50%);}',

            '.sh-action-top:hover{transform:translateX(-50%) scale(1.2);box-shadow:0 4px 14px rgba(16,185,129,.7);}',
            '.sh-action-left:hover{transform:translateY(-50%) scale(1.2);box-shadow:0 4px 14px rgba(236,72,153,.7);}',
            '.sh-action-right:hover{transform:translateY(-50%) scale(1.2);box-shadow:0 4px 14px rgba(236,72,153,.7);}',
            '.sh-action-bottom:hover{transform:translateX(-50%) scale(1.2);box-shadow:0 4px 14px rgba(59,130,246,.7);}',

            /* ── TOOLTIP ── */
            '.sh-tooltip{position:fixed;z-index:9999;background:rgba(19,19,46,.97);backdrop-filter:blur(14px);',
            'border:1px solid rgba(99,102,241,.22);border-radius:12px;padding:12px 16px;pointer-events:none;',
            "opacity:0;transition:opacity .18s;max-width:240px;font-family:'Inter',sans-serif;}",
            '.sh-tt-show{opacity:1;}',
            '.sh-tt-name{font-size:14px;font-weight:700;color:#f1f5f9;margin-bottom:4px;}',
            '.sh-tt-info{font-size:11px;color:#64748b;line-height:1.55;}'
        ].join('');
        document.head.appendChild(style);
    };

    // CSS yi avtomatik yuklash
    ShTree._injectCSS();

    // ════════════════════════════════════════════════════
    //  GLOBAL EXPORT
    // ════════════════════════════════════════════════════
    global.ShTree = ShTree;

}(typeof window !== 'undefined' ? window : this));
