/**
 * ShTree — Menu UI
 * =================
 * Node ustiga sichqoncha o'ng tugma bosilganda
 * yoki maxsus tugma bosilganda chiqadigan kontekst menyu.
 *
 * Menyu elementlari:
 *   - Ota qo'sh
 *   - Er/Xotin qo'sh
 *   - Farzand qo'sh
 *   - ── ajratuvchi ──
 *   - Ko'proq ma'lumot
 *   - O'chirish
 *
 * Ishlatish:
 *   new ShTree.MenuUI(bus, config)
 */

(function (ShTree) {
    'use strict';

    /**
     * @param {EventBus} bus
     * @param {Object}   config
     * @constructor
     */
    function MenuUI(bus, config) {
        this.bus = bus;
        this.config = config;
        this.el = null;
        this._currentNode = null;

        this._create();
        this._bindBus();
        this._bindGlobal();
    }

    // ════════════════════════════════════════════════
    //  DOM YARATISH
    // ════════════════════════════════════════════════
    MenuUI.prototype._create = function () {
        var el = document.createElement('div');
        el.className = 'sh-menu';
        el.id = 'sh-context-menu';
        el.innerHTML =
            '<div class="sh-menu-item" data-sh-menu="addParent">↑ Ota-ona qo\'sh</div>' +
            '<div class="sh-menu-item" data-sh-menu="addSpouse">⟷ Er/Xotin qo\'sh</div>' +
            '<div class="sh-menu-item" data-sh-menu="addChild">↓ Farzand qo\'sh</div>' +
            '<div class="sh-menu-sep"></div>' +
            '<div class="sh-menu-item" data-sh-menu="details">🔍 Batafsil</div>' +
            '<div class="sh-menu-item sh-menu-danger" data-sh-menu="delete">✕ O\'chirish</div>';

        document.body.appendChild(el);
        this.el = el;

        // Menyu ichidagi elementlarga click
        var self = this;
        el.addEventListener('click', function (e) {
            var item = e.target.closest('[data-sh-menu]');
            if (!item) return;
            var action = item.getAttribute('data-sh-menu');
            if (self._currentNode) {
                self.bus.emit(action, { node: self._currentNode });
            }
            self.hide();
        });
    };

    // ════════════════════════════════════════════════
    //  BUS VA GLOBAL HODISALAR
    // ════════════════════════════════════════════════
    MenuUI.prototype._bindBus = function () {
        var self = this;

        // Node o'ng click → menyu ochish
        this.bus.on('nodeRightClick', function (d) {
            self._currentNode = d.node;
            self.show(d.event.clientX, d.event.clientY);
        });
    };

    MenuUI.prototype._bindGlobal = function () {
        var self = this;

        // Boshqa joyga bosish → menyu yopish
        document.addEventListener('click', function (e) {
            if (!e.target.closest('#sh-context-menu')) {
                self.hide();
            }
        });

        // ESC → yopish
        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape') self.hide();
        });

        // Scroll → yopish
        document.addEventListener('scroll', function () { self.hide(); }, true);
    };

    // ════════════════════════════════════════════════
    //  METODLAR
    // ════════════════════════════════════════════════
    MenuUI.prototype.show = function (x, y) {
        this.el.style.display = 'block';
        this.el.style.left = x + 'px';
        this.el.style.top = y + 'px';

        // Ekran chegarasidan chiqmasligi
        var rect = this.el.getBoundingClientRect();
        if (rect.right > window.innerWidth) this.el.style.left = (x - rect.width) + 'px';
        if (rect.bottom > window.innerHeight) this.el.style.top = (y - rect.height) + 'px';
    };

    MenuUI.prototype.hide = function () {
        this.el.style.display = 'none';
        this._currentNode = null;
    };

    MenuUI.prototype.isVisible = function () {
        return this.el.style.display === 'block';
    };

    // ── Export ──
    ShTree.MenuUI = MenuUI;

}(window.ShTree = window.ShTree || {}));
