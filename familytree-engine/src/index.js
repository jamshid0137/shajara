/**
 * familytree-engine/src/index.js
 * Asosiy eksport qilinadigan modul
 * Original FamilyTreeJS kutubxonasiga asoslanib api ga moslangan
 */

import { DEFAULT_CONFIG, ACTION, LAYOUT, ORIENTATION } from './core/constants.js';
import { createLayout } from './algorithms/layout.js';
import { SvgRenderer } from './core/svg.js';
import { EventEmitter } from './core/events.js';

export class FamilyTree {
  static action = ACTION;
  static layout = LAYOUT;
  static orientation = ORIENTATION;
  static icon = {
      edit: (w, h, c) => `<svg width="${w}" height="${h}" fill="${c}"><path d=""/></svg>`,
      details: (w, h, c) => `<svg width="${w}" height="${h}" fill="${c}"><path d=""/></svg>`,
      remove: (w, h, c) => `<svg width="${w}" height="${h}" fill="${c}"><path d=""/></svg>`,
  };
  static templates = {
      tommy: { node: '', plus: '', minus: '' }
  };

  constructor(element, config = {}) {
    this.element = typeof element === 'string' ? document.querySelector(element) : element;
    
    // Konfiguratsiyani birlashtirish
    this.config = { ...DEFAULT_CONFIG, ...config };
    if (config.anim) this.config.anim = { ...DEFAULT_CONFIG.anim, ...config.anim };
    
    // Original nodes arrayini referens kabi saqlash
    this.config.nodes = this.config.nodes || [];

    this.nodeWidth = 240;
    this.nodeHeight = 100;
    this.templateRenderer = this._defaultTemplate;

    // Componentlar
    this.renderer = new SvgRenderer(this.element, this.config);
    this.layoutEngine = createLayout(this.config, this.nodeWidth, this.nodeHeight);
    
    // Voqealar tizimi
    this._emitter = new EventEmitter();

    this._idIndex = 0; // generateId() uchun maxsus
    this._selectedNodeId = null;
    
    this._initMenu();

    // Dastlab chizib olish
    if (this.config.nodes.length > 0) {
      this.draw();
    }
  }

  // ============== VOQEALAR BOSHQRARUVI (EVENTS) ==============

  on(eventName, listener) {
    this._emitter.on(eventName, listener);
    return this;
  }

  removeListener(eventName, listener) {
    this._emitter.off(eventName, listener);
    return this;
  }

  // ============== ASOSIY RENDERING API ==============

  load(nodes, callback) {
    this.config.nodes = nodes || [];
    this.draw(ACTION.init);
    if (typeof callback === 'function') callback();
  }

  draw(action = ACTION.update, params = {}, callback) {
    // 1. Placeholderlarni tayyorlash (agar click bo'lgan bo'lsa)
    const processNodes = JSON.parse(JSON.stringify(this.config.nodes || []));

    if (this.config.nodeTreeMenu && this._selectedNodeId) {
        const node = processNodes.find(n => n.id == this._selectedNodeId);
        if (node) {
            // Partner Placeholder
            processNodes.push({ id: 'temp_add_spouse', isPlaceholder: true, placeholderAction: 'spouse', targetId: node.id, pids: [node.id], tags: ['placeholder'] });
            
            // Son / Daughter Placeholders
            processNodes.push({ id: 'temp_add_son', isPlaceholder: true, placeholderAction: 'son', targetId: node.id, fid: node.id, tags: ['placeholder'] });
            processNodes.push({ id: 'temp_add_daughter', isPlaceholder: true, placeholderAction: 'daughter', targetId: node.id, fid: node.id, tags: ['placeholder'] });
            
            // Parent Placeholder
            if (!node.fid && !node.mid) {
                processNodes.push({ id: 'temp_add_parent', isPlaceholder: true, placeholderAction: 'parent', targetId: node.id, tags: ['placeholder'] });
                // Layoutni balandda ko'rsatishi uchun vaqtinchalik otasi sifatida ulaymiz
                node.fid = 'temp_add_parent'; 
            }
        }
    }

    // 2. Layout hisoblash
    const nodeMap = this.layoutEngine.compute(processNodes);

    // 3. DOM-ga elementlarni o'tqazish
    this.renderer.clear();
    this.renderer.drawLinks(nodeMap);
    this.renderer.drawNodes(nodeMap, (data, w, h) => {
        if (data.isPlaceholder) {
            return this._placeholderTemplate(data, w, h);
        }
        return this.templateRenderer(data, w, h);
    });

    // 4. View box (faqat initda to'liq ko'rsatamiz)
    if (action === ACTION.init) {
      this.renderer.fitView(nodeMap);
    }
    
    if (typeof callback === 'function') {
      callback();
    }
  }

