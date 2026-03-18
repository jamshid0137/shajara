// FamilyTree.js offline override
// FamilyTree.remote._fromReqDTO ni Balkan Azure serveri o'rniga
// bizning lokal /api/layout1/calculate ga yo'naltiradi.
//
// FamilyTree.js _setPositions() bu funksiyani quyidagilar bilan chaqiradi:
//   nodes  = compact node array [{p:[id,pid,stpid,w,h], c:[childIds], ...}]
//   roots  = root ID array
//   configs = layout config map {"base": [orientation, levelSep, ...]}
//
// Javob OrgChart.remote._fromResDTO tomonidan qabul qilinadi:
//   { nodeId: {p:[x,y,w,h], ln: leftNeighId, rn: rightNeighId}, ... }

(function () {
    // FamilyTree.remote mavjud bo'lmasa yaratamiz
    if (typeof FamilyTree === 'undefined') {
        console.warn('[familytree-override] FamilyTree yuklanmagan, override o\'tkazib yuborildi');
        return;
    }
    if (!FamilyTree.remote) FamilyTree.remote = {};

    FamilyTree.remote._fromReqDTO = function (nodes, roots, configs, callback) {
        fetch('/api/layout1/calculate', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            // n, r, c — compact DTO fieldlari (LayoutController kutayotgan format)
            body: JSON.stringify({ n: nodes, r: roots, c: configs })
        })
            .then(function (res) {
                if (!res.ok) {
                    console.error('[familytree-override] HTTP ' + res.status);
                    return null;
                }
                return res.json();
            })
            .then(function (result) {
                if (result) callback(result);
            })
            .catch(function (err) {
                console.error('[familytree-override] Tarmoq xatosi:', err);
            });
    };

    console.log('[familytree-override] FamilyTree.remote._fromReqDTO → /api/layout1/calculate (oflayn rejim)');
})();
