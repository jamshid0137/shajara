/**
 * familytree-engine/src/core/svg.js
 * SVG elementlarini DOM-ga joylashtirish va bog'lanish (Link) chiziqlarini hisoblash
 */

import { ORIENTATION } from './constants.js';

/**
 * SVG chiziqlarni yaratadi va yo'nalishga asoslangan burchak yumalatish qo'llaydi
 * FamilyTreeJS dagi _addPoint* metodlarining o'rnini bosadi
 */
export class SvgRenderer {
  constructor(container, config) {
    this.container = typeof container === 'string' ? document.querySelector(container) : container;
    this.config = config;
    this.svg = null;
    this.defs = null;
    this.linksGroup = null;
    this.nodesGroup = null;

    this._init();
  }

  _init() {
    this.container.innerHTML = '';
    this.container.style.position = 'relative';
    this.container.style.overflow = 'hidden';

    // SVG elementini yaratish
    this.svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
    this.svg.style.width = '100%';
    this.svg.style.height = '100%';
    this.svg.style.cursor = 'grab';

    // SVG turlarini yaratish
    this.defs = document.createElementNS('http://www.w3.org/2000/svg', 'defs');
    this.linksGroup = document.createElementNS('http://www.w3.org/2000/svg', 'g');
    this.nodesGroup = document.createElementNS('http://www.w3.org/2000/svg', 'g');

    this.linksGroup.classList.add('bft-links');
    this.nodesGroup.classList.add('bft-nodes');

    this.svg.appendChild(this.defs);
    this.svg.appendChild(this.linksGroup);
    this.svg.appendChild(this.nodesGroup);
    
    this.container.appendChild(this.svg);

    // Pan & Zoom uchun o'zgaruvchilar
    this.transform = { x: 0, y: 0, scale: this.config.scaleInitial || 1 };
    this.isDragging = false;
    this.dragStart = { x: 0, y: 0 };

    this._setupPanZoom();
  }

  /**
   * ViewBoxni yangilash
   */
  updateViewBox() {
    const { x, y, scale } = this.transform;
    const w = this.container.clientWidth / scale;
    const h = this.container.clientHeight / scale;
    this.svg.setAttribute('viewBox', `${-x/scale} ${-y/scale} ${w} ${h}`);
  }

  _setupPanZoom() {
    // Zoom (Wheel)
    this.container.addEventListener('wheel', (e) => {
      if (!this.config.enablePan && this.config.mouseScrool === 'none') return;
      e.preventDefault();

      const zoomFactor = 1.1;
      const direction = e.deltaY < 0 ? 1 : -1;
      let newScale = this.transform.scale * (direction > 0 ? zoomFactor : 1 / zoomFactor);

      // Scale limit
      newScale = Math.max(this.config.scaleMin, Math.min(this.config.scaleMax, newScale));

      // Markazdan zoom qilish (qisman matematikasi)
      const rect = this.container.getBoundingClientRect();
      const mouseX = e.clientX - rect.left;
      const mouseY = e.clientY - rect.top;

      // Hozirgi koordinatalarga nisbatan SVG dagi haqiqiy nuqta
      const svgX = (mouseX - this.transform.x) / this.transform.scale;
      const svgY = (mouseY - this.transform.y) / this.transform.scale;

      this.transform.x = mouseX - svgX * newScale;
      this.transform.y = mouseY - svgY * newScale;
      this.transform.scale = newScale;

      this.updateViewBox();
    });

    // Pan (Drag)
    this.svg.addEventListener('mousedown', (e) => {
      if (!this.config.enablePan) return;
      this.isDragging = true;
      this.dragStart.x = e.clientX - this.transform.x;
      this.dragStart.y = e.clientY - this.transform.y;
      this.svg.style.cursor = 'grabbing';
    });

    window.addEventListener('mousemove', (e) => {
      if (!this.isDragging) return;
      this.transform.x = e.clientX - this.dragStart.x;
      this.transform.y = e.clientY - this.dragStart.y;
      this.updateViewBox();
    });

    window.addEventListener('mouseup', () => {
      if (this.isDragging) {
        this.isDragging = false;
        this.svg.style.cursor = 'grab';
      }
    });
  }

  /**
   * Barcha render ishlarni tozalash (update dan oldin)
   */
  clear() {
    this.linksGroup.innerHTML = '';
    this.nodesGroup.innerHTML = '';
  }

  /**
   * Burchaklarni yumalatuvchi maxsus algoritm
   * FamilyTreeJS roundPathCorners funksiyasi yordamida
   */
  _roundPathCorners(pathString, radius) {
    const commands = pathString.split(/(?=[MmLlHhVvCcsSzZ])/g);
    // Bu soddalashtirilgan versiya: to'g'ri chiziqli yo'llar (M... L... L...) ni qabul qilib
    // burchaklarni quadratic burchakka aylantiramiz
    let res = "";
    if (commands.length <= 2) return pathString;

    res += commands[0]; // M
    for (let i = 1; i < commands.length - 1; i++) {
        // Asl FamilyTreeJS kodi M L L lar o'rtasida radiusli C/Q command qoshadi.
        // Hozir default pathni qaytaramiz (Buni CSS yordamida rounded qilish ham mumkin rx, ry, stroke-linejoin).
        res += commands[i];
    }
    res += commands[commands.length - 1];
    return pathString; 
  }

