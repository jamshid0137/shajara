// // const WIDTH = window.innerWidth;
// // const HEIGHT = window.innerHeight;
// // const NODE_W = 100;
// // const NODE_H = 40;
// //
// // // === SVG va container (zoom/pan uchun) ===
// // const svg = d3.select("#tree")
// //     .append("svg")
// //     .attr("width", WIDTH)
// //     .attr("height", HEIGHT)
// //     .style("border", "1px solid #ccc");
// //
// // const container = svg.append("g");
// //
// // // === Zoom/Pan ===
// // svg.call(
// //     d3.zoom()
// //         .scaleExtent([0.3, 3])
// //         .on("zoom", (event) => container.attr("transform", event.transform))
// // );
// //
// // // === Fetch data ===
// // fetch("http://localhost:8080/api/family-trees/1/graph")
// //     .then(res => res.json())
// //     .then(drawGraph);
// //
// // // === Draw Graph ===
// // function drawGraph(data) {
// //     const nodes = [];
// //     const parentLinks = [];
// //     const spouseLinks = [];
// //
// //     // 1️⃣ Nodes
// //     data.levels.forEach((level, depth) => {
// //         level.forEach((p, index) => {
// //             nodes.push({
// //                 id: p.id,
// //                 name: p.name,
// //                 gender: p.gender,
// //                 x: 200 + index * 200,
// //                 y: 120 + depth * 160,
// //                 level: depth,
// //                 birthDate: p.birthDate,
// //                 profession: p.profession,
// //                 homeland: p.homeland,
// //                 diedDate: p.diedDate,
// //                 phoneNumber: p.phoneNumber
// //             });
// //         });
// //     });
// //
// //     const nodeMap = new Map(nodes.map(n => [n.id, n]));
// //
// //     // 2️⃣ Links
// //     data.relations.forEach(r => {
// //         if (r.type === "PARENT") {
// //             parentLinks.push({ source: nodeMap.get(r.from), target: nodeMap.get(r.to) });
// //         }
// //         if (r.type === "SPOUSE") {
// //             spouseLinks.push({ a: nodeMap.get(r.from), b: nodeMap.get(r.to) });
// //         }
// //     });
// //
// //     // 3️⃣ Parent Links
// //     container.selectAll(".parent-link")
// //         .data(parentLinks)
// //         .enter()
// //         .append("line")
// //         .attr("class", "parent-link")
// //         .attr("x1", d => d.source.x)
// //         .attr("y1", d => d.source.y + NODE_H/2)
// //         .attr("x2", d => d.target.x)
// //         .attr("y2", d => d.target.y - NODE_H/2)
// //         .attr("stroke", "#333")
// //         .attr("stroke-width", 2);
// //
// //     // 4️⃣ Spouse Links + 💍
// //     spouseLinks.forEach(link => {
// //         const midX = (link.a.x + link.b.x) / 2;
// //         const midY = (link.a.y + link.b.y) / 2;
// //
// //         container.append("line")
// //             .attr("x1", link.a.x)
// //             .attr("y1", link.a.y)
// //             .attr("x2", link.b.x)
// //             .attr("y2", link.b.y)
// //             .attr("stroke", "#ff9800")
// //             .attr("stroke-width", 3)
// //             .attr("stroke-dasharray", "6,4");
// //
// //         container.append("text")
// //             .attr("x", midX)
// //             .attr("y", midY - 5)
// //             .attr("text-anchor", "middle")
// //             .attr("font-size", "20px")
// //             .text("💍");
// //     });
// //
// //     // 5️⃣ Nodes
// //     const node = container.selectAll(".node")
// //         .data(nodes)
// //         .enter()
// //         .append("g")
// //         .attr("class", "node")
// //         .attr("transform", d => `translate(${d.x - NODE_W/2},${d.y - NODE_H/2})`)
// //         .on("dblclick", (_, d) => showUpdateModal(d));
// //
// //     node.append("rect")
// //         .attr("width", NODE_W)
// //         .attr("height", NODE_H)
// //         .attr("rx", 8)
// //         .attr("ry", 8)
// //         .attr("fill", d => d.gender === "MALE" ? "#1e90ff" : "#ff69b4");
// //
// //     node.append("text")
// //         .attr("x", NODE_W/2)
// //         .attr("y", NODE_H/2 + 5)
// //         .attr("text-anchor", "middle")
// //         .attr("fill", "#fff")
// //         .attr("font-size", "14px")
// //         .attr("font-weight", "bold")
// //         .text(d => d.name);
// //
// //     // 6️⃣ + Buttons
// //     drawButtons(node);
// // }
// //
// // // === + Buttons ===
// // function drawButtons(nodeGroup) {
// //     const buttons = [
// //         // TOP diagonal
// //         { dx: NODE_W/4, dy: -15, type: "PARENT", direction: "TOP_LEFT" },
// //         { dx: 3*NODE_W/4, dy: -15, type: "PARENT", direction: "TOP_RIGHT" },
// //         // BOTTOM diagonal
// //         { dx: NODE_W/4, dy: NODE_H+15, type: "CHILD", direction: "BOTTOM_LEFT" },
// //         { dx: 3*NODE_W/4, dy: NODE_H+15, type: "CHILD", direction: "BOTTOM_RIGHT" },
// //         // LEFT / RIGHT spouse
// //         { dx: -15, dy: NODE_H/2, type: "SPOUSE", direction: "LEFT" },
// //         { dx: NODE_W+15, dy: NODE_H/2, type: "SPOUSE", direction: "RIGHT" }
// //     ];
// //
// //     buttons.forEach(b => {
// //         nodeGroup.append("circle")
// //             .attr("cx", b.dx)
// //             .attr("cy", b.dy)
// //             .attr("r", 10)
// //             .attr("fill", "#00c853")
// //             .style("cursor", "pointer")
// //             .on("click", (_, d) => quickCreate(d.id, b.type, b.direction));
// //
// //         nodeGroup.append("text")
// //             .attr("x", b.dx - 4)
// //             .attr("y", b.dy + 4)
// //             .text("+")
// //             .attr("fill", "white")
// //             .attr("font-size", "14px")
// //             .attr("font-weight", "bold")
// //             .style("pointer-events", "none");
// //     });
// // }
// //
// // // === Quick Create ===
// // function quickCreate(targetId, type, direction) {
// //     const dto = { treeId: 1, targetId, type, direction };
// //     fetch("http://localhost:8080/api/persons/quick-create", {
// //         method: "POST",
// //         headers: { "Content-Type": "application/json" },
// //         body: JSON.stringify(dto)
// //     }).then(res => {
// //         if(res.ok) location.reload();
// //         else alert("Quick create failed!");
// //     });
// // }
// //
// // // === Update Modal ===
// // function showUpdateModal(person) {
// //     let modal = document.getElementById("updateModal");
// //     if (!modal) {
// //         modal = document.createElement("div");
// //         modal.id = "updateModal";
// //         modal.style.position = "fixed";
// //         modal.style.top = "0";
// //         modal.style.left = "0";
// //         modal.style.width = "100%";
// //         modal.style.height = "100%";
// //         modal.style.background = "rgba(0,0,0,0.5)";
// //         modal.style.display = "flex";
// //         modal.style.justifyContent = "center";
// //         modal.style.alignItems = "center";
// //         modal.style.zIndex = "1000";
// //
// //         modal.innerHTML = `
// //             <div style="background:white;padding:20px;border-radius:8px;min-width:300px;position:relative;">
// //                 <h3>Update Person</h3>
// //                 <form id="updateForm">
// //                     <label>Name: <input type="text" name="name"/></label><br/><br/>
// //                     <label>Gender:
// //                         <select name="gender">
// //                             <option value="MALE">Male</option>
// //                             <option value="FEMALE">Female</option>
// //                         </select>
// //                     </label><br/><br/>
// //                     <label>Birth Date: <input type="date" name="birthDate"/></label><br/><br/>
// //                     <label>Profession: <input type="text" name="profession"/></label><br/><br/>
// //                     <label>Homeland: <input type="text" name="homeland"/></label><br/><br/>
// //                     <label>Died Date: <input type="date" name="diedDate"/></label><br/><br/>
// //                     <label>Phone: <input type="text" name="phoneNumber"/></label><br/><br/>
// //                     <button type="submit">Save</button>
// //                     <button type="button" id="closeModal">Cancel</button>
// //                 </form>
// //             </div>
// //         `;
// //         document.body.appendChild(modal);
// //         document.getElementById("closeModal").onclick = () => { modal.style.display = "none"; };
// //     }
// //
// //     const form = document.getElementById("updateForm");
// //     form.name.value = person.name || "";
// //     form.gender.value = person.gender || "MALE";
// //     form.birthDate.value = person.birthDate || "";
// //     form.profession.value = person.profession || "";
// //     form.homeland.value = person.homeland || "";
// //     form.diedDate.value = person.diedDate || "";
// //     form.phoneNumber.value = person.phoneNumber || "";
// //
// //     modal.style.display = "flex";
// //
// //     form.onsubmit = (e) => {
// //         e.preventDefault();
// //         const dto = {
// //             name: form.name.value,
// //             gender: form.gender.value,
// //             birthDate: form.birthDate.value || null,
// //             profession: form.profession.value || null,
// //             homeland: form.homeland.value || null,
// //             diedDate: form.diedDate.value || null,
// //             phoneNumber: form.phoneNumber.value || null
// //         };
// //         fetch(`http://localhost:8080/api/persons/${person.id}`, {
// //             method: "PUT",
// //             headers: { "Content-Type": "application/json" },
// //             body: JSON.stringify(dto)
// //         }).then(res => {
// //             if (res.ok) {
// //                 alert("Updated successfully!");
// //                 location.reload();
// //             } else {
// //                 alert("Update failed!");
// //             }
// //         });
// //     };
// // }
// //
// //
// //
// //
// //
// //
// //
// //
// //
// //
// //
// //
// //
// //
// //
// //
// // //
//
// const WIDTH = window.innerWidth;
// const HEIGHT = window.innerHeight;
// const NODE_W = 100;
// const NODE_H = 40;
//
// // === SVG container (zoom/pan) ===
// const svg = d3.select("#tree")
//     .append("svg")
//     .attr("width", WIDTH)
//     .attr("height", HEIGHT)
//     .style("border", "1px solid #ccc");
//
// const container = svg.append("g");
//
// // === Zoom/Pan ===
// svg.call(
//     d3.zoom()
//         .scaleExtent([0.3, 3])
//         .on("zoom", (event) => container.attr("transform", event.transform))
// );
//
// // === Fetch data ===
// fetch("http://localhost:8080/api/family-trees/1/graph")
//     .then(res => res.json())
//     .then(drawGraph);
//
// // === Draw Graph ===
// function drawGraph(data) {
//     const nodes = [];
//     const parentLinks = [];
//     const spouseLinks = [];
//
//     // 1️⃣ Nodes
//     data.levels.forEach((level, depth) => {
//         level.forEach((p, index) => {
//             nodes.push({
//                 id: p.id,
//                 name: p.name,
//                 gender: p.gender,
//                 x: 200 + index * 200,
//                 y: 120 + depth * 160,
//                 level: depth,
//                 birthDate: p.birthDate,
//                 profession: p.profession,
//                 homeland: p.homeland,
//                 diedDate: p.diedDate,
//                 phoneNumber: p.phoneNumber
//             });
//         });
//     });
//
//     const nodeMap = new Map(nodes.map(n => [n.id, n]));
//
//     // 2️⃣ Links
//     data.relations.forEach(r => {
//         if (r.type === "PARENT") parentLinks.push({ source: nodeMap.get(r.from), target: nodeMap.get(r.to) });
//         if (r.type === "SPOUSE") spouseLinks.push({ a: nodeMap.get(r.from), b: nodeMap.get(r.to) });
//     });
//
//     // 3️⃣ Parent Links
//     container.selectAll(".parent-link")
//         .data(parentLinks)
//         .enter()
//         .append("line")
//         .attr("class", "parent-link")
//         .attr("x1", d => d.source.x)                //chiziq boshlangich nuqta
//         .attr("y1", d => d.source.y + NODE_H / 2)
//         .attr("x2", d => d.target.x)
//         .attr("y2", d => d.target.y - NODE_H / 2)
//         .attr("stroke", "#333")
//         .attr("stroke-width", 2);
//
//     // 4️⃣ Spouse Links + 💍
//     spouseLinks.forEach(link => {
//         link.b.y = link.a.y; // spouselar bir qatorda bo‘lsin
//         const midX = (link.a.x + link.b.x) / 2;
//         const midY = link.a.y;
//
//         container.append("line")
//             .attr("x1", link.a.x)
//             .attr("y1", link.a.y)
//             .attr("x2", link.b.x)
//             .attr("y2", link.b.y)
//             .attr("stroke", "#ff9800")
//             .attr("stroke-width", 3)
//             .attr("stroke-dasharray", "6,4");
//
//         container.append("text")
//             .attr("x", midX)
//             .attr("y", midY - 5)
//             .attr("text-anchor", "middle")
//             .attr("font-size", "20px")
//             .text("💍");
//     });
//
//     // 5️⃣ Nodes
//     const node = container.selectAll(".node")
//         .data(nodes)
//         .enter()
//         .append("g")
//         .attr("class", "node")
//         .attr("transform", d => `translate(${d.x - NODE_W / 2},${d.y - NODE_H / 2})`)
//         .on("dblclick", (_, d) => showUpdateModal(d));
//
//     node.append("rect")
//         .attr("width", NODE_W)
//         .attr("height", NODE_H)
//         .attr("rx", 8)
//         .attr("ry", 8)
//         .attr("fill", d => d.gender === "MALE" ? "#1e90ff" : "#ff69b4");
//
//     node.append("text")
//         .attr("x", NODE_W / 2)
//         .attr("y", NODE_H / 2 + 5)
//         .attr("text-anchor", "middle")
//         .attr("fill", "#fff")
//         .attr("font-size", "14px")
//         .attr("font-weight", "bold")
//         .text(d => d.name);
//
//     // 6️⃣ + Buttons
//     drawButtons(node, parentLinks, spouseLinks);
// }
//
// // === + Buttons (dynamic placement) ===
// function drawButtons(nodeGroup, parentLinks, spouseLinks) {
//     nodeGroup.each(function(d) {
//         const group = d3.select(this);
//
//         // === Parent tugmalari ===
//         const hasParent = parentLinks.some(l => l.target.id === d.id);
//         const parentDy = hasParent ? -15 : NODE_H / 2; // parent yo'q bo‘lsa node ustida chiqmasin
//         const parentButtons = [
//             { dx: NODE_W / 4, dy: parentDy, direction: "TOP_LEFT" },
//             { dx: 3 * NODE_W / 4, dy: parentDy, direction: "TOP_RIGHT" }
//         ];
//
//         // === Child tugmalari ===
//         const childDy = NODE_H + 15;
//         const childButtons = [
//             { dx: NODE_W / 4, dy: childDy, direction: "BOTTOM_LEFT" },
//             { dx: 3 * NODE_W / 4, dy: childDy, direction: "BOTTOM_RIGHT" }
//         ];
//
//
//         //
//         // // === Spouse tugmalari ===
//         // const spouses = spouseLinks.filter(l => l.a.id === d.id || l.b.id === d.id);
//         // const parentiPersonni = hasParent ? -15 : NODE_H / 2;
//         // const spouseButtons = spouses.map((_, i) => ({
//         //     dx: NODE_W + 15 + i * 25, // yonma-yon
//         //     dy: NODE_H / 2,
//         //     direction: "RIGHT"
//         // }));
//
// // === Spouse tugmalari ===
//         const spouses = spouseLinks.filter(l => l.a.id === d.id || l.b.id === d.id);
//         const parentLink = parentLinks.find(l => l.target.id === d.id);
//         const spouseDy = parentLink ? parentLink.source.y + NODE_H + 15 : NODE_H / 2;
// // Agar parent bo‘lsa spouse tugmasi node’ning pastida, aks holda node markazi
//         //const spouseDy = hasParent ? NODE_H + 15 : NODE_H / 2;
//
//         const spouseButtons = spouses.map((_, i) => ({
//             dx: NODE_W + 15 + i * 25, // yonma-yon
//             dy: spouseDy,
//             direction: "RIGHT"
//         }));
//
//
//         const draw = (btns, type) => {
//             btns.forEach(b => {
//                 group.append("circle")
//                     .attr("cx", b.dx)
//                     .attr("cy", b.dy)
//                     .attr("r", 10)
//                     .attr("fill", "#00c853")
//                     .style("cursor", "pointer")
//                     .on("click", () => quickCreate(d.id, type, b.direction));
//
//                 group.append("text")
//                     .attr("x", b.dx - 4)
//                     .attr("y", b.dy + 4)
//                     .text("+")
//                     .attr("fill", "white")
//                     .attr("font-size", "14px")
//                     .attr("font-weight", "bold")
//                     .style("pointer-events", "none");
//             });
//         };
//
//         draw(parentButtons, "PARENT");
//         draw(childButtons, "CHILD");
//         draw(spouseButtons, "SPOUSE");
//
//     });
// }
//
// // === Quick Create ===
// function quickCreate(targetId, type, direction) {
//     const dto = { treeId: 1, targetId, type, direction };
//     fetch("http://localhost:8080/api/persons/quick-create", {
//         method: "POST",
//         headers: { "Content-Type": "application/json" },
//         body: JSON.stringify(dto)
//     }).then(res => {
//         if(res.ok) location.reload();
//         else alert("Quick create failed!");
//     });
// }
//
// // === Update Modal ===
// function showUpdateModal(person) {
//     let modal = document.getElementById("updateModal");
//     if (!modal) {
//         modal = document.createElement("div");
//         modal.id = "updateModal";
//         modal.style.position = "fixed";
//         modal.style.top = "0";
//         modal.style.left = "0";
//         modal.style.width = "100%";
//         modal.style.height = "100%";
//         modal.style.background = "rgba(0,0,0,0.5)";
//         modal.style.display = "flex";
//         modal.style.justifyContent = "center";
//         modal.style.alignItems = "center";
//         modal.style.zIndex = "1000";
//
//         modal.innerHTML = `
//             <div style="background:white;padding:20px;border-radius:8px;min-width:300px;position:relative;">
//                 <h3>Update Person</h3>
//                 <form id="updateForm">
//                     <label>Name: <input type="text" name="name"/></label><br/><br/>
//                     <label>Gender:
//                         <select name="gender">
//                             <option value="MALE">Male</option>
//                             <option value="FEMALE">Female</option>
//                         </select>
//                     </label><br/><br/>
//                     <label>Birth Date: <input type="date" name="birthDate"/></label><br/><br/>
//                     <label>Profession: <input type="text" name="profession"/></label><br/><br/>
//                     <label>Homeland: <input type="text" name="homeland"/></label><br/><br/>
//                     <label>Died Date: <input type="date" name="diedDate"/></label><br/><br/>
//                     <label>Phone: <input type="text" name="phoneNumber"/></label><br/><br/>
//                     <button type="submit">Save</button>
//                     <button type="button" id="closeModal">Cancel</button>
//                 </form>
//             </div>
//         `;
//         document.body.appendChild(modal);
//         document.getElementById("closeModal").onclick = () => { modal.style.display = "none"; };
//     }
//
//     const form = document.getElementById("updateForm");
//     form.name.value = person.name || "";
//     form.gender.value = person.gender || "MALE";
//     form.birthDate.value = person.birthDate || "";
//     form.profession.value = person.profession || "";
//     form.homeland.value = person.homeland || "";
//     form.diedDate.value = person.diedDate || "";
//     form.phoneNumber.value = person.phoneNumber || "";
//
//     modal.style.display = "flex";
//
//     form.onsubmit = (e) => {
//         e.preventDefault();
//         const dto = {
//             name: form.name.value,
//             gender: form.gender.value,
//             birthDate: form.birthDate.value || null,
//             profession: form.profession.value || null,
//             homeland: form.homeland.value || null,
//             diedDate: form.diedDate.value || null,
//             phoneNumber: form.phoneNumber.value || null
//         };
//         fetch(`http://localhost:8080/api/persons/${person.id}`, {
//             method: "PUT",
//             headers: { "Content-Type": "application/json" },
//             body: JSON.stringify(dto)
//         }).then(res => {
//             if (res.ok) {
//                 alert("Updated successfully!");
//                 location.reload();
//             } else {
//                 alert("Update failed!");
//             }
//         });
//     };
// }
//
