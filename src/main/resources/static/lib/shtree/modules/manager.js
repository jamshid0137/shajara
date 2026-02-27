/**
 * ShTree — Manager (Ma'lumot va Layout)
 * =======================================
 * Backend JSON formatini qabul qiladi:
 *   { nodes: [...], connections: [...], minX, maxX, minY, maxY }
 *
 * Va internal ishlash uchun:
 *   - Pozitsiya xaritasini (posMap) yaratadi
 *   - SVG o'lchamlarini (svgW, svgH) hisoblaydi
 *   - Chegara (boundary) ma'lumotlarini saqlaydi
 *
 * ESLATMA: Layout algoritmi BACKEND da (Java TreeLayoutService)
 * ishlaydi. Manager faqat tayyor x,y koordinatlarini qayta ishlaydi.
 */

(function (ShTree) {
    'use strict';

    /**
     * @param {Object} config - ShTree.config
     * @constructor
     */
    function Manager(config) {
        this.config = config;
        this._rawData = null;  // oxirgi yuklangan backend JSON
        this._posMap = {};    // { id → {x,y,w,h,cx,cy} }
        this._boundary = {};    // {minX,maxX,minY,maxY,svgW,svgH}
    }

    // ════════════════════════════════════════════════
    //  ASOSIY METOD: process(data)
    //  Backend JSON ni ichki formatga aylantiradi
    // ════════════════════════════════════════════════
    /**
     * @param {Object}   data     - backend JSON
     * @param {Function} callback - fn(posMap, boundary, rawData)
     */
    Manager.prototype.process = function (data, callback) {
        if (!data || !data.nodes || !data.nodes.length) {
            if (callback) callback(null, null, null);
            return;
        }

        this._rawData = data;
        this._posMap = this._buildPosMap(data.nodes);
        this._boundary = this._calcBoundary(data);

        if (callback) {
            callback(this._posMap, this._boundary, data);
        }
    };

    // ════════════════════════════════════════════════
    //  POZITSIYA XARITASI
    //  Har bir node uchun ekran koordinatalarini hisoblaydi
    // ════════════════════════════════════════════════
    /**
     * @param {Array} nodes - backend nodes massivi
     * @returns {Object} posMap - { id: {x,y,w,h,cx,cy} }
     */
    Manager.prototype._buildPosMap = function (nodes) {
        var cfg = this.config;
        var posMap = {};
        var ox = cfg.offsetX;   // gorizontal offset (viewport markazlash uchun)
        var oy = cfg.offsetY;   // vertikal offset

        nodes.forEach(function (n) {
            // CENTER node kattaroq
            var w = (n.role === 'CENTER') ? cfg.centerNodeW : cfg.nodeW;
            var h = (n.role === 'CENTER') ? cfg.centerNodeH : cfg.nodeH;
            var sx = n.x + ox;   // svg koordinatasi
            var sy = n.y + oy;

            posMap[n.id] = {
                x: sx,          // chap-yuqori burchak
                y: sy,
                w: w,           // eni
                h: h,           // bo'yi
                cx: sx + w / 2,  // markaziy x
                cy: sy + h / 2   // markaziy y
            };
        });

        return posMap;
    };

    // ════════════════════════════════════════════════
    //  CHEGARA VA SVG O'LCHAMLARI
    // ════════════════════════════════════════════════
    /**
     * @param {Object} data - backend JSON
     * @returns {{ minX,maxX,minY,maxY,svgW,svgH }}
     */
    Manager.prototype._calcBoundary = function (data) {
        var cfg = this.config;
        var ox = cfg.offsetX;
        var oy = cfg.offsetY;

        // Backend chegaralar
        var bMinX = data.minX !== undefined ? data.minX : 0;
        var bMaxX = data.maxX !== undefined ? data.maxX : 800;
        var bMinY = data.minY !== undefined ? data.minY : 0;
        var bMaxY = data.maxY !== undefined ? data.maxY : 600;

        // SVG kenglik/balandlik
        var svgW = Math.abs(bMaxX - bMinX) + ox * 2 + 800;
        var svgH = Math.abs(bMaxY - bMinY) + oy * 2 + 800;

        return {
            minX: bMinX,
            maxX: bMaxX,
            minY: bMinY,
            maxY: bMaxY,
            svgW: svgW,
            svgH: svgH
        };
    };

    // ════════════════════════════════════════════════
    //  GETTERLAR
    // ════════════════════════════════════════════════
    /** Joriy posMap ni qaytaradi */
    Manager.prototype.getPosMap = function () {
        return this._posMap;
    };

    /** Joriy chegara ma'lumotlarini qaytaradi */
    Manager.prototype.getBoundary = function () {
        return this._boundary;
    };

    /** Joriy raw JSON ni qaytaradi */
    Manager.prototype.getRawData = function () {
        return this._rawData;
    };

    /** Node ni id bo'yicha topish */
    Manager.prototype.getNode = function (id) {
        if (!this._rawData) return null;
        return this._rawData.nodes.find(function (n) {
            return String(n.id) === String(id);
        }) || null;
    };

    /** Bitta node ning pozitsiyasini qaytarish */
    Manager.prototype.getPos = function (id) {
        return this._posMap[id] || null;
    };

    /** Statistika */
    Manager.prototype.getStats = function () {
        if (!this._rawData) return { total: 0, spouses: 0, children: 0, connections: 0 };
        var nodes = this._rawData.nodes;
        return {
            total: nodes.length,
            spouses: nodes.filter(function (n) { return n.role === 'SPOUSE'; }).length,
            children: nodes.filter(function (n) { return n.role === 'CHILD'; }).length,
            connections: (this._rawData.connections || []).length
        };
    };

    /** Holatni tozalash */
    Manager.prototype.reset = function () {
        this._rawData = null;
        this._posMap = {};
        this._boundary = {};
    };

    // ── Export ──
    ShTree.Manager = Manager;

}(window.ShTree = window.ShTree || {}));