  /**
  /**
   * Yaponcha qavslarga o'xshagan silliq (rounded) filialli yo'lni hisoblaydi
   */
  _calculateLinkPath(startX, startY, endX, endY) {
    const midY = startY + (endY - startY) / 2;
    const r = 10;
    
    // To'g'ri pastga
    if (Math.abs(startX - endX) < 1) {
        return `M ${startX} ${startY} L ${endX} ${endY}`;
    }
    
    const dirX = startX < endX ? 1 : -1;
    // O'ta kichik masofalar uchun xavfsiz radius
    const safeR = Math.min(r, Math.abs(midY - startY), Math.abs(endX - startX) / 2);

    return `M ${startX} ${startY} 
            L ${startX} ${midY - safeR} 
            A ${safeR} ${safeR} 0 0 ${dirX === 1 ? 0 : 1} ${startX + safeR * dirX} ${midY} 
            L ${endX - safeR * dirX} ${midY} 
            A ${safeR} ${safeR} 0 0 ${dirX === 1 ? 1 : 0} ${endX} ${midY + safeR} 
            L ${endX} ${endY}`;
  }

  /**
   * DOM ga HTML/SVG elementlarini qo'shish
   */
  drawLinks(nodeMap) {
    let linksHtml = '';

    for (const [id, node] of nodeMap) {
      const data = node.data;

      // 1. Partner links (pids)
      if (data.pids && data.pids.length > 0) {
         for (const pid of data.pids) {
            const partnerNode = nodeMap.get(pid);
            // Faqat o'ng tomondagi partnerlar uchun chizamiz, ikki marta takrorlanmasligi uchun
            if (partnerNode && partnerNode.x > node.x) {
                const startX = node.x + node.w;
                const startY = node.y + node.h / 2;
                const endX = partnerNode.x;
                const endY = partnerNode.y + partnerNode.h / 2;

                const path = `M ${startX} ${startY} L ${endX} ${endY}`;
                linksHtml += `<path d="${path}" stroke="#7B8290" stroke-width="2" fill="none" class="bft-link-partner" />`;
            }
         }
      }
      
      // 2. Farzandlarga uzatma
      if (data.fid || data.mid) {
         let startX, startY;
         const father = data.fid ? nodeMap.get(data.fid) : null;
         const mother = data.mid ? nodeMap.get(data.mid) : null;
         
         if (father && mother) {
             const leftNode = father.x < mother.x ? father : mother;
             const rightNode = father.x < mother.x ? mother : father;
             // Ikkala ota-onaning qo'li bog'langan masofaning o'rtasidan tushyapti
             startX = leftNode.x + leftNode.w + (rightNode.x - (leftNode.x + leftNode.w)) / 2;
             startY = leftNode.y + leftNode.h / 2; 
         } else if (father) {
             startX = father.x + father.w / 2;
             startY = father.y + father.h;
         } else if (mother) {
             startX = mother.x + mother.w / 2;
             startY = mother.y + mother.h;
         }

         if (startX !== undefined) {
             const endX = node.x + node.w / 2;
             const endY = node.y;
             
             const path = this._calculateLinkPath(startX, startY, endX, endY);
             linksHtml += `<path d="${path}" stroke="#7B8290" stroke-width="2" fill="none" class="bft-link" />`;
         }
      }
    }

    this.linksGroup.innerHTML = linksHtml;
  }

  /**
   * Node larni qo'shish (shablonlar orqali)
   */
  drawNodes(nodeMap, templateRenderer) {
    let nodesHtml = '';
    for (const [id, node] of nodeMap) {
      const { x, y, w, h } = node;
      const innerHtml = templateRenderer(node.data, w, h);
      
      // Node o'ramasi
      nodesHtml += `
        <g data-n-id="${id}" class="bft-node" transform="translate(${x}, ${y})">
          ${innerHtml}
        </g>
      `;
    }
    this.nodesGroup.innerHTML = nodesHtml;
  }

  /**
   * Daraxt markazlash va to'g'ri o'lchamlarga keltirish
   */
  fitView(nodeMap) {
      let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
      for (const [, node] of nodeMap) {
          minX = Math.min(minX, node.x);
          minY = Math.min(minY, node.y);
          maxX = Math.max(maxX, node.x + node.w);
          maxY = Math.max(maxY, node.y + node.h);
      }

      if (minX === Infinity) return;

      const cW = this.container.clientWidth;
      const cH = this.container.clientHeight;
      const tW = maxX - minX + this.config.padding * 2;
      const tH = maxY - minY + this.config.padding * 2;

      let scale = Math.min(cW / tW, cH / tH);
      scale = Math.max(this.config.scaleMin, Math.min(this.config.scaleMax, scale));

      const cx = (minX + maxX) / 2;
      const cy = (minY + maxY) / 2;

      this.transform.scale = scale;
      this.transform.x = (cW / 2) - cx * scale;
      this.transform.y = (cH / 2) - cy * scale;

      this.updateViewBox();
  }
}
