/* ================================================================
   SHAJARA — app3.js
   ──────────────────────────────────────────────────────────────
   MAQSAD:
     tree.html  → bitta markaz person atrofida layout
                  (GET /api/layout/person/{id})
     tree3.html → Family Tree ID berilganda SHU TREE DAGI
                  BARCHA personlarni FamilyTree.js ga uzatish
                  (GET /api/persons/tree/{treeId}
                   GET /api/relations/tree/{treeId})

   YONDASHUV:
     1. /api/persons/tree/{treeId}  →  [{id, name, gender, fatherId, motherId, ...}]
     2. /api/relations/tree/{treeId} →  [{fromPersonId, toPersonId, type}]
     3. JS MAPPER: har bir person uchun FamilyTree.js format yasaymiz:
        { id, fid, mid, pids[], name, tags, photoUrl, ... }
     4. chart.load(nodes) — kutubxona layoutni o'zi hisoblaydi

   tree.html dan farq:
     - Layout hisoblash backendda emas, FamilyTree.js da
     - Barcha oila a'zolari bir safar yuklanadi
     - Markaz person tushunchasi yo'q — hammasi ko'rinadi
   ================================================================ */

'use strict';

/* ────────────────────────────────────────────────────────────
   GLOBAL HOLATlar
──────────────────────────────────────────────────────────── */
var chart = null;

/* Yuklangan nodelar keshi { personId → ftNode } */
var _loadedNodes = {};

/* personId → treeId xaritasi */
var _nodeTreeIdMap = {};

/* Pending child qo'shish holati */
var _pendingChildId = null;
var _pendingChildTreeId = null;
var _pendingChildSpouseId = null;

/* ────────────────────────────────────────────────────────────
   INIT — sahifa yuklanganda
──────────────────────────────────────────────────────────── */
window.addEventListener('load', function () {
    showLoading(false);

    /* URL parametrlarini olamiz */
    var urlToken  = getParam('token');
    var urlTreeId = getParam('treeId') || getParam('id');

    if (urlToken)  document.getElementById('tokenInput').value  = urlToken;
    if (urlTreeId) document.getElementById('treeIdInput').value = urlTreeId;

    initChart();

    /* Ikkisi ham bo'lsa — avtomatik yuklash */
    if (urlToken && urlTreeId) loadFullTree();
});

