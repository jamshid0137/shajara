/**
 * ShTree — PanZoom UI
 * ====================
 * Sichqoncha drag (pan) va scroll (zoom) boshqaruvchi modul.
 * Touch ekranlarni ham qo'llab-quvvatlaydi.
 *
 * Transform: translate(panX, panY) scale(scale)
 * Viewport elementiga qo'llanadi.
 */

(function (ShTree) {
    'use strict';

    /**
     * @param {HTMLElement} wrapEl    - klik ushlanadigan tashqi konteyner
     * @param {HTMLElement} viewportEl - transform qo'llaniladigan element
     * @param {Object}      config
     * @param {EventBus}    bus
     * @constructor
     */
    function PanZoomUI(wrapEl, viewportEl, config, bus) {
        this.wrap = wrapEl;
        this.vp = viewportEl;
        this.config = config;
        this.bus = bus;

        // Joriy holat
        this.scale = config.initScale || 1;
        this.panX = config.initX || 0;
        this.panY = config.initY || 0;

        // Chegaralar
        this.minScale = config.minScale || 0.05;
        this.maxScale = config.maxScale || 4.0;

        // Drag holati
        this._drag = false;
        this._sx = 0; this._sy = 0;   // drag boshlangandagi sichqoncha
        this._px = 0; this._py = 0;   // drag boshlangandagi panX/panY

        this._bindEvents();
    }

    // ════════════════════════════════════════════════
    //  HODISALARNI ULASH
    // ════════════════════════════════════════════════
    PanZoomUI.prototype._bindEvents = function () {
        var self = this;
        var wrap = this.wrap;

        // ── Mouse Drag ──
        wrap.addEventListener('mousedown', function (e) {
            // Faqat chap tugma va node usti bo'lmasa
            if (e.button !== 0) return;
            if (e.target.closest('.sh-node, .sh-action-btn')) return;

            self._drag = true;
            self._sx = e.clientX;
            self._sy = e.clientY;
            self._px = self.panX;
            self._py = self.panY;
            wrap.style.cursor = 'grabbing';
        });

        window.addEventListener('mousemove', function (e) {
            if (!self._drag) return;
            self.panX = self._px + e.clientX - self._sx;
            self.panY = self._py + e.clientY - self._sy;
            self._apply();
        });

        window.addEventListener('mouseup', function () {
            if (!self._drag) return;
            self._drag = false;
            wrap.style.cursor = self.config.cursor || 'grab';
        });

        // ── Touch Drag ──
        wrap.addEventListener('touchstart', function (e) {
            if (e.touches.length !== 1) return;
            self._drag = true;
            self._sx = e.touches[0].clientX;
            self._sy = e.touches[0].clientY;
            self._px = self.panX;
            self._py = self.panY;
        }, { passive: true });

        window.addEventListener('touchmove', function (e) {
            if (!self._drag || e.touches.length !== 1) return;
            self.panX = self._px + e.touches[0].clientX - self._sx;
            self.panY = self._py + e.touches[0].clientY - self._sy;
            self._apply();
        }, { passive: true });

        window.addEventListener('touchend', function () {
            self._drag = false;
        });

        // ── Wheel Zoom ──
        wrap.addEventListener('wheel', function (e) {
            e.preventDefault();
            var delta = e.deltaY < 0 ? 0.12 : -0.12;
            var rect = wrap.getBoundingClientRect();
            var mx = e.clientX - rect.left;
            var my = e.clientY - rect.top;
            self.zoomAt(mx, my, delta);
        }, { passive: false });

        // Boshlang'ich cursor
        wrap.style.cursor = self.config.cursor || 'grab';
    };

    // ════════════════════════════════════════════════
    //  ZOOM METODLAR
    // ════════════════════════════════════════════════

    /**
     * Berilgan nuqta atrofida zoom qilish.
     * @param {number} mx    - pivot x (viewport ichida)
     * @param {number} my    - pivot y
     * @param {number} delta - + kattalashtiradi, - kichrayתiradi
     */
    PanZoomUI.prototype.zoomAt = function (mx, my, delta) {
        var ns = Math.min(this.maxScale, Math.max(this.minScale, this.scale + delta));
        this.panX = mx - (mx - this.panX) * (ns / this.scale);
        this.panY = my - (my - this.panY) * (ns / this.scale);
        this.scale = ns;
        this._apply();
    };

    /**
     * Ekran markazi atrofida zoom qilish (tugmalar uchun).
     * @param {number} delta
     */
    PanZoomUI.prototype.zoomCenter = function (delta) {
        var mx = this.wrap.clientWidth / 2;
        var my = this.wrap.clientHeight / 2;
        this.zoomAt(mx, my, delta);
    };

    // ════════════════════════════════════════════════
    //  VIEW METODLAR
    // ════════════════════════════════════════════════

    /**
     * Transformni to'g'ridan belgilash.
     * @param {number} x
     * @param {number} y
     * @param {number} s - scale
     */
    PanZoomUI.prototype.setTransform = function (x, y, s) {
        this.panX = x;
        this.panY = y;
        this.scale = s;
        this._apply();
    };

    /**
     * Barcha daraxtni ekranga sig'dirish.
     * @param {{ svgW, svgH }} boundary
     */
    PanZoomUI.prototype.fit = function (boundary) {
        if (!boundary || !boundary.svgW) return;
        var wW = this.wrap.clientWidth - 80;
        var wH = this.wrap.clientHeight - 80;
        var sc = Math.min(wW / boundary.svgW, wH / boundary.svgH, 1.2);
        var px = (this.wrap.clientWidth - boundary.svgW * sc) / 2;
        var py = (this.wrap.clientHeight - boundary.svgH * sc) / 2;
        this.setTransform(px, py, sc);
    };

    /**
     * Bosh ko'rinish (1:1, markaz).
     * @param {number} [offsetX]
     * @param {number} [offsetY]
     */
    PanZoomUI.prototype.resetView = function (offsetX, offsetY) {
        var w = this.wrap.clientWidth;
        var h = this.wrap.clientHeight;
        this.setTransform(
            w / 2 - (offsetX || 0),
            h / 2 - (offsetY || 0),
            1
        );
    };

    /**
     * Berilgan node ni ekran markaziga olib kelish.
     * @param {{ x, y, w, h }} pos - node pozitsiyasi
     */
    PanZoomUI.prototype.centerOnPos = function (pos) {
        if (!pos) return;
        var wW = this.wrap.clientWidth;
        var wH = this.wrap.clientHeight;
        var s = this.scale;
        this.setTransform(
            wW / 2 - (pos.x + pos.w / 2) * s,
            wH / 2 - (pos.y + pos.h / 2) * s,
            s
        );
    };

    /** Joriy scale ni qaytarish */
    PanZoomUI.prototype.getScale = function () {
        return this.scale;
    };

    // ════════════════════════════════════════════════
    //  ICHKI APPLY
    // ════════════════════════════════════════════════
    PanZoomUI.prototype._apply = function () {
        this.vp.style.transform =
            'translate(' + this.panX + 'px,' + this.panY + 'px)' +
            ' scale(' + this.scale + ')';

        // Zoom foizini yangilash uchun hodisa
        this.bus.emit('scaleChange', { scale: this.scale });
    };

    // ── Export ──
    ShTree.PanZoomUI = PanZoomUI;

}(window.ShTree = window.ShTree || {}));
