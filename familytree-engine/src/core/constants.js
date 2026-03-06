/**
 * familytree-engine/src/core/constants.js
 * FamilyTreeJS dagi barcha konstantalar — o'zimizning implementatsiyamiz
 */

// ─── Layout turlari ────────────────────────────────────────────────────────
export const LAYOUT = {
  normal:          0,
  mixed:           1,
  tree:            2,
  treeLeftOffset:  3,
  treeRightOffset: 4,
  treeLeft:        5,
  treeRight:       6,
  grid:           -1,
};

// ─── Yo'nalish (Orientation) ────────────────────────────────────────────────
export const ORIENTATION = {
  top:          0,
  bottom:       1,
  right:        2,
  left:         3,
  top_left:     4,
  bottom_left:  5,
  right_top:    6,
  left_top:     7,
};

// ─── Harakatlar (Actions) ───────────────────────────────────────────────────
export const ACTION = {
  expand:       0,
  collapse:     1,
  maximize:     101,
  minimize:     102,
  edit:         1,
  zoom:         2,
  ctrlZoom:     22,
  xScroll:      3,
  yScroll:      4,
  none:         5,
  init:         6,
  update:       7,
  pan:          8,
  centerNode:   9,
  resize:       10,
  insert:       11,
  details:      13,
  exporting:    14,
};

// ─── Hizalanish ───────────────────────────────────────────────────────────
export const ALIGN = {
  center:      8,
  orientation: 9,
};

// ─── Default konfiguratsiya ────────────────────────────────────────────────
export const DEFAULT_CONFIG = {
  // Layout
  layout:                          LAYOUT.normal,
  orientation:                     ORIENTATION.top,
  levelSeparation:                 60,
  siblingSeparation:               35,
  subtreeSeparation:               40,
  mixedHierarchyNodesSeparation:   15,
  assistantSeparation:             100,
  minPartnerSeparation:            30,
  partnerChildrenSplitSeparation:  10,
  partnerNodeSeparation:           15,
  columns:                         10,
  padding:                         30,

  // Ko'rinish
  mode:           'light',   // 'light' | 'dark'
  template:       'default',
  scaleInitial:   1,
  scaleMin:       0.1,
  scaleMax:       5,

  // Animatsiya
  anim: {
    duration: 200,
    func:     'outPow',
  },

  // Zoom
  zoom: {
    speed:  120,
    smooth: 12,
  },

  // Interaktivlik
  interactive:    true,
  enableSearch:   true,
  enablePan:      true,
  enableDragDrop: false,
  mouseScrool:    'zoom',  // 'zoom' | 'scroll' | 'none'
  keyNavigation:  false,
  sticky:         true,

  // Data
  nodes:          [],
  clinks:         [],
  slinks:         [],

  // Binding
  nodeBinding:    {},
  linkBinding:    {},

  // Qidiruv
  searchFields:       null,
  searchDisplayField: null,

  // Edit forma
  editForm: {
    readOnly:     false,
    titleBinding: 'name',
    photoBinding: 'img',
    elements:     [],
    buttons: {
      edit:    { text: 'Tahrirlash', hideIfEditMode: true },
      details: { text: 'Batafsil' },
      remove:  { text: "O'chirish", hideIfDetailsMode: true },
    },
  },

  // Collapse/Expand
  collapse: {},
  expand:   {},

  // Misc
  align:           ALIGN.center,
  orderBy:         null,
  roots:           null,
  nodeMenu:        null,
  nodeTreeMenu:    true,
  toolbar:         false,
  miniMap:         false,
  lazyLoading:     true,
};

// ─── Performance chegaralari (FamilyTreeJS dan olingan) ─────────────────────
export const THRESHOLDS = {
  TEXT_THRESHOLD:    400,
  IMAGES_THRESHOLD:  100,
  LINKS_THRESHOLD:   200,
  BUTTONS_THRESHOLD: 70,
  ANIM_THRESHOLD:    50,
  MAX_DEPTH:         200,
  SCALE_FACTOR:      1.44,
  LINK_ROUNDED_CORNERS: 5,
  CLINK_CURVE:       1,
  MOVE_STEP:         5,
};

// ─── Render konstantalari ──────────────────────────────────────────────────
export const RENDER = {
  LINKS_BEFORE_NODES:  true,
  CLINKS_BEFORE_NODES: false,
  MIXED_LAYOUT_ALL_NODES: true,
  MIXED_LAYOUT_IF_MORE_THAN: 1,
};

// ─── Array field nomlari (CSV/import uchun) ────────────────────────────────
export const ARRAY_FIELDS = ['tags', 'pids'];

export const VERSION = '1.0.0';