/* ────────────────────────────────────────────────────────────
   FamilyTree.js INITSIALIZATSIYA
──────────────────────────────────────────────────────────── */
function initChart() {
    if (typeof FamilyTree === 'undefined') {
        showError('FamilyTree.js kutubxonasi yuklanmadi! /lib/familytree.js tekshiring.');
        return;
    }

    chart = new FamilyTree(document.getElementById('tree'), {
        mode: 'dark',
        template: 'tommy',
        enableSearch: true,
        enablePan: true,
        mouseScrool: FamilyTree.action.zoom,

        nodeBinding: {
            field_0: 'name',
            field_1: 'roleLabel',
            field_2: 'years',
            img_0:   'photoUrl'
        },

        editForm: {
            readOnly: false,
            titleBinding: 'name',
            photoBinding: 'photoUrl',
            buttons: {
                edit:    { icon: FamilyTree.icon.edit(24, 24, '#fff'),    text: 'Tahrirlash' },
                details: { icon: FamilyTree.icon.details(24, 24, '#fff'), text: 'Batafsil'   },
                remove:  { icon: FamilyTree.icon.remove(24, 24, '#fff'),  text: "O'chirish"  }
            },
            elements: [
                { type: 'textbox', label: 'Ism',        binding: 'name'      },
                { type: 'textbox', label: "Tug'ilgan",  binding: 'birthDate' },
                { type: 'textbox', label: 'Vafot',      binding: 'diedDate'  },
                { type: 'textbox', label: 'Rasm URL',   binding: 'photoUrl'  }
            ]
        },

        toolbar: { zoom: true, fit: true, fullScreen: true },

        nodeTreeMenu: true,

        /* ── Node o'ng-klik menyu ── */
        nodeMenu: {
            focusMenu: {
                /* tree3.html FARQI: hammasi ko'rinadi — "Ko'rsatish" o'sha nodega FOKUS qiladi */
                text: "🎯 Fokus",
                onClick: function (nodeId) {
                    if (chart) { chart.center(nodeId); }
                }
            },
            addParentMenu: {
                text: "🔼 Ota-ona qo'sh",
                onClick: function (nodeId) { addParent(nodeId); }
            },
            addSpouseMenu: {
                text: "💑 Juft qo'sh",
                onClick: function (nodeId) { addSpouse(nodeId); }
            },
            addChildMenu: {
                text: "🔽 Farzand qo'sh",
                onClick: function (nodeId) {
                    _pendingChildId       = nodeId;
                    _pendingChildSpouseId = null;
                    _pendingChildTreeId   = _nodeTreeIdMap[nodeId] || Number(getTreeId());
                    document.getElementById('genderPopup').classList.remove('hidden');
                }
            },
            details: { text: '📋 Batafsil' }
        },

        nodes: []
    });

    /* ── UPDATE va DELETE eventlari ── */
    chart.on('update', function (sender, args) {

        /* ─── ADD ─── */
        if (args.addNodesData && args.addNodesData.length > 0) {
            var addedNode = args.addNodesData[0];
            console.log('[update ADD] id:', addedNode.id,
                        '| fid:', addedNode.fid, '| mid:', addedNode.mid,
                        '| pids:', JSON.stringify(addedNode.pids));

            var isParent = false, isChild = false, isSpouse = false;
            var baseNodeId = null;

            /* 1️⃣ PARENT tekshiruvi */
            if (args.updateNodesData && args.updateNodesData.length > 0) {
                for (var i = 0; i < args.updateNodesData.length; i++) {
                    var uNode = args.updateNodesData[i];
                    if (String(uNode.fid) === String(addedNode.id) ||
                        String(uNode.mid) === String(addedNode.id)) {
                        isParent   = true;
                        baseNodeId = uNode.id;
                        break;
                    }
                }
            }

            /* 2️⃣ SPOUSE tekshiruvi */
            if (!isParent && addedNode.pids && addedNode.pids.length > 0) {
                for (var pi = 0; pi < addedNode.pids.length; pi++) {
                    if (_nodeTreeIdMap[addedNode.pids[pi]]) {
                        isSpouse   = true;
                        baseNodeId = addedNode.pids[pi];
                        break;
                    }
                }
            }

            /* 3️⃣ CHILD tekshiruvi */
            var hasFid = !isParent && !isSpouse &&
                         addedNode.fid && addedNode.fid != 0 && _nodeTreeIdMap[addedNode.fid];
            var hasMid = !isParent && !isSpouse &&
                         addedNode.mid && addedNode.mid != 0 && _nodeTreeIdMap[addedNode.mid];

            if (!isParent && !isSpouse && (hasFid || hasMid)) {
                isChild    = true;
                baseNodeId = hasFid ? addedNode.fid : addedNode.mid;
            }

            console.log('[update ADD] → isParent:', isParent, '| isSpouse:', isSpouse,
                        '| isChild:', isChild, '| baseNodeId:', baseNodeId);

            if (isParent && baseNodeId) {
                addParent(baseNodeId);
                return false;
            } else if (isSpouse && baseNodeId) {
                addSpouse(baseNodeId);
                return false;
            } else if (isChild && baseNodeId) {
                var tags    = addedNode.tags || [];
                var gField  = (addedNode.gender || '').toLowerCase();
                var isFemale = tags.indexOf('female') !== -1
                    || tags.indexOf('daughter') !== -1
                    || gField === 'female' || gField === 'f';
                var gender = isFemale ? 'FEMALE' : 'MALE';

                _pendingChildId      = baseNodeId;
                _pendingChildTreeId  = _nodeTreeIdMap[baseNodeId] || Number(getTreeId());
                _pendingChildSpouseId = (hasFid && hasMid) ? addedNode.mid : null;
                confirmChild(gender);
                return false;
            }
        }

        /* ─── DELETE ─── */
        if (args.removeNodeId !== undefined && args.removeNodeId !== null) {
            var delId = args.removeNodeId;
            if (!confirm("Haqiqatan ham bu odamni o'chirmoqchimisiz?\nID: " + delId)) {
                return false;
            }
            apiFetch('DELETE', '/api/persons/' + delId, null,
                function () { reloadTree(); },
                function (err) { showError("O'chirishda xatolik:\n" + err); }
            );
            return true;
        }

        /* ─── UPDATE ─── */
        var node = args.updateNodesData && args.updateNodesData[0];
        if (!node || !node.id) return true;

        apiFetch('PUT', '/api/persons/' + node.id,
            {
                name:      node.name      || null,
                birthDate: node.birthDate || null,
                diedDate:  node.diedDate  || null,
                photoUrl:  node.photoUrl  || null
            },
            function () { console.log('Person ' + node.id + ' yangilandi'); },
            function (err) { showError('Saqlashda xatolik:\n' + err); }
        );
        return true;
    });
}

