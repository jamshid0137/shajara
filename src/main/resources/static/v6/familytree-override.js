// FamilyTree.js offline override v2
// FamilyTree.remote._fromReqDTO → lokal /api/layout1/calculate
// + DIAGNOSTIC LOGGING (F12 console da ko'ring)

(function () {
    if (typeof FamilyTree === 'undefined') {
        console.warn('[FT-override] FamilyTree yuklanmagan!');
        return;
    }
    if (!FamilyTree.remote) FamilyTree.remote = {};

    // _fromResDTO ni wrap qilib, har bir node uchun positions log qilamiz
    var _origFromResDTO = FamilyTree.remote._fromResDTO;

    FamilyTree.remote._fromReqDTO = function (nodes, roots, configs, callback) {
        console.group('[FT-override] Layout request');
        console.log('nodes count:', nodes.length);
        console.log('roots:', roots);
        console.log('first node:', nodes[0]);
        console.log('configs:', configs);
        console.groupEnd();

        fetch('/api/layout1/calculate', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ n: nodes, r: roots, c: configs })
        })
        .then(function (res) {
            if (!res.ok) {
                console.error('[FT-override] HTTP Error:', res.status, res.statusText);
                return null;
            }
            return res.json();
        })
        .then(function (result) {
            if (!result) {
                console.error('[FT-override] Server null response qaytardi!');
                return;
            }

            // ── DIAGNOSTIC: server response ni ko'ramiz ─────────────
            var keys = Object.keys(result);
            console.group('[FT-override] Server response: ' + keys.length + ' ta node');
            keys.slice(0, 5).forEach(function(k) {
                var p = result[k].p;
                console.log('  node ' + k + ' → x=' + p[0].toFixed(1) +
                            ', y=' + p[1].toFixed(1) +
                            ', w=' + p[2] + ', h=' + p[3]);
            });
            if (keys.length > 5) console.log('  ... va ' + (keys.length - 5) + ' ta ko\'proq');
            console.groupEnd();

            // ── DIAGNOSTIC: _fromResDTO OLDIDAN node.x/y ─────────────
            // (shajara internal node bo'lsa, _fromResDTO dan keyin o'zgaradi)
            console.log('[FT-override] _fromResDTO chaqirish oldidan rootlar:', roots);

            // Wrap _fromResDTO to catch errors
            var safeCallback = function(res) {
                try {
                    callback(res);
                } catch (err) {
                    console.error('[FT-override] callback() ichida xato:', err);
                }
            };

            safeCallback(result);
        })
        .catch(function (err) {
            console.error('[FT-override] Fetch xatosi:', err);
        });
    };

    // _fromResDTO ni patch qilib xatolar uchun null check qo'shamiz
    FamilyTree.remote._fromResDTO = function (e, t, i, r, a) {
        if (!e) return;
        var n = t[e.id];
        if (!n) {
            console.warn('[FT-override] _fromResDTO: response da node topilmadi! id=' + e.id +
                         ', type=' + typeof e.id);
            // Fallback: stChildren va children ni ham traversal qilamiz
            if (e.stChildren) for (var o = 0; o < e.stChildren.length; o++)
                FamilyTree.remote._fromResDTO(e.stChildren[o], t, i, r, a);
            if (e.children) for (o = 0; o < e.children.length; o++)
                FamilyTree.remote._fromResDTO(e.children[o], t, i, r, a);
            return;
        }
        // Original logic
        e.x = n.p[0];
        e.y = n.p[1];
        e.w = n.p[2];
        e.h = n.p[3];
        if (null != n.ln) e.leftNeighbor  = a ? a[n.ln]  : null;
        if (null != n.rn) e.rightNeighbor = a ? a[n.rn] : null;

        if (e.stChildren) for (var o = 0; o < e.stChildren.length; o++)
            FamilyTree.remote._fromResDTO(e.stChildren[o], t, i, r, a);
        if (e.children) for (o = 0; o < e.children.length; o++)
            FamilyTree.remote._fromResDTO(e.children[o], t, i, r, a);
    };

    console.log('[FT-override v2] FamilyTree.remote._fromReqDTO → /api/layout1/calculate (oflayn+diagnoz)');
})();
