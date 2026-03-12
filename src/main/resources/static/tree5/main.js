const container = document.getElementById('tree-container');
let width = container.clientWidth;
let height = container.clientHeight;

const margin = { top: 60, right: 120, bottom: 60, left: 120 };
const nodeWidth = 220;
const nodeHeight = 80;

const zoom = d3.zoom()
    .scaleExtent([0.1, 3])
    .on("zoom", (event) => {
        g.attr("transform", event.transform);
    });

const svg = d3.select("#tree-container").append("svg")
    .attr("width", "100%")
    .attr("height", "100%")
    .call(zoom);

const g = svg.append("g");

function centerRoot() {
    svg.transition().duration(750).call(
        zoom.transform, 
        d3.zoomIdentity.translate(width / 2, height / 2).scale(1)
    );
}

// Chiziqlarni chizish funksiyasi
function drawLinks(connections, nodesMap) {
    const link = g.selectAll('path.link')
        .data(connections);

    link.enter().insert('path', "g")
        .attr("class", "link")
        .style("stroke-width", d => d.type === 'SPOUSE' ? '2px' : '3px')
        .style("stroke", d => d.type === 'SPOUSE' ? '#c084fc' : '#475569') // Turmush o'rtog'ini alohida rang bilan ajratish
        .style("stroke-dasharray", d => d.type === 'SPOUSE' ? "5,5" : "none") // Turmush o'rtog'ini chizig'iga nuqta-nuqta (dash) qilib berish
        .attr('d', d => {
            const source = nodesMap.get(d.fromId);
            const target = nodesMap.get(d.toId);
            if (!source || !target) return "";

            // X va Y lar backenddan keladi.
            const sX = source.x;
            const sY = source.y;
            const tX = target.x;
            const tY = target.y;

            if (d.type === 'SPOUSE') {
                // Er va hotin rasmlari yonma yon bo'ladi
                return `M ${sX} ${sY} L ${tX} ${tY}`;
            } else {
                // Ota-onadan farzandga egri chiziq
                const halfway = (sY + tY) / 2;
                return `M ${sX} ${sY + nodeHeight / 2}
                        C ${sX}   ${halfway},
                          ${tX}   ${halfway},
                          ${tX}   ${tY - nodeHeight / 2}`;
            }
        });
}

// Nodelarni chizish funksiyasi
function drawNodes(nodesArray) {
    const node = g.selectAll('g.node')
        .data(nodesArray, d => d.id);

    const nodeEnter = node.enter().append('g')
        .attr('class', 'node')
        .attr('transform', d => `translate(${d.x},${d.y})`)
        .on('click', (event, d) => {
            console.log("Bosildi: ", d);
            // Yangi shaxsni markazga olish uchun API chaqiramiz:
            loadTree(d.id);
        });

    nodeEnter.style('cursor', 'pointer').append('rect')
        .attr('width', nodeWidth)
        .attr('height', nodeHeight)
        .attr('x', -nodeWidth / 2)
        .attr('y', -nodeHeight / 2)
        .attr('rx', 12)
        .style("fill", d => d.gender === 'FEMALE' ? '#2e1065' : '#1e293b'); // Ayollarga sal boshqacharoq orqa fon

    // Rasm
    nodeEnter.append('clipPath')
        .attr('id', d => 'clip-circle-' + d.id)
        .append('circle')
        .attr('r', 24)
        .attr('cx', -nodeWidth / 2 + 35)
        .attr('cy', 0);

    nodeEnter.append('circle')
        .attr('r', 26)
        .attr('cx', -nodeWidth / 2 + 35)
        .attr('cy', 0)
        .attr('fill', '#1e293b')
        .attr('stroke', d => d.gender === 'FEMALE' ? '#c084fc' : '#3b82f6')
        .attr('stroke-width', '2px');

    nodeEnter.append('image')
        .attr('href', d => {
            if (d.photoUrl) return d.photoUrl;
            // Oflayn ishlaydigan rasm (Bosh harflar bilan):
            const noImgColor = d.gender === 'FEMALE' ? 'c084fc' : '3b82f6';
            const nameCuts = d.name ? d.name.substring(0, 2).toUpperCase() : "US";
            // Kichik oflayn SVG generatsiya qilish 
            const svgStr = `<svg xmlns="http://www.w3.org/2000/svg" width="48" height="48">
              <rect width="48" height="48" fill="%23${noImgColor}"/>
              <text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" fill="white" font-size="20px" font-family="sans-serif">${nameCuts}</text>
            </svg>`;
            return 'data:image/svg+xml;utf8,' + svgStr;
        })
        .attr('width', 48)
        .attr('height', 48)
        .attr('x', -nodeWidth / 2 + 11)
        .attr('y', -24)
        .attr('clip-path', d => 'url(#clip-circle-' + d.id + ')');

    // Ism
    nodeEnter.append('text')
        .attr('class', 'name')
        .attr('x', -nodeWidth / 2 + 75)
        .attr('y', -4)
        .text(d => d.name && d.name.length > 15 ? d.name.substring(0, 15) + "..." : (d.name || "Noma'lum"));

    // Lavozimi yoki tug'ilgan sanasi
    nodeEnter.append('text')
        .attr('class', 'title')
        .attr('x', -nodeWidth / 2 + 75)
        .attr('y', 16)
        .text(d => {
            let role = d.role || '';
            let bDate = d.birthDate || '';
            let text = `${role} ${bDate ? '(' + bDate + ')' : ''}`.trim();
            return text || "Ma'lumot yo'q";
        });
}