/* ────────────────────────────────────────────────────────────
   ASOSIY FUNKSIYA: BARCHA PERSONLARNI YUKLASH
   ──────────────────────────────────────────────────────────
   tree.html dan FARQI:
     tree.html  → GET /api/layout/person/{id}   (bitta markaz)
     tree3.html → GET /api/persons/tree/{treeId}  (hammasi)
                + GET /api/relations/tree/{treeId} (spouse bog'lanishlar)
──────────────────────────────────────────────────────────── */
function loadFullTree() {
    var token  = getToken();
    var treeId = getTreeId();

    if (!token)  { showError('Iltimos, Bearer tokenni kiriting!'); return; }
    if (!treeId) { showError('Iltimos, Tree ID ni kiriting!');      return; }

    showLoading(true);
    hideError();
    _loadedNodes    = {};
    _nodeTreeIdMap  = {};

    /* Ikki so'rovni parallel bajaramiz:
       1) Barcha personlar
       2) Barcha relationlar (SPOUSE uchun)                        */
    var personsUrl   = '/api/persons/tree/' + treeId;
    var relationsUrl = '/api/relations/tree/' + treeId;

    var token_ = token;

    /* 3-chi so'rov: tree nomi uchun family-tree ma'lumotlari */
    var treeInfoUrl = '/api/family-trees/' + treeId;

    Promise.all([
        fetch(personsUrl,   { headers: { 'Authorization': 'Bearer ' + token_ } }),
        fetch(relationsUrl, { headers: { 'Authorization': 'Bearer ' + token_ } }),
        fetch(treeInfoUrl,  { headers: { 'Authorization': 'Bearer ' + token_ } })
    ])
    .then(function (responses) {
        return Promise.all(responses.map(function (r) {
            if (!r.ok) throw new Error('HTTP ' + r.status + ' (' + r.url + ')');
            return r.json();
        }));
    })
    .then(function (results) {
        showLoading(false);
        var persons   = results[0]; /* PersonResponseDto[]   */
        var relations = results[1]; /* RelationResponseDto[] */
        var treeInfo  = results[2]; /* FamilyTreeDto         */
        mapAndRender(persons, relations, Number(treeId), treeInfo && treeInfo.name);
    })
    .catch(function (e) {
        showLoading(false);
        showError('Yuklashda xatolik:\n' + (e.message || e));
    });
}

/* Qayta yuklash */
function reloadTree() { loadFullTree(); }

