/**
 * ShTree — Renderer (SVG + HTML Render)
 * =======================================
 * Ikki qatlamli render:
 *   1. SVG qatlami  → liniyalar (connections)
 *   2. HTML qatlami → tugunlar (nodes as divs)
 *
 * Har tugunning ko'rinishi Template orqali belgilanadi.
 * Renderer faqat chizadi — mantiq EventBus orqali.
 */

(function (ShTree) {
    'use strict';

    /**
     * @param {Object}   config  - ShTree.config
     * @param {EventBus} bus     - ShTree.EventBus instance
     * @constructor
     */
    function Renderer(config, bus) {
        this.config = config;
        this.bus = bus;
    }

    // ════════════════════════════════════════════════
    //  SVG QATLAMI
    // ════════════════════════════════════════════════

    /**
     * SVG element o'lchamini belgilash.
     * @param {SVGElement} svg
     * @param {{ svgW, svgH }} boundary
     */
    Renderer.prototype.sizeSvg = function (svg, boundary) {
        svg.setAttribute('width', boundary.svgW);
        svg.setAttribute('height', boundary.svgH);
        svg.style.width = boundary.svgW + 'px';
        svg.style.height = boundary.svgH + 'px';
    };

    /**
     * Barcha connectionlarni SVG ga chizish.
     * @param {SVGElement} svg
     * @param {Array}      connections - backend connections[]
     * @param {Object}     posMap      - { id → {cx,cy,...} }
     */
    Renderer.prototype.drawConnections = function (svg, connections, posMap) {
        if (!connections || !connections.length) return;

        // Mavjud path larni o'chirish (defs saqlab)
        this._clearSvg(svg);

        connections.forEach(function (c, i) {
            var f = posMap[c.fromId];
            var t = posMap[c.toId];
            if (!f || !t) return;

            var isSpouse = (c.type === 'SPOUSE' || c.type === 'PARTNER');

            var path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
            path.setAttribute('d', Renderer._buildPathD(f, t, isSpouse));
            path.setAttribute('class', 'sh-conn ' + (isSpouse ? 'sh-conn-spouse' : 'sh-conn-parent'));
            path.style.animationDelay = (i * 0.04) + 's';

            svg.appendChild(path);
        });
    };

    /**
     * SVG pathining "d" attributini hisoblash.
     * Spouse → to'g'ri chiziq, boshqa → Bezier egri
     * @private
     */
    Renderer._buildPathD = function (from, to, isSpouse) {
        if (isSpouse) {
            return 'M ' + from.cx + ' ' + from.cy +
                ' L ' + to.cx + ' ' + to.cy;
        }
        var midY = (from.cy + to.cy) / 2;
        return 'M ' + from.cx + ' ' + from.cy +
            ' C ' + from.cx + ' ' + midY + ', ' +
            to.cx + ' ' + midY + ', ' +
            to.cx + ' ' + to.cy;
    };

    /**
     * SVG ni defs ni saqlab tozalash.
     * @param {SVGElement} svg
     * @private
     */
    Renderer.prototype._clearSvg = function (svg) {
        var defs = svg.querySelector('defs');
        svg.innerHTML = '';
        if (defs) svg.appendChild(defs);
    };

    // ════════════════════════════════════════════════
    //  HTML NODE QATLAMI
    // ════════════════════════════════════════════════

    /**
     * Barcha nodelarni HTML ga chizish.
     * @param {HTMLElement} container  - nodesLayer div
     * @param {Array}       nodes      - backend nodes[]
     * @param {Object}      posMap     - { id → {x,y,w,h} }
     * @param {Object}      template   - ShTree.templates[name]
     */
    Renderer.prototype.drawNodes = function (container, nodes, posMap, template) {
        var self = this;
        container.innerHTML = '';

        nodes.forEach(function (n, i) {
            var pos = posMap[n.id];
            if (!pos) return;

            var el = self.createNodeEl(n, pos, template);
            el.style.animationDelay = (i * 0.05) + 's';
            container.appendChild(el);
        });
    };

    /**
     * Bitta node HTML element yaratish.
     * @param {Object} node     - backend node
     * @param {Object} pos      - {x,y,w,h}
     * @param {Object} template - template moduli
     * @returns {HTMLElement}
     */
    Renderer.prototype.createNodeEl = function (node, pos, template) {
        var self = this;
        var cfg = this.config;
        var bus = this.bus;

        var el = document.createElement('div');
        el.id = 'sh-node-' + node.id;
        el.setAttribute('data-sh-id', node.id);

        // ── CSS klasi ──
        el.className = 'sh-node sh-role-' + node.role +
            (node.gender ? ' sh-gender-' + node.gender : '');

        // ── Pozitsiya ──
        el.style.position = 'absolute';
        el.style.left = pos.x + 'px';
        el.style.top = pos.y + 'px';
        el.style.width = pos.w + 'px';
        el.style.height = pos.h + 'px';

        // ── Ichki HTML (template orqali) ──
        el.innerHTML = template.renderNode(node, cfg);

        // ── Hodisalar ──
        el.addEventListener('click', function (e) {
            // Action tugmani bossak o'tkazib yubor
            if (e.target.closest('[data-sh-action]')) return;
            bus.emit('nodeClick', { node: node, event: e });
        });

        el.addEventListener('mouseenter', function (e) {
            bus.emit('nodeEnter', { node: node, event: e });
        });
        el.addEventListener('mousemove', function (e) {
            bus.emit('nodeMove', { event: e });
        });
        el.addEventListener('mouseleave', function () {
            bus.emit('nodeLeave', { node: node });
        });

        // ── Action tugmalar ──
        if (cfg.showActionBtns) {
            this._appendActionBtns(el, node, bus);
        }

        return el;
    };

    /**
     * Hover ugla tugmalarni qo'shish.
     * @param {HTMLElement} el    - node element
     * @param {Object}      node  - backend node
     * @param {EventBus}    bus
     * @private
     */
    Renderer.prototype._appendActionBtns = function (el, node, bus) {
        var actions = [
            { pos: 'top', symbol: '↑', event: 'addParent', title: "Ota-ona qo'sh" },
            { pos: 'left', symbol: '+', event: 'addSpouse', title: "Er/Xotin qo'sh" },
            { pos: 'right', symbol: '+', event: 'addSpouse', title: "Er/Xotin qo'sh" },
            { pos: 'bottom', symbol: '↓', event: 'addChild', title: "Farzand qo'sh" }
        ];

        actions.forEach(function (a) {
            var btn = document.createElement('button');
            btn.className = 'sh-action-btn sh-action-' + a.pos;
            btn.textContent = a.symbol;
            btn.title = a.title;
            btn.setAttribute('data-sh-action', a.event);

            btn.addEventListener('click', function (e) {
                e.stopPropagation();
                bus.emit(a.event, { node: node, side: a.pos, event: e });
            });

            el.appendChild(btn);
        });
    };

    // ── Export ──
    ShTree.Renderer = Renderer;

}(window.ShTree = window.ShTree || {}));