  setTemplateRenderer(fn) {
    this.templateRenderer = fn;
  }

  _initMenu() {
    if (!this.config.nodeMenu) return;

    this.menuElement = document.createElement('div');
    this.menuElement.style.cssText = `
       display: none; position: fixed; background: #262626; border-radius: 6px;
       box-shadow: 0 4px 15px rgba(0,0,0,0.5); z-index: 9999; flex-direction: column; overflow: hidden;
       min-width: 160px; font-family: 'Inter', Arial, sans-serif;
    `;
    document.body.appendChild(this.menuElement);

    document.addEventListener('click', () => {
       this.menuElement.style.display = 'none';
    });

    this.renderer.container.addEventListener('click', (e) => {
       const menuBtn = e.target.closest('.bft-menu-btn');
       if (menuBtn) {
          e.stopPropagation();
          this._showMenu(e, menuBtn.getAttribute('data-id'));
          return;
       }
       
       const plusBtn = e.target.closest('.bft-plus-btn');
       if (plusBtn) {
          e.stopPropagation();
          const id = plusBtn.getAttribute('data-id');
          this._selectedNodeId = (this._selectedNodeId === id) ? null : id;
          this.draw(ACTION.update);
          return;
       }

       const placeholderBtn = e.target.closest('.bft-placeholder');
       if (placeholderBtn) {
          e.stopPropagation();
          const action = placeholderBtn.getAttribute('data-action');
          const targetId = placeholderBtn.getAttribute('data-target');
          this._handlePlaceholderClick(action, targetId);
          return;
       }

       // Agar chetga bosilsa, selectedNodeId tozalansin
       if (!e.target.closest('.node')) {
          if (this._selectedNodeId) {
             this._selectedNodeId = null;
             this.draw(ACTION.update);
          }
       }
    });
  }

  _handlePlaceholderClick(action, targetId) {
      const tempId = this.generateId();
      let args = null;
      
      const parentNode = this.get(targetId);
      if (!parentNode) return;

      if (action === 'spouse') {
          const oldPids = parentNode.pids || [];
          args = {
              addNodesData: [{ id: tempId, pids: [targetId] }],
              updateNodesData: [{ id: targetId, pids: [...oldPids, tempId] }],
              removeNodeId: null
          };
      } else if (action === 'son' || action === 'daughter') {
          let mid = parentNode.tags && parentNode.tags.includes('female') ? targetId : (parentNode.pids && parentNode.pids[0]);
          let fid = parentNode.tags && parentNode.tags.includes('female') ? (parentNode.pids && parentNode.pids[0]) : targetId;
          
          args = {
              addNodesData: [{ id: tempId, fid: fid || targetId, mid: mid || null }],
              updateNodesData: [],
              removeNodeId: null
          };
      } else if (action === 'parent') {
          args = {
              addNodesData: [{ id: tempId }],
              updateNodesData: [{ id: targetId, fid: tempId }],
              removeNodeId: null
          };
      }

      this._selectedNodeId = null;
      if (args && this._emitter.emit('update', this, args) !== false) {
          // faqat backend ruhsat bersa chizamiz, aslida tree.html o'zi page ni reload qilib tashlaydi update da
      }
      this.draw(ACTION.update);
  }

