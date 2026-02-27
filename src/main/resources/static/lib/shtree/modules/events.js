/**
 * ShTree — EventBus (Hodisalar tizimi)
 * ======================================
 * Publish-Subscribe pattern.
 * Kutubxona ichidagi barcha modullar
 * bir-biri bilan shu orqali muloqot qiladi.
 *
 * Ishlatish:
 *   var bus = new ShTree.EventBus();
 *   bus.on('nodeClick', function(data) { ... });
 *   bus.emit('nodeClick', { node: n });
 *   bus.off('nodeClick', fn);
 */

(function (ShTree) {
    'use strict';

    /**
     * @constructor
     */
    function EventBus() {
        /** @type {Object.<string, Function[]>} */
        this._listeners = {};
    }

    /**
     * Hodisaga listener qo'shish.
     * @param {string}   type  - hodisa nomi
     * @param {Function} fn    - chaqiriladigan funksiya
     * @returns {EventBus}     - chain uchun
     */
    EventBus.prototype.on = function (type, fn) {
        if (!this._listeners[type]) {
            this._listeners[type] = [];
        }
        this._listeners[type].push(fn);
        return this;
    };

    /**
     * Barcha bir xil turdagi listenerlarni olib tashlash.
     * @param {string}   type
     * @param {Function} [fn]  - berilmasa, shu turdagi HAMMA listenerlar o'chiriladi
     * @returns {EventBus}
     */
    EventBus.prototype.off = function (type, fn) {
        if (!this._listeners[type]) return this;

        if (!fn) {
            // Hamma listener o'chadi
            this._listeners[type] = [];
        } else {
            this._listeners[type] = this._listeners[type].filter(function (f) {
                return f !== fn;
            });
        }
        return this;
    };

    /**
     * Hodisani ishga tushurish.
     * @param {string} type  - hodisa nomi
     * @param {*}      data  - listenerga beriladigan ma'lumot
     * @returns {boolean}    - biror listener false qaytarsa → false
     */
    EventBus.prototype.emit = function (type, data) {
        var fns = this._listeners[type] || [];
        var result = true;

        for (var i = 0; i < fns.length; i++) {
            var r = fns[i](data);
            if (r === false) result = false;
        }

        return result;
    };

    /**
     * Faqat bir marta ishlaydigan listener.
     * @param {string}   type
     * @param {Function} fn
     * @returns {EventBus}
     */
    EventBus.prototype.once = function (type, fn) {
        var self = this;
        var wrapper = function (data) {
            fn(data);
            self.off(type, wrapper);
        };
        return this.on(type, wrapper);
    };

    /**
     * Barcha listenerlarni tozalash.
     * @param {string} [type] - berilmasa hamma tozalanadi
     */
    EventBus.prototype.clear = function (type) {
        if (type) {
            this._listeners[type] = [];
        } else {
            this._listeners = {};
        }
    };

    // ── Export ──
    ShTree.EventBus = EventBus;

}(window.ShTree = window.ShTree || {}));
