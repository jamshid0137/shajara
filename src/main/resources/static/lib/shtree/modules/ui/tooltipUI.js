/**
 * ShTree — Tooltip UI
 * ====================
 * Node ustiga sichqoncha borganda
 * qisqacha ma'lumot ko'rsatuvchi tooltip.
 *
 * Barcha tooltiplar bitta global element orqali ishlaydi
 * (her node uchun alohida yaratilmaydi).
 */

(function (ShTree) {
    'use strict';

    /**
     * @param {EventBus} bus
     * @param {Object}   config
     * @constructor
     */
    function TooltipUI(bus, config) {
        this.config = config;
        this.bus = bus;
        this.el = null;
        this._create();
        this._bindBus();
    }

    /** DOM element yaratish */
    TooltipUI.prototype._create = function () {
        var el = document.createElement('div');
        el.className = 'sh-tooltip';
        el.id = 'sh-tooltip';
        document.body.appendChild(el);
        this.el = el;
    };

    /** EventBus ga ulash */
    TooltipUI.prototype._bindBus = function () {
        var self = this;
        this.bus.on('nodeEnter', function (d) { self.show(d.node, d.event); });
        this.bus.on('nodeMove', function (d) { self.move(d.event); });
        this.bus.on('nodeLeave', function () { self.hide(); });
    };

    /** Tooltipni ko'rsatish */
    TooltipUI.prototype.show = function (node, e) {
        var roleLbls = this.config.roleLbls || {};

        var birth = this._fmtDate(node.birthDate);
        var died = this._fmtDate(node.diedDate);

        this.el.innerHTML =
            '<div class="sh-tt-name">' + (node.name || '—') + '</div>' +
            '<div class="sh-tt-info">' +
            "Ro'li: " + (roleLbls[node.role] || node.role || '—') + '<br>' +
            'ID: ' + node.id +
            (birth ? '<br>Tug\'ilgan: ' + birth : '') +
            (died ? '<br>Vafot etgan: ' + died : '') +
            '</div>';

        this.el.classList.add('sh-tt-show');
        this.move(e);
    };

    /** Tooltipni yopish */
    TooltipUI.prototype.hide = function () {
        this.el.classList.remove('sh-tt-show');
    };

    /** Sichqoncha harakat qilganda pozitsiyani yangilash */
    TooltipUI.prototype.move = function (e) {
        if (!e) return;
        var x = e.clientX + 16;
        var y = e.clientY - 10;

        // Ekran chegarasidan chiqmasligi uchun
        var w = this.el.offsetWidth || 200;
        var h = this.el.offsetHeight || 60;
        if (x + w > window.innerWidth) x = e.clientX - w - 10;
        if (y + h > window.innerHeight) y = e.clientY - h - 10;

        this.el.style.left = x + 'px';
        this.el.style.top = y + 'px';
    };

    /** Sana formatlash */
    TooltipUI.prototype._fmtDate = function (d) {
        if (!d) return '';
        if (Array.isArray(d)) {
            return String(d[2]).padStart(2, '0') + '.' +
                String(d[1]).padStart(2, '0') + '.' + d[0];
        }
        if (typeof d === 'string') {
            try { return new Date(d).toLocaleDateString('uz-UZ'); }
            catch (e) { return d; }
        }
        return '';
    };

    /** Tozalash */
    TooltipUI.prototype.destroy = function () {
        if (this.el && this.el.parentNode) {
            this.el.parentNode.removeChild(this.el);
        }
        this.bus.off('nodeEnter');
        this.bus.off('nodeMove');
        this.bus.off('nodeLeave');
    };

    // ── Export ──
    ShTree.TooltipUI = TooltipUI;

}(window.ShTree = window.ShTree || {}));
