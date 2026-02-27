/**
 * ShTree — Template: base
 * =========================
 * Asosiy tugun ko'rinishi.
 * Avatar (rasm yoki harf) + Ism + Rol + Sana.
 *
 * Ishlash tartibi:
 *   ShTree.templates['base'].renderNode(node, config)
 *   → tugun ichki HTML string qaytaradi
 *
 * Yangi template yaratish uchun shu faylni nusxalab
 * 'shajara.js' ga o'xshash alohida fayl yarating.
 */

(function (ShTree) {
    'use strict';

    // ════════════════════════════════════════════════
    //  YORDAMCHI FUNKSIYALAR (faqat template ichida)
    // ════════════════════════════════════════════════

    /**
     * Matnni qisqartirish.
     * @param {string} str
     * @param {number} max
     * @returns {string}
     */
    function trunc(str, max) {
        if (!str) return '—';
        return str.length > max ? str.slice(0, max) + '…' : str;
    }

    /**
     * LocalDate formatini o'qib inson o'qiy oladigan ko'rinishga o'girish.
     * Backend dan Java LocalDate quyidagi formatlarda kelishi mumkin:
     *   - Array: [2000, 5, 20]       → "20.05.2000"
     *   - String: "2000-05-20"       → "20.05.2000"
     *   - null / undefined           → ""
     * @param {*} d
     * @returns {string}
     */
    function fmtDate(d) {
        if (!d) return '';

        if (Array.isArray(d)) {
            var y = d[0];
            var m = String(d[1]).padStart(2, '0');
            var day = String(d[2]).padStart(2, '0');
            return day + '.' + m + '.' + y;
        }

        if (typeof d === 'string') {
            try {
                var dt = new Date(d);
                return dt.toLocaleDateString('uz-UZ', {
                    year: 'numeric',
                    month: '2-digit',
                    day: '2-digit'
                });
            } catch (e) {
                return d;
            }
        }

        return '';
    }

    /**
     * Placeholder HTML (rasm yo'q bo'lganda) — birinchi harf.
     * @param {string} name
     * @returns {string}
     */
    function avatarPlaceholder(name) {
        return (name || '?').charAt(0).toUpperCase();
    }

    // ════════════════════════════════════════════════
    //  BASE TEMPLATE
    // ════════════════════════════════════════════════
    ShTree.templates = ShTree.templates || {};

    ShTree.templates['base'] = {

        /**
         * Tugun ichki HTMLni yaratadi.
         * @param {Object} node   - backend node: {id,name,role,gender,birthDate,diedDate,photoUrl}
         * @param {Object} config - ShTree.config
         * @returns {string} HTML string
         */
        renderNode: function (node, config) {
            var roleLbls = config.roleLbls || {};

            var initial = avatarPlaceholder(node.name);
            var dateStr = fmtDate(node.birthDate);
            var diedStr = fmtDate(node.diedDate);
            var roleLabel = roleLbls[node.role] || node.role || '';

            // Avatar HTML
            var avatarInner = node.photoUrl
                ? '<img src="' + node.photoUrl + '" alt="' + trunc(node.name, 20) + '"' +
                ' onerror="this.parentElement.textContent=\'' + initial + '\'">'
                : initial;

            // Sana qatori
            var dateHtml = '';
            if (dateStr) {
                dateHtml = '<div class="sh-date">' + dateStr;
                if (diedStr) dateHtml += ' → ' + diedStr;
                dateHtml += '</div>';
            }

            return (
                '<div class="sh-avatar">' + avatarInner + '</div>' +
                '<div class="sh-info">' +
                '  <div class="sh-name">' + trunc(node.name, 18) + '</div>' +
                '  <div class="sh-lbl">' + roleLabel + '</div>' +
                dateHtml +
                '</div>'
            );
        }
    };

}(window.ShTree = window.ShTree || {}));
