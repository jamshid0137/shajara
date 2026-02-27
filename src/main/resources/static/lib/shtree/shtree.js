/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║  ShTree.js — Asosiy fayl (Entry Point)                          ║
 * ║  Oila daraxti kutubxonasi                                        ║
 * ║                                                                  ║
 * ║  Ishlatish tartibi (tree.html da):                              ║
 * ║    <link rel="stylesheet" href="/lib/shtree/shtree.css">        ║
 * ║    <script src="/lib/shtree/modules/events.js"></script>        ║
 * ║    <script src="/lib/shtree/modules/manager.js"></script>       ║
 * ║    <script src="/lib/shtree/modules/renderer.js"></script>      ║
 * ║    <script src="/lib/shtree/modules/templates/base.js"></script>║
 * ║    <script src="/lib/shtree/modules/templates/shajara.js"></script>
 * ║    <script src="/lib/shtree/modules/ui/panZoomUI.js"></script>  ║
 * ║    <script src="/lib/shtree/modules/ui/tooltipUI.js"></script>  ║
 * ║    <script src="/lib/shtree/modules/ui/searchUI.js"></script>   ║
 * ║    <script src="/lib/shtree/modules/ui/menuUI.js"></script>     ║
 * ║    <script src="/lib/shtree/modules/ui/toolbarUI.js"></script>  ║
 * ║    <script src="/lib/shtree/modules/ui/editUI.js"></script>     ║
 * ║    <script src="/lib/shtree/shtree.js"></script>  ← SHU OXIRDA ║
 * ╚══════════════════════════════════════════════════════════════════╝
 *
 * MISOL:
 *   var tree = new ShTree('#canvasWrap', {
 *       template:   'shajara',
 *       offsetX:    2000,
 *       offsetY:    1500,
 *       showTooltip: true,
 *       showActionBtns: true,
 *       toolbarIds: { zoomIn:'btnZoomIn', fit:'btnFit', zoomLabel:'zlabel' },
 *       statsIds:   { total:'sTotal', spouse:'sSpouse', child:'sChild', conn:'sConn' }
 *   });
 *
 *   tree.load(backendJson);            // backend JSON → chizish
 *   tree.on('nodeClick', fn);          // hodisalar
 *   tree.fit();                        // moslash
 *   tree.center(personId);             // markazlash
 */