/* ────────────────────────────────────────────────────────────
   JS MAPPER
   ──────────────────────────────────────────────────────────
   Kirish:
     persons[]   — PersonResponseDto
       { id, name, gender, fatherId, motherId, photoUrl,
         birthDate, diedDate, treeId }
     relations[] — RelationResponseDto
       { id, fromPersonId, toPersonId, type }

   Chiqish: FamilyTree.js nodes[]
     { id, fid, mid, pids[], name, roleLabel, years,
       birthDate, diedDate, photoUrl, tags[] }
──────────────────────────────────────────────────────────── */
function mapAndRender(persons, relations, treeId, treeName) {

    if (!persons || persons.length === 0) {
        showError("Bu tree bo'sh yoki ma'lumot topilmadi.");
        return;
    }

    /* ── 1. Person ID lari to'plami (fid/mid filtri uchun) ── */
    var personIds = {};
    persons.forEach(function (p) { personIds[p.id] = true; });

    /* ── 2. Spouse xaritasi: id → [spouseId, ...] ── */
    var spouseMap = {};
    relations.forEach(function (r) {
        if ((r.type || '').toUpperCase() !== 'SPOUSE') return;
        var fId = r.fromPersonId, tId = r.toPersonId;
        if (!personIds[fId] || !personIds[tId]) return;

        if (!spouseMap[fId]) spouseMap[fId] = [];
        if (!spouseMap[tId]) spouseMap[tId] = [];
        if (spouseMap[fId].indexOf(tId) < 0) spouseMap[fId].push(tId);
        if (spouseMap[tId].indexOf(fId) < 0) spouseMap[tId].push(fId);
    });

    /* ── 3. FamilyTree.js nodes yasash ── */
    var ftNodes = persons.map(function (p) {

        var fid = (p.fatherId && personIds[p.fatherId]) ? p.fatherId : null;
        var mid = (p.motherId && personIds[p.motherId]) ? p.motherId : null;
        var pids = spouseMap[p.id] || [];

        /* Yillar */
        var years = '';
        if (p.birthDate || p.diedDate) {
            years = (p.birthDate ? getYear(p.birthDate) : '?')
                  + (p.diedDate  ? ' – ' + getYear(p.diedDate) : '');
        }

        var isFemale = (p.gender || '').toUpperCase() === 'FEMALE';

        /* roleLabel: ota-ona belgisi (fid/mid bo'lsa farzand, pids bo'lsa juft) */
        var roleLabel = '';
        if (pids.length > 0 && !fid && !mid) roleLabel = isFemale ? '👩 Ona' : '👨 Ota';
        else if (fid || mid) roleLabel = isFemale ? '👧 Qiz' : '👦 O\'g\'il';

        return {
            id:        p.id,
            fid:       fid,
            mid:       mid,
            pids:      pids,
            name:      p.name || '—',
            roleLabel: roleLabel,
            years:     years,
            birthDate: p.birthDate ? String(p.birthDate) : '',
            diedDate:  p.diedDate  ? String(p.diedDate)  : '',
            photoUrl:  p.photoUrl  || null,
            tags:      [isFemale ? 'female' : 'male']
        };
    });

    /* ── 4. Keshni yangilaymiz ── */
    ftNodes.forEach(function (n) {
        _loadedNodes[n.id]   = n;
        _nodeTreeIdMap[n.id] = treeId;
    });

    /* ── 5. FamilyTree.js ga yuklash ── */
    if (!chart) { showError('Chart initsializatsiya qilinmagan!'); return; }
    chart.load(ftNodes);

    /* ── 6. Sarlavhani yangilash ── */
    if (treeName) {
        document.title = '🌳 ' + treeName + ' — Shajara v3';
        var logoEl = document.getElementById('treeName');
        if (logoEl) logoEl.textContent = treeName;
    }

    /* ── 7. Statistika ── */
    var maleCount   = persons.filter(function (p) { return (p.gender||'').toUpperCase() === 'MALE'; }).length;
    var femaleCount = persons.filter(function (p) { return (p.gender||'').toUpperCase() === 'FEMALE'; }).length;
    var spouseCount = relations.filter(function (r) { return (r.type||'').toUpperCase() === 'SPOUSE'; }).length;

    document.getElementById('sTotal').textContent  = persons.length;
    document.getElementById('sMale').textContent   = maleCount;
    document.getElementById('sFemale').textContent = femaleCount;
    document.getElementById('sSpouse').textContent = spouseCount;
    document.getElementById('stats').style.display = 'flex';

    console.log('[tree3] Yuklandi: ' + ftNodes.length + ' person, ' + spouseCount + ' juftlik');
}