  _showMenu(e, nodeId) {
     this.menuElement.innerHTML = '';
     for (const key in this.config.nodeMenu) {
         const item = this.config.nodeMenu[key];
         const div = document.createElement('div');
         
         div.style.cssText = 'padding: 10px 16px; color: #ddd; cursor: pointer; font-size: 14px; border-bottom: 1px solid #363636; display: flex; align-items: center; gap: 8px; transition: background 0.2s;';
         div.onmouseover = () => div.style.background = '#363636';
         div.onmouseout = () => div.style.background = 'transparent';
         div.innerHTML = item.text || key;
         div.onclick = (ev) => { 
             ev.stopPropagation(); 
             this.menuElement.style.display = 'none'; 
             if (item.onClick) item.onClick(nodeId); 
         };
         this.menuElement.appendChild(div);
     }
     
     const rect = this.renderer.container.getBoundingClientRect();
     let x = e.clientX;
     let y = e.clientY + 10;
     this.menuElement.style.left = x + 'px';
     this.menuElement.style.top = y + 'px';
     this.menuElement.style.display = 'flex';
  }

  _defaultTemplate(data, w, h) {
    const isFemale = data.tags ? data.tags.includes('female') : false;
    const bgColor = isFemale ? '#F57C00' : '#039BE5'; 

    let photoHtml = '';
    // Draw an overlapping avatar circle specifically if a photo exists or if it's the main person
    if (data.photoUrl || data.role === 'CENTER' || data.roleLabel && data.roleLabel.includes('Asosiy')) {
       const imgUri = data.photoUrl || ''; // generic if not provided
       photoHtml = `
         <circle cx="${w - 20}" cy="20" r="28" fill="#1b1b1b" stroke="#fff" stroke-width="2"/>
         <clipPath id="cp_${data.id}">
            <circle cx="${w - 20}" cy="20" r="26" />
         </clipPath>
         ${imgUri ? `<image href="${imgUri}" x="${w - 46}" y="-6" width="52" height="52" clip-path="url(#cp_${data.id})" preserveAspectRatio="xMidYMid slice" />` : 
         `<svg x="${w - 36}" y="4" width="32" height="32" viewBox="0 0 24 24" fill="#a0a0a0"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>`}
       `;
    }

    return `
      <rect x="0" y="0" width="${w}" height="${h}" rx="8" fill="${bgColor}" stroke="rgba(255,255,255,0.1)" stroke-width="1"></rect>
      
      <!-- Role / Title with Icon -->
      <g transform="translate(14, 12)">
         <svg width="18" height="18" viewBox="0 0 24 24" fill="#fff" opacity="0.9"><path d="M15 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm-9-2V7H4v3H1v2h3v3h2v-3h3v-2H6zm9 4c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
      </g>
      <text x="38" y="26" fill="#ffffff" font-size="13" font-family="'Inter', Arial, sans-serif" opacity="0.9">${data.roleLabel || ''}</text>
      
      <!-- Name -->
      <text x="14" y="58" fill="#ffffff" font-size="17" font-weight="bold" font-family="'Inter', Arial, sans-serif">${data.name || ''}</text>
      
      <!-- Years -->
      <text x="14" y="82" fill="#ffffff" font-size="13" font-family="'Inter', Arial, sans-serif" opacity="0.8">${data.years || ''}</text>
      
      ${photoHtml}

      <!-- Menu Button (Three dots) -->
      <g class="bft-menu-btn" transform="translate(${w - 20}, ${h - 15})" style="cursor:pointer;" data-id="${data.id}">
         <circle cx="-6" cy="0" r="1.5" fill="#fff" opacity="0.7"/>
         <circle cx="0" cy="0" r="1.5" fill="#fff" opacity="0.7"/>
         <circle cx="6" cy="0" r="1.5" fill="#fff" opacity="0.7"/>
         <!-- hit area -->
         <rect x="-15" y="-10" width="30" height="20" fill="transparent" />
      <!-- Plus Button for Tooltip (nodeTreeMenu) -->
      ${this.config.nodeTreeMenu ? `
      <g class="bft-plus-btn" transform="translate(${w/2}, 0)" style="cursor:pointer;" data-id="${data.id}">
         <circle cx="0" cy="0" r="12" fill="#2d2d2d" stroke="#ffffff" stroke-width="2"/>
         <line x1="-5" y1="0" x2="5" y2="0" stroke="#fff" stroke-width="2"/>
         <line x1="0" y1="-5" x2="0" y2="5" stroke="#fff" stroke-width="2"/>
         <rect x="-12" y="-12" width="24" height="24" fill="transparent"/>
      </g>` : ''}
    `;
  }