(function (global) {
    'use strict';

    // ShTree namespaceiga ishora
    var SH = global.ShTree;

    // ════════════════════════════════════════════════
    //  ASOSIY SINF
    // ════════════════════════════════════════════════
    /**
     * @param {string|HTMLElement} selector - konteyner element yoki CSS selector
     * @param {Object}             options  - konfiguratsiya
     * @constructor
     */
    function ShTree(selector, options) {
        // ── SH modullarini tekshirish ──
        var required = ['EventBus', 'Manager', 'Renderer', 'PanZoomUI'];
        for (var ri = 0; ri < required.length; ri++) {
            if (!SH || typeof SH[required[ri]] !== 'function') {
                throw new Error('ShTree: modul yuklanmagan → ShTree.' + required[ri] +
                    '. Iltimos /lib/shtree/modules/ fayllarini tekshiring.');
            }
        }

        // ── Element ──
        this.el = typeof selector === 'string'
            ? document.querySelector(selector)
            : selector;

        if (!this.el) {
            throw new Error('ShTree: element topilmadi — ' + selector);
        }

        // ── Konfiguratsiya ──
        this.config = ShTree._mergeDefaults(options || {});

        // ── Hodisalar ──
        this.bus = new SH.EventBus();

        // ── DOM tuzilmasi ──
        this._buildDOM();

        // ── Modullar ──
        this.manager = new SH.Manager(this.config);
        this.renderer = new SH.Renderer(this.config, this.bus);

        // ── PanZoom ──
        this.panZoom = new SH.PanZoomUI(
            this.wrapEl,
            this.vpEl,
            this.config,
            this.bus
        );

        // ── UI Modullari ──
        this._initUI();

        // ── Joriy holat ──
        this._data = null;
    }

    // ════════════════════════════════════════════════
    //  DEFAULT KONFIGURATSIYA
    // ════════════════════════════════════════════════
    ShTree._defaults = {
        /* Render */
        template: 'base',    // 'base' | 'shajara' | boshqa
        nodeW: 200,
        nodeH: 80,
        centerNodeW: 210,
        centerNodeH: 88,

        /* Viewport offset (backend x,y → ekran x,y) */
        offsetX: 2000,
        offsetY: 1500,

        /* PanZoom */
        minScale: 0.05,
        maxScale: 4.0,
        cursor: 'grab',

        /* UI */
        showTooltip: true,
        showActionBtns: true,
        showMenu: false,   // o'ng klik menyu

        /* Konfiguratsiya ID lari (toolbar elementlari) */
        toolbarIds: {},

        /* Statistika element ID lari */
        statsIds: {},

        /* Edit panel */
        editPanelId: null,
        editContentId: null,

        /* Qidiruv input ID si */
        searchInputId: null,

        /* Rol yorliqlari (o'zbek tili) */
        roleLbls: {
            CENTER: 'Markaz',
            FATHER: 'Ota',
            MOTHER: 'Ona',
            SPOUSE: "Turmush o'rtog'i",
            CHILD: 'Farzand',
            SIBLING: 'Aka-uka / Opa-singil'
        }
    };

    /**
     * Options va defaults ni birlashtirish.
     * @param {Object} options
     * @returns {Object}
     */
    ShTree._mergeDefaults = function (options) {
        var cfg = {};
        var def = ShTree._defaults;

        for (var k in def) cfg[k] = def[k];
        for (var k in options) cfg[k] = options[k];

        // Ichki ob'ektlarni ham merge qilish
        cfg.toolbarIds = Object.assign({}, def.toolbarIds, options.toolbarIds || {});
        cfg.statsIds = Object.assign({}, def.statsIds, options.statsIds || {});
        cfg.roleLbls = Object.assign({}, def.roleLbls, options.roleLbls || {});

        return cfg;
    };

    // ════════════════════════════════════════════════
    //  DOM TUZILMASI
    // ════════════════════════════════════════════════
    /**
     * Viewport, SVG va nodes qatlamlarini yaratish.
     * Agar tashqi HTML da allaqachon mavjud bo'lsa — ularni ishlatadi.
     */
    ShTree.prototype._buildDOM = function () {
        var el = this.el;

        // Viewport
        var vp = el.querySelector('[data-sh-viewport]');
        if (vp) {
            this.wrapEl = el;
            this.vpEl = vp;
            this.svgEl = el.querySelector('[data-sh-svg]');
            this.nodeEl = el.querySelector('[data-sh-nodes]');
            return;
        }

        this.wrapEl = el;

        // ── Viewport ──
        var vpEl = document.createElement('div');
        vpEl.className = 'sh-viewport';
        vpEl.setAttribute('data-sh-viewport', '1');
        vpEl.style.cssText = 'position:absolute;transform-origin:0 0;will-change:transform;';

        // ── SVG (liniyalar) ──
        var svgEl = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        svgEl.setAttribute('class', 'sh-svg-layer');
        svgEl.setAttribute('data-sh-svg', '1');
        svgEl.style.cssText =
            'position:absolute;top:0;left:0;pointer-events:none;overflow:visible;';
        svgEl.innerHTML =
            '<defs>' +
            '<linearGradient id="sh-grad-line" x1="0%" y1="0%" x2="100%" y2="100%">' +
            '  <stop offset="0%"   stop-color="#6366f1" stop-opacity=".7"/>' +
            '  <stop offset="100%" stop-color="#22d3ee" stop-opacity=".5"/>' +
            '</linearGradient>' +
            '</defs>';

        // ── Nodes qatlami ──
        var nodeEl = document.createElement('div');
        nodeEl.setAttribute('data-sh-nodes', '1');
        nodeEl.style.cssText = 'position:absolute;top:0;left:0;';

        vpEl.appendChild(svgEl);
        vpEl.appendChild(nodeEl);
        el.appendChild(vpEl);

        this.vpEl = vpEl;
        this.svgEl = svgEl;
        this.nodeEl = nodeEl;
    };

    // ════════════════════════════════════════════════
    //  UI MODULLAR
    // ════════════════════════════════════════════════
    ShTree.prototype._initUI = function () {
        var cfg = this.config;

        // ── Tooltip ──
        if (cfg.showTooltip && SH.TooltipUI) {
            this.tooltip = new SH.TooltipUI(this.bus, cfg);
        }

        // ── Toolbar ──
        if (SH.ToolbarUI) {
            this.toolbar = new SH.ToolbarUI(this.bus, this.panZoom, this.manager, cfg);
        }

        // ── Edit panel ──
        if (SH.EditUI) {
            this.editUI = new SH.EditUI(this.bus, cfg);
        }

        // ── Qidiruv ──
        if (cfg.searchInputId && SH.SearchUI) {
            this.search = new SH.SearchUI(
                cfg.searchInputId, this.bus, this.manager, this.panZoom, cfg
            );
        }

        // ── Context menyu ──
        if (cfg.showMenu && SH.MenuUI) {
            this.menu = new SH.MenuUI(this.bus, cfg);
            this._enableRightClick();
        }
    };

    /** O'ng klik → MenuUI ga xabar berish */
    ShTree.prototype._enableRightClick = function () {
        var self = this;
        this.nodeEl.addEventListener('contextmenu', function (e) {
            var nodeEl = e.target.closest('[data-sh-id]');
            if (!nodeEl) return;
            e.preventDefault();
            var id = nodeEl.getAttribute('data-sh-id');
            var node = self.manager.getNode(id);
            if (node) self.bus.emit('nodeRightClick', { node: node, event: e });
        });
    };

    // ════════════════════════════════════════════════
    //  ASOSIY METOD: load(data)
    // ════════════════════════════════════════════════
    /**
     * Backend JSON ni yuklash va daraxtni chizish.
     *
     * @param {Object} data - backend JSON:
     *   { nodes:[...], connections:[...], minX, maxX, minY, maxY }
     * @returns {ShTree} - chain uchun
     */
    ShTree.prototype.load = function (data) {
        var self = this;

        if (!data || !data.nodes || !data.nodes.length) {
            this.bus.emit('empty', {});
            return this;
        }

        this._data = data;

        // Manager: posMap va boundary hisoblash
        this.manager.process(data, function (posMap, boundary) {
            if (!posMap) return;

            var tpl = SH.templates[self.config.template]
                || SH.templates['base'];

            // 1. SVG o'lchami + liniyalar
            self.renderer.sizeSvg(self.svgEl, boundary);
            self.renderer.drawConnections(self.svgEl, data.connections, posMap);

            // 2. HTML nodelar
            self.renderer.drawNodes(self.nodeEl, data.nodes, posMap, tpl);

            // 3. Stats yangilash
            if (self.toolbar) {
                self.toolbar.updateStats(self.config.statsIds);
            }
        });

        // Avtomatik fit (kichik kechikish bilan DOM tayyor bo'lgandan keyin)
        setTimeout(function () { self.fit(); }, 60);

        this.bus.emit('loaded', { data: data });
        return this;
    };

    // ════════════════════════════════════════════════
    //  VIEW METODLAR
    // ════════════════════════════════════════════════

    /** Barcha daraxtni ekranga sig'dirish */
    ShTree.prototype.fit = function () {
        var b = this.manager.getBoundary();
        this.panZoom.fit(b);
        return this;
    };

    /** Bosh ko'rinish (1:1) */
    ShTree.prototype.resetView = function () {
        var cfg = this.config;
        this.panZoom.resetView(cfg.offsetX, cfg.offsetY);
        return this;
    };

    /**
     * Berilgan person ID ni ekran markaziga olib kelish.
     * @param {number|string} personId
     */
    ShTree.prototype.center = function (personId) {
        var pos = this.manager.getPos(personId);
        if (pos) this.panZoom.centerOnPos(pos);
        return this;
    };

    /**
     * Zoom qilish.
     * @param {number} delta - musbat: kattalashtirish, manfiy: kichraytirish
     */
    ShTree.prototype.zoom = function (delta) {
        this.panZoom.zoomCenter(delta);
        return this;
    };

    // ════════════════════════════════════════════════
    //  HODISALAR
    // ════════════════════════════════════════════════
    /**
     * Hodisaga obuna bo'lish.
     *
     * Hodisalar ro'yxati:
     *   'nodeClick'      → { node, event }         tugunga bosildi
     *   'nodeEnter'      → { node, event }         tugun ustiga keldi
     *   'nodeLeave'      → { node }                tugundan chiqdi
     *   'addParent'      → { node, side, event }    ota qo'sh bosildi
     *   'addSpouse'      → { node, side, event }    er/xotin qo'sh
     *   'addChild'       → { node, side, event }    farzand qo'sh
     *   'details'        → { node }                 batafsil
     *   'delete'         → { node }                 o'chirish
     *   'loaded'         → { data }                 yuklandi
     *   'empty'          → {}                        ma'lumot yo'q
     *   'scaleChange'    → { scale }                zoom o'zgardi
     *   'editOpen'       → { node }                 panel ochildi
     *   'editClose'      → {}                        panel yopildi
     *
     * @param {string}   type
     * @param {Function} fn
     * @returns {ShTree}
     */
    ShTree.prototype.on = function (type, fn) {
        this.bus.on(type, fn);
        return this;
    };

    ShTree.prototype.off = function (type, fn) {
        this.bus.off(type, fn);
        return this;
    };

    // ════════════════════════════════════════════════
    //  MA'LUMOT OLISH
    // ════════════════════════════════════════════════
    /** Joriy raw JSON */
    ShTree.prototype.getData = function () {
        return this._data;
    };

    /** ID bo'yicha node topish */
    ShTree.prototype.getNode = function (id) {
        return this.manager.getNode(id);
    };

    /** Joriy scale */
    ShTree.prototype.getScale = function () {
        return this.panZoom.getScale();
    };

    // ════════════════════════════════════════════════
    //  TOZALASH
    // ════════════════════════════════════════════════
    ShTree.prototype.destroy = function () {
        this.bus.clear();
        this.el.innerHTML = '';
        this.manager.reset();
        this._data = null;
    };

    // ════════════════════════════════════════════════
    //  GLOBAL EXPORT
    // ════════════════════════════════════════════════
    global.ShTree = ShTree;

    // Modullarni ShTree ga ham ulash (qulaylik uchun)
    ShTree.EventBus = SH.EventBus;
    ShTree.Manager = SH.Manager;
    ShTree.Renderer = SH.Renderer;
    ShTree.PanZoomUI = SH.PanZoomUI;
    ShTree.TooltipUI = SH.TooltipUI;
    ShTree.SearchUI = SH.SearchUI;
    ShTree.MenuUI = SH.MenuUI;
    ShTree.ToolbarUI = SH.ToolbarUI;
    ShTree.EditUI = SH.EditUI;
    ShTree.templates = SH.templates;

}(typeof window !== 'undefined' ? window : this));
