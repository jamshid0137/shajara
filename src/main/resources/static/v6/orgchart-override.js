// OrgChart.js _fromReqDTO override:
// OrgChart.js _setPositions() bu funksiyani (nodes, roots, configs, callback) bilan chaqiradi
// nodes = compact node array [{p:[id,pid,stpid,w,h], c:[childIds], ...}]
// roots = root ID array
// configs = layout config map {"base": [orientation, levelSep, ...]}
// ── shu formatni to'g'ridanto'g'ri Java serverga yuboriladigan qilamiz ──

OrgChart.remote._fromReqDTO = function (nodes, roots, configs, callback) {
    fetch('/api/layout1/calculate', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        // n, r, c — OrgChart.js compact DTO fieldlari (LayoutController kutayotgan format)
        body: JSON.stringify({ n: nodes, r: roots, c: configs })
    })
        .then(function (res) {
            if (!res.ok) {
                console.error('[layout1] HTTP ' + res.status);
                return null;
            }
            return res.json();
        })
        .then(function (result) {
            if (result) callback(result);
        })
        .catch(function (err) {
            console.error('[layout1] xato:', err);
        });
};