  _placeholderTemplate(data, w, h) {
      let text = "ADD";
      let iconHtml = "";
      if (data.placeholderAction === 'spouse') { text = "Add partner"; iconHtml = `<path d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5z"/>`; }
      if (data.placeholderAction === 'son') { text = "Add son"; iconHtml = `<path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/>`; }
      if (data.placeholderAction === 'daughter') { text = "Add daughter"; iconHtml = `<path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/>`; }
      if (data.placeholderAction === 'parent') { text = "Add parent"; iconHtml = `<path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/>`; }
      
      const isBlue = data.placeholderAction === 'son' || data.placeholderAction === 'parent';
      const color = isBlue ? '#039BE5' : '#F57C00';

      return `
        <!-- Border Outline -->
        <rect x="0" y="0" width="${w}" height="${h}" rx="8" fill="rgba(255,255,255,0.02)" stroke="${color}" stroke-width="1.5" stroke-dasharray="0" class="bft-placeholder" data-action="${data.placeholderAction}" data-target="${data.targetId}" style="cursor:pointer;"></rect>
        
        <text x="${w - 20}" y="30" fill="${color}" font-size="16" font-family="'Inter', Arial" text-anchor="end" style="pointer-events:none;">${text}</text>
        
        <g transform="translate(40, 30) scale(1.6)">
           <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="${color}" stroke-width="1.2"><path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
        </g>
      `;
  }

  // ============== ZOOM & PAN ==============
  
  zoom(delta) {
    const ev = new WheelEvent('wheel', {
      deltaY: delta ? -100 : 100,
      clientX: document.body.clientWidth/2,
      clientY: document.body.clientHeight/2,
    });
    this.renderer.container.dispatchEvent(ev);
  }

  fit() {
    const nodeMap = this.layoutEngine.compute(this.config.nodes);
    this.renderer.fitView(nodeMap);
  }

  // ============== ICHKI OBJECT MANAGMENT ==============

  get(id) {
    const n = this.config.nodes.find(n => n.id == id);
    return n ? JSON.parse(JSON.stringify(n)) : null;
  }

  getNode(id) {
    // Aslida nodeMap (LayoutNode) ligi beriladi, biz esa datani o'zini qaytarish uchun
    return this.config.nodes.find(n => n.id == id);
  }

  add(data) {
    this.config.nodes.push(data);
    return this;
  }

  update(data) {
    const index = this.config.nodes.findIndex(n => n.id == data.id);
    if (index !== -1) {
      this.config.nodes[index] = { ...this.config.nodes[index], ...data };
    }
    return this;
  }

  remove(id) {
    this.config.nodes = this.config.nodes.filter(n => n.id != id);
    // Bog'lanishlarni ham tozalash
    for (let node of this.config.nodes) {
      if (node.fid == id) delete node.fid;
      if (node.mid == id) delete node.mid;
      if (node.pids && node.pids.includes(id)) {
        node.pids = node.pids.filter(p => p != id);
      }
    }
    return this;
  }

  generateId() {
    return '_' + Math.random().toString(36).substr(2, 9);
  }

  // ============== API METHODS (Add/Update/Remove) ==============
  
