/**
 * ShTree — Search UI
 * ===================
 * Shajara ichida ism bo'yicha qidirish.
 *
 * Ishlash:
 *   1. Foydalanuvchi qidiruv maydoniga yozadi
 *   2. Nodelar filtrlashadi (mos kelmaganlar xiralanadi)
 *   3. Bita natijaga bosish → o'sha node markazga olib keladi
 *   4. ESC / tozalash → normal holat
 *
 * Ishlatish:
 *   new ShTree.SearchUI(wrapId, bus, manager, panZoom, config)
 *   yoki tree.search.open(), tree.search.close()
 */

(function (ShTree) {
    'use strict';

    /**
     * @param {string}    inputElId  - qidiruv input elementi ID si
     * @param {EventBus}  bus
     * @param {Manager}   manager
     * @param {PanZoomUI} panZoom
     * @param {Object}    config
     * @constructor
     */
    function SearchUI(inputElId, bus, manager, panZoom, config) {
        this.inputEl = document.getElementById(inputElId);
        this.bus = bus;
        this.manager = manager;
        this.panZoom = panZoom;
        this.config = config;
        this._active = false;

        if (!this.inputEl) {
            console.warn('ShTree.SearchUI: input elementi topilmadi: #' + inputElId);
            return;
        }

        this._bindEvents();
    }

    SearchUI.prototype._bindEvents = function () {
        var self = this;

        // Har harfda qidirish
        this.inputEl.addEventListener('input', function () {
            var q = self.inputEl.value.trim().toLowerCase();
            if (q.length < 1) {
                self._clearHighlight();
            } else {
                self._search(q);
            }
        });

        // ESC → tozalash
        this.inputEl.addEventListener('keydown', function (e) {
            if (e.key === 'Escape') {
                self.inputEl.value = '';
                self._clearHighlight();
            }
        });
    };

    /**
     * Qidirish va natijalarni ko'rsatish.
     * @param {string} query - kichik harflarga aylantrilgan
     */
    SearchUI.prototype._search = function (query) {
        var data = this.manager.getRawData();
        if (!data || !data.nodes) return;

        var matchIds = {};
        data.nodes.forEach(function (n) {
            var name = (n.name || '').toLowerCase();
            if (name.indexOf(query) !== -1) {
                matchIds[n.id] = true;
            }
        });

        // Nodelarni ajratib ko'rsatish
        var allNodes = document.querySelectorAll('.sh-node');
        allNodes.forEach(function (el) {
            var id = el.getAttribute('data-sh-id');
            if (matchIds[id]) {
                el.classList.remove('sh-search-dim');
                el.classList.add('sh-search-match');
            } else {
                el.classList.add('sh-search-dim');
                el.classList.remove('sh-search-match');
            }
        });

        // Birinchi topilganni markazga olib kelish
        var firstId = Object.keys(matchIds)[0];
        if (firstId) {
            var pos = this.manager.getPos(firstId);
            if (pos) this.panZoom.centerOnPos(pos);
        }

        this._active = true;
    };

    /** Highlight ni tozalash */
    SearchUI.prototype._clearHighlight = function () {
        var allNodes = document.querySelectorAll('.sh-node');
        allNodes.forEach(function (el) {
            el.classList.remove('sh-search-dim', 'sh-search-match');
        });
        this._active = false;
    };

    /** Qidiruvni tozalash */
    SearchUI.prototype.clear = function () {
        if (this.inputEl) this.inputEl.value = '';
        this._clearHighlight();
    };

    /** Faolmi? */
    SearchUI.prototype.isActive = function () {
        return this._active;
    };

    // ── Export ──
    ShTree.SearchUI = SearchUI;

}(window.ShTree = window.ShTree || {}));