function loadTree(clickedPersonId) {
    const tokenInput = document.getElementById('tokenInput');
    const treeIdInput = document.getElementById('treeIdInput');
    const personIdInput = document.getElementById('personIdInput'); // new input
    
    // Agar inputda token va treeId bo'lsa olish (agar yo'q bo'lsa unda o'zimiznikini olamiz)
    const token = tokenInput ? tokenInput.value.trim() : "";
    const customTreeId = treeIdInput ? treeIdInput.value.trim() : "";
    const manualPersonId = personIdInput ? personIdInput.value.trim() : "";
    
    // Tugundan bosilgan ID (clickedPersonId) ustuvor, aks holda tepadagi input (manualPersonId) ustuvor.
    const activePersonId = clickedPersonId || manualPersonId;
    
    let url = '';
    
    if (activePersonId) {
        url = `/api/layout/person/${activePersonId}`;
    } else if (customTreeId) {
        url = `/api/layout/tree/${customTreeId}`;
    } else {
        url = '/api/layout/tree/1'; // default
    }
    
    // Headers setup with Authorization token
    const headers = { 'Content-Type': 'application/json' };
    if (token) {
        headers['Authorization'] = 'Bearer ' + token;
    }
    
    fetch(url, { method: 'GET', headers: headers })
        .then(response => {
            if (!response.ok) {
                return response.text().then(text => {
                    throw new Error(text || response.statusText);
                });
            }
            return response.json();
        })
        .then(data => {
            console.log("Backend ma'lumoti keldi: ", data);
            
            // Tozalash
            g.selectAll('*').remove();
            
            const nodesArray = data.nodes || [];
            const connections = data.connections || [];
            
            // Map the nodes for easy lookup
            const nodesMap = new Map();
            nodesArray.forEach(n => nodesMap.set(n.id, n));

            drawLinks(connections, nodesMap);
            drawNodes(nodesArray);
            
            // Layoutni o'rtaga keltirish
            if (data.minX !== undefined && data.maxX !== undefined) {
               const centerX = (data.minX + data.maxX) / 2;
               const centerY = (data.minY + data.maxY) / 2;
               svg.transition().duration(750).call(
                   zoom.transform, 
                   d3.zoomIdentity.translate(width / 2 - centerX, height / 2 - centerY).scale(1)
               );
            } else {
            centerRoot();
            }
        })
        .catch(error => {
            console.error(error);
            const fallbackMsg = `⚠️ DIQQAT! XATOLIK.\n\nBackenddan xato keldi: ${error.message}\nManzil: ${url}\n\nSabablari:\n1. Kiritilgan ID (Tree yoki Person) mavjud emas yoki o'chirilgan.\n2. Boshqa ID berib ko'ring.\n3. Token noto'g'ri kiritilgan bo'lishi mumkin.`;
            alert(fallbackMsg);
            
            // Xato bo'lsa test ko'rinishida yozilgan daraxtni o'zini chizadi (Ko'z bilan ko'rishingiz uchun):
            if (typeof renderTree === 'function' && typeof fallbackData !== 'undefined') {
                renderTree(fallbackData); 
            }
        });
}

// Boshlash
loadTree(null); // avtomat '1' id li daraxtni oladi

// Controls
document.getElementById('btn-load-tree').addEventListener('click', () => loadTree(null));
document.getElementById('btn-center').addEventListener('click', centerRoot);

document.getElementById('btn-zoom-in').addEventListener('click', () => {
    svg.transition().duration(300).call(zoom.scaleBy, 1.3);
});

document.getElementById('btn-zoom-out').addEventListener('click', () => {
    svg.transition().duration(300).call(zoom.scaleBy, 1 / 1.3);
});

window.addEventListener('resize', () => {
    width = container.clientWidth;
    height = container.clientHeight;
});
