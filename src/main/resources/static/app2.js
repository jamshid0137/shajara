const WIDTH = window.innerWidth;
const HEIGHT = window.innerHeight;

const NODE_W = 140;
const NODE_H = 50;

const svg = d3.select("#tree")
    .append("svg")
    .attr("width", WIDTH)
    .attr("height", HEIGHT);

const container = svg.append("g");

// zoom + pan
svg.call(
    d3.zoom().on("zoom", (event) => {
        container.attr("transform", event.transform);
    })
);

// center
container.attr("transform", `translate(${WIDTH / 2}, ${HEIGHT / 2})`);

// === LOAD PERSON GRAPH ===
function loadPerson(personId) {
    fetch(`http://localhost:8080/api/persons/${personId}/full`)
        .then(res => res.json())
        .then(drawGraph)
        .catch(err => console.error("API error:", err));
}

// === DRAW ===
function drawGraph(data) {
    container.selectAll("*").remove();

    const nodes = [];
    const links = [];

    const center = { ...data, x: 0, y: 0 };
    nodes.push(center);

    // parents (TOP)
    const pStartX = -(data.parents.length - 1) * 160 / 2;
    data.parents.forEach((p, i) => {
        const node = { ...p, x: pStartX + i * 160, y: -140 };
        nodes.push(node);
        links.push({ source: node, target: center });
    });

    // spouses (LEFT + RIGHT)
    const sStartY = -(data.spouses.length - 1) * 80 / 2;
    data.spouses.forEach((s, i) => {
        const node = { ...s, x: i % 2 === 0 ? -200 : 200, y: sStartY + i * 80 };
        nodes.push(node);
        links.push({ source: center, target: node });
    });

    // children (BOTTOM)
    const cStartX = -(data.children.length - 1) * 160 / 2;
    data.children.forEach((c, i) => {
        const node = { ...c, x: cStartX + i * 160, y: 140 };
        nodes.push(node);
        links.push({ source: center, target: node });
    });

    drawLinks(links);
    drawNodes(nodes);
}

// === LINKS ===
function drawLinks(links) {
    container.selectAll(".link")
        .data(links)
        .enter()
        .append("line")
        .attr("class", "link")
        .attr("x1", d => d.source.x)
        .attr("y1", d => d.source.y)
        .attr("x2", d => d.target.x)
        .attr("y2", d => d.target.y)
        .attr("stroke", "#999")
        .attr("stroke-width", 2);
}

// === NODES ===
function drawNodes(nodes) {
    const g = container.selectAll(".node")
        .data(nodes)
        .enter()
        .append("g")
        .attr("class", "node")
        .attr("transform", d => `translate(${d.x},${d.y})`)
        .on("click", (e, d) => loadPerson(d.id));

    g.append("rect")
        .attr("x", -NODE_W / 2)
        .attr("y", -NODE_H / 2)
        .attr("width", NODE_W)
        .attr("height", NODE_H)
        .attr("rx", 10)
        .attr("fill", d => d.gender === "MALE" ? "#4da6ff" : "#ff80bf");

    g.append("text")
        .attr("text-anchor", "middle")
        .attr("dy", "0.35em")
        .attr("fill", "white")
        .style("font-size", "14px")
        .text(d => d.name);
}

// === INIT ===
loadPerson(56); // start person id
