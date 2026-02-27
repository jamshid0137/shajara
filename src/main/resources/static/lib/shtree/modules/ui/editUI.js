/**
 * ShTree — Edit UI
 * =================
 * Node ma'lumotlarini ko'rish/tahrirlash panel.
 * O'ng tomondan slayd bilan chiqadigan panel.
 *
 * Panel tashqaridan qo'lda yozilgan HTML element bo'lishi
 * yoki ShTree.EditUI tomonidan avtomatik yaratilishi mumkin.
 *
 * Ishlash:
 *   1. nodeClick hodisasi → panel ochiladi
 *   2. Panel ichida ma'lumot ko'rsatiladi
 *   3. Yopish tugmasi → yopiladi
 *
 * config.editPanelId = 'detailPanel'   → tegishli HTML element
 * config.editContentId = 'dpContent'   → ichki kontent div
 */

(function (ShTree) {
    'use strict';

    /**
     * @param {EventBus} bus
     * @param {Object}   config
     * @constructor
     */
    function EditUI(bus, config) {
        this.bus = bus;
        this.config = config;

        this.panelEl = document.getElementById(config.editPanelId || 'sh-edit-panel');
        this.contentEl = document.getElementById(config.editContentId || 'sh-edit-content');

        if (!this.panelEl) {
            this.panelEl = this._createPanel();
        }

        this._bindBus();
    }

    // ════════════════════════════════════════════════
    //  DOM YARATISH (agar panel HTML da yo'q bo'lsa)
    // ════════════════════════════════════════════════
    EditUI.prototype._createPanel = function () {
        var panel = document.createElement('div');
        panel.id = 'sh-edit-panel';
        panel.className = 'sh-edit-panel';
        panel.innerHTML =
            '<button class="sh-edit-close" id="sh-edit-close">✕</button>' +
            '<div id="sh-edit-content" class="sh-edit-content"></div>';
        document.body.appendChild(panel);

        this.contentEl = panel.querySelector('#sh-edit-content');

        var self = this;
        panel.querySelector('#sh-edit-close').addEventListener('click', function () {
            self.close();
        });

        return panel;
    };

    // ════════════════════════════════════════════════
    //  BUS ULASH
    // ════════════════════════════════════════════════
    EditUI.prototype._bindBus = function () {
        var self = this;

        // Node click → panel ochish
        this.bus.on('nodeClick', function (d) {
            self.open(d.node);
        });
        this.bus.on('details', function (d) {
            self.open(d.node);
        });

        // Boshqa node yuklanganda panelni yopish
        this.bus.on('loaded', function () {
            // Yuklanganda ham ochiq qolsin — muamllo yo'q
        });
    };

    // ════════════════════════════════════════════════
    //  OCHISH / YOPISH
    // ════════════════════════════════════════════════

    /**
     * Panelni ochish va node ma'lumotini ko'rsatish.
     * @param {Object} node - backend node
     */
    EditUI.prototype.open = function (node) {
        if (!this.panelEl || !this.contentEl) return;

        this.contentEl.innerHTML = this._buildContent(node);
        this.panelEl.classList.add('sh-edit-panel-open');
        this.bus.emit('editOpen', { node: node });
    };

    /** Panelni yopish */
    EditUI.prototype.close = function () {
        if (!this.panelEl) return;
        this.panelEl.classList.remove('sh-edit-panel-open');
        this.bus.emit('editClose', {});
    };

    /** Panel ochiqmi? */
    EditUI.prototype.isOpen = function () {
        return this.panelEl && this.panelEl.classList.contains('sh-edit-panel-open');
    };

    // ════════════════════════════════════════════════
    //  KONTENT YARATISH
    // ════════════════════════════════════════════════

    /**
     * Node ma'lumotlarini HTML ga aylantirish.
     * @param {Object} node
     * @returns {string} HTML
     */
    EditUI.prototype._buildContent = function (node) {
        var roleLbls = this.config.roleLbls || {};
        var initial = (node.name || '?').charAt(0).toUpperCase();

        var avatarHtml = node.photoUrl
            ? '<img src="' + node.photoUrl + '" alt="' + (node.name || '') + '">'
            : initial;

        var birth = this._fmtDate(node.birthDate);
        var died = this._fmtDate(node.diedDate);

        var genderLabel = node.gender === 'MALE' ? '👨 Erkak'
            : node.gender === 'FEMALE' ? '👩 Ayol'
                : "Noma'lum";

        var html =
            '<div class="sh-dp-avatar">' + avatarHtml + '</div>' +
            '<div class="sh-dp-name">' + (node.name || '—') + '</div>' +
            '<div class="sh-dp-role">' + (roleLbls[node.role] || node.role || '') + '</div>' +
            '<hr class="sh-dp-divider">' +
            '<div class="sh-dp-section">Ma\'lumotlar</div>';

        html += this._field('🎂', 'Tug\'ilgan', birth || "Noma'lum");
        if (died) html += this._field('🕊️', 'Vafot etgan', died);
        html += this._field('⚧', 'Jinsi', genderLabel);
        html += this._field('🆔', 'ID', String(node.id));

        return html;
    };

    /** Bitta ma'lumot satri HTML */
    EditUI.prototype._field = function (icon, label, value) {
        return '<div class="sh-dp-field">' +
            '<span class="sh-dp-icon">' + icon + '</span>' +
            '<div><div class="sh-dp-label">' + label + '</div>' +
            '<div class="sh-dp-val">' + value + '</div></div>' +
            '</div>';
    };

    /** Sana formatlash */
    EditUI.prototype._fmtDate = function (d) {
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

    // ── Export ──
    ShTree.EditUI = EditUI;

}(window.ShTree = window.ShTree || {}));
