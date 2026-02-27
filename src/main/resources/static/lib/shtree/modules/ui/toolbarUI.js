/**
 * ShTree — Toolbar UI
 * ====================
 * Daraxtni boshqarish tugmalar paneli.
 * Zoom in/out, Fit, Reset, qidiruv va stats info.
 *
 * Toolbar to'g'ridan-to'g'ri yaratilmaydi —
 * tree.html da HTML yoziladi va ShTree.ToolbarUI
 * shu elementlarga event bog'laydi.
 *
 * Alternativa: agar toolbarIds config da ko'rsatilsa,
 * avtomatik bog'laydi.
 *
 * config.toolbarIds = {
 *   zoomIn:    'btnZoomIn',
 *   zoomOut:   'btnZoomOut',
 *   fit:       'btnFit',
 *   reset:     'btnReset',
 *   zoomLabel: 'zlabel'
 * }
 */

(function (ShTree) {
    'use strict';

    /**
     * @param {EventBus}  bus
     * @param {PanZoomUI} panZoom
     * @param {Manager}   manager
     * @param {Object}    config
     * @constructor
     */
    function ToolbarUI(bus, panZoom, manager, config) {
        this.bus = bus;
        this.panZoom = panZoom;
        this.manager = manager;
        this.config = config;
        this.ids = config.toolbarIds || {};

        this._bindBus();
        this._bindButtons();
    }

    /** Scale o'zgarganda zoom label ni yangilash */
    ToolbarUI.prototype._bindBus = function () {
        var self = this;
        this.bus.on('scaleChange', function (d) {
            self._updateLabel(d.scale);
        });
    };

    /** Toolbar tugmalariga event qo'shish */
    ToolbarUI.prototype._bindButtons = function () {
        var self = this;
        var ids = this.ids;

        this._bind(ids.zoomIn, function () {
            self.panZoom.zoomCenter(0.15);
        });

        this._bind(ids.zoomOut, function () {
            self.panZoom.zoomCenter(-0.15);
        });

        this._bind(ids.fit, function () {
            var b = self.manager.getBoundary();
            self.panZoom.fit(b);
        });

        this._bind(ids.reset, function () {
            var cfg = self.config;
            self.panZoom.resetView(cfg.offsetX, cfg.offsetY);
        });
    };

    /** ID bo'yicha element topib, click bind qilish */
    ToolbarUI.prototype._bind = function (id, fn) {
        if (!id) return;
        var el = document.getElementById(id);
        if (el) el.addEventListener('click', fn);
    };

    /** Zoom foizini yangilash */
    ToolbarUI.prototype._updateLabel = function (scale) {
        var id = this.ids.zoomLabel;
        if (!id) return;
        var el = document.getElementById(id);
        if (el) el.textContent = Math.round(scale * 100) + '%';
    };

    /** Statistikani yangilash */
    ToolbarUI.prototype.updateStats = function (statsIds) {
        if (!statsIds) return;
        var stats = this.manager.getStats();

        function set(id, val) {
            var el = document.getElementById(id);
            if (el) el.textContent = val;
        }

        set(statsIds.total, stats.total);
        set(statsIds.spouse, stats.spouses);
        set(statsIds.child, stats.children);
        set(statsIds.conn, stats.connections);
    };

    // ── Export ──
    ShTree.ToolbarUI = ToolbarUI;

}(window.ShTree = window.ShTree || {}));