  updateNode(data, callback, fireEvent = true) {
    const args = { addNodesData: [], updateNodesData: [data], removeNodeId: null };
    
    // Voqeani uzatish. Emitter "false" qaytarmasligi kerak!
    if (fireEvent && this._emitter.emit('update', this, args) === false) return false;
    
    this.update(data);
    this.draw(ACTION.update, {}, callback);
  }

  removeNode(id, callback, fireEvent = true) {
    const args = { addNodesData: [], updateNodesData: [], removeNodeId: id };
    
    if (fireEvent && this._emitter.emit('update', this, args) === false) return false;

    this.remove(id);
    this.draw(ACTION.update, {}, callback);
  }

  addNode(data, callback, fireEvent = true) {
    const args = { addNodesData: [data], updateNodesData: [], removeNodeId: null };
    
    if (fireEvent && this._emitter.emit('update', this, args) === false) return false;
    
    this.add(data);
    this.draw(ACTION.insert, {}, callback);
  }

  addChildNode(data, callback, fireEvent = true) {
    if (data.id == null) data.id = this.generateId();
    this.addNode(data, callback, fireEvent);
  }

  addChildAndPartnerNodes(partnerId, childData, partnerData, callback, fireEvent = true) {
    if (childData.id == null) childData.id = this.generateId();
    if (partnerData.id == null) partnerData.id = this.generateId();

    const parentNode = this.get(partnerId);
    
    // Juftni parentGa bog'lash
    partnerData.pids = partnerData.pids || [];
    if (!partnerData.pids.includes(partnerId)) partnerData.pids.push(partnerId);

    // parentData ga partnerData idsini kiritish
    parentNode.pids = parentNode.pids || [];
    if (!parentNode.pids.includes(partnerData.id)) parentNode.pids.push(partnerData.id);

    // Bolani ota va onaga belgilash. (kim kim bo'lishi bu erda ahamiyatsiz, bitta fid, bitta mid beramiz)
    childData.fid = partnerId;
    childData.mid = partnerData.id;

    const args = { 
      addNodesData: [childData, partnerData], 
      updateNodesData: [parentNode], 
      removeNodeId: null 
    };

    if (fireEvent && this._emitter.emit('update', this, args) === false) return false;

    this.add(childData).add(partnerData).update(parentNode);
    this.draw(ACTION.update, {}, callback);
  }

  addPartnerNode(data, callback, fireEvent = true) {
    if (data.id == null) data.id = this.generateId();
    // Asosiy partner ID = data.pids[0] bo'ladi odatda
    const mainPartnerId = data.pids && data.pids[0];
    const mainPartner = mainPartnerId ? this.get(mainPartnerId) : null;
    
    const updateNodesData = [];
    if (mainPartner) {
        mainPartner.pids = mainPartner.pids || [];
        if(!mainPartner.pids.includes(data.id)) mainPartner.pids.push(data.id);
        updateNodesData.push(mainPartner);
    }

    const args = { addNodesData: [data], updateNodesData, removeNodeId: null };
    
    if (fireEvent && this._emitter.emit('update', this, args) === false) return false;
    
    this.add(data);
    if(mainPartner) this.update(mainPartner);
    
    this.draw(ACTION.insert, {}, callback);
  }

  addParentNode(childId, type, data, callback, fireEvent = true) {
    if (data.id == null) data.id = this.generateId();
    const child = this.get(childId);
    if (!child) return false;

    // Bolani "type" maydoniga ko'rsatilgan otani yozamiz
    child[type] = data.id; // type: 'fid' yoki 'mid'

    const args = { addNodesData: [data], updateNodesData: [child], removeNodeId: null };
    
    if (fireEvent && this._emitter.emit('update', this, args) === false) return false;
    
    this.add(data);
    this.update(child);
    this.draw(ACTION.insert, {}, callback);
  }
}

// Global scope
if (typeof window !== 'undefined') {
  window.FamilyTree = FamilyTree;
}