/* ────────────────────────────────────────────────────────────
   API ACTIONS (tree.html bilan bir xil)
──────────────────────────────────────────────────────────── */
function addParent(nodeId) {
    var treeId = _nodeTreeIdMap[nodeId] || Number(getTreeId());
    if (!treeId) { showError('Tree ID topilmadi!'); return; }

    apiFetch('POST', '/api/persons/add-parent',
        { id: Number(nodeId), fatherId: null, motherId: null, treeId: treeId },
        function () { reloadTree(); },
        function (err) { showError("Ota-ona qo'shishda xatolik:\n" + err); }
    );
}

function addSpouse(nodeId) {
    var treeId = _nodeTreeIdMap[nodeId] || Number(getTreeId());
    if (!treeId) { showError('Tree ID topilmadi!'); return; }

    apiFetch('POST', '/api/persons/add-spouse',
        { id: Number(nodeId), treeId: treeId },
        function () { reloadTree(); },
        function (err) { showError("Juft qo'shishda xatolik:\n" + err); }
    );
}

function confirmChild(gender) {
    closeGender();
    if (!_pendingChildId) { showError('Node tanlanmagan!'); return; }

    var treeId = _nodeTreeIdMap[_pendingChildId] || _pendingChildTreeId;
    if (!treeId) { showError('Tree ID topilmadi!'); return; }

    apiFetch('POST', '/api/persons/add-child',
        {
            id:          Number(_pendingChildId),
            spouseId:    _pendingChildSpouseId ? Number(_pendingChildSpouseId) : null,
            childGender: gender,
            treeId:      treeId
        },
        function () {
            _pendingChildId      = null;
            _pendingChildTreeId  = null;
            _pendingChildSpouseId = null;
            reloadTree();
        },
        function (err) { showError("Farzand qo'shishda xatolik:\n" + err); }
    );
}

function closeGender() {
    document.getElementById('genderPopup').classList.add('hidden');
}

/* ────────────────────────────────────────────────────────────
   UNIVERSAL FETCH HELPER
──────────────────────────────────────────────────────────── */
function apiFetch(method, url, body, onOk, onErr) {
    var token = getToken();
    if (!token) { onErr('Token kiritilmagan!'); return; }

    showLoading(true);

    var opts = {
        method: method,
        headers: {
            'Authorization': 'Bearer ' + token,
            'Content-Type':  'application/json'
        }
    };
    if (body && (method === 'POST' || method === 'PUT')) {
        opts.body = JSON.stringify(body);
    }

    fetch(url, opts)
        .then(function (res) {
            showLoading(false);
            if (!res.ok) {
                return res.text().then(function (t) {
                    try {
                        var j = JSON.parse(t);
                        t = j.message || j.error || JSON.stringify(j);
                    } catch (e) { /* plain text */ }
                    onErr(res.status + ' ' + res.statusText + ':\n' + t);
                });
            }
            if (res.status === 204) { onOk(null); return; }
            var ct = res.headers.get('content-type') || '';
            if (ct.indexOf('application/json') >= 0) {
                return res.json().then(function (d) { onOk(d); });
            }
            return res.text().then(function () { onOk(null); });
        })
        .catch(function (e) {
            showLoading(false);
            onErr('Tarmoq xatosi: ' + (e.message || e));
        });
}

/* ────────────────────────────────────────────────────────────
   HELPERS
──────────────────────────────────────────────────────────── */
function getYear(d) {
    if (!d) return '';
    if (Array.isArray(d)) return String(d[0]);
    if (typeof d === 'string') return d.substring(0, 4);
    return '';
}

function showLoading(v) {
    document.getElementById('loading').classList.toggle('hidden', !v);
}

function showError(msg) {
    document.getElementById('errorMsg').textContent = msg;
    document.getElementById('errorModal').classList.add('show');
}

function hideError() {
    document.getElementById('errorModal').classList.remove('show');
}

function getToken()  { return document.getElementById('tokenInput').value.trim(); }

function getTreeId() {
    return document.getElementById('treeIdInput').value.trim()
        || getParam('treeId')
        || getParam('id')
        || '';
}

function getParam(k) {
    return new URLSearchParams(location.search).get(k);
}
