/**
 * ShTree — Template: shajara
 * ===========================
 * Loyihaga maxsus dizayn:
 *   - Vafot etganlar uchun maxsus ko'rinish (qorong'i, qiya chiziq)
 *   - Kattaroq avatar (CENTER uchun)
 *   - Yillar ko'rsatish (masalan: 1980 – 2023)
 *   - Ikonlar bilan ma'lumot satri
 *
 * Ishlatish:
 *   new ShTree('#wrap', { template: 'shajara', ... })
 */

(function (ShTree) {
    'use strict';

    // ── Yordamchi funksiyalar ──

    function trunc(str, max) {
        if (!str) return '—';
        return str.length > max ? str.slice(0, max) + '…' : str;
    }

    function getYear(d) {
        if (!d) return '';
        if (Array.isArray(d)) return String(d[0]);
        if (typeof d === 'string') {
            var parts = d.split('-');
            return parts[0] || '';
        }
        return '';
    }

    function fmtDate(d) {
        if (!d) return '';
        if (Array.isArray(d)) {
            return String(d[2]).padStart(2, '0') + '.' +
                String(d[1]).padStart(2, '0') + '.' + d[0];
        }
        if (typeof d === 'string') {
            try {
                return new Date(d).toLocaleDateString('uz-UZ', {
                    year: 'numeric', month: '2-digit', day: '2-digit'
                });
            } catch (e) { return d; }
        }
        return '';
    }

    function avatarPlaceholder(name) {
        return (name || '?').charAt(0).toUpperCase();
    }

    // ════════════════════════════════════════════════
    //  SHAJARA TEMPLATE
    // ════════════════════════════════════════════════
    ShTree.templates = ShTree.templates || {};

    ShTree.templates['shajara'] = {

        renderNode: function (node, config) {
            var roleLbls = config.roleLbls || {};
            var initial = avatarPlaceholder(node.name);
            var roleLabel = roleLbls[node.role] || node.role || '';
            var isDead = !!node.diedDate;

            // Yillar satri
            var birthY = getYear(node.birthDate);
            var diedY = getYear(node.diedDate);
            var yearsHtml = '';
            if (birthY || diedY) {
                yearsHtml = '<div class="sh-years">';
                if (birthY) yearsHtml += '🎂 ' + birthY;
                if (diedY) yearsHtml += (birthY ? ' – ' : '† ') + diedY;
                yearsHtml += '</div>';
            }

            // Avatar
            var avatarInner = node.photoUrl
                ? '<img src="' + node.photoUrl + '" alt="' + initial + '"' +
                ' onerror="this.parentElement.textContent=\'' + initial + '\'">'
                : initial;

            // Vafot etgan belgisi
            var deadBadge = isDead
                ? '<span class="sh-dead-badge" title="Vafot etgan">†</span>'
                : '';

            return (
                '<div class="sh-avatar' + (isDead ? ' sh-avatar-dead' : '') + '">' +
                avatarInner + deadBadge +
                '</div>' +
                '<div class="sh-info">' +
                '  <div class="sh-name">' + trunc(node.name, 17) + '</div>' +
                '  <div class="sh-lbl sh-lbl-shajara">' + roleLabel + '</div>' +
                yearsHtml +
                '</div>'
            );
        }
    };

}(window.ShTree = window.ShTree || {}));
