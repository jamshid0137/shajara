package com.example.shajara.dto.layout1new;

import org.springframework.stereotype.Component;
import java.util.*;

/**
 * Buchheim (improved Walker) algorithm вЂ” O(n) tree layout
 *
 * Qoidalar:
 * - Parent har doim farzanddan YUQORIDA (Y = level * levelHeight)
 * - Sibling va spouselar EKV QATORDA (same level)
 * - apportion() metodi bilan USTMA-UST tushish oldini olinadi
 */
@Component
public class OrgChartLayoutCalculatorNew {

    // в”Ђв”Ђ Constants
    // в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ
    public static final int ORIENTATION_TOP = 0;
    public static final int ORIENTATION_BOTTOM = 1;
    public static final int ORIENTATION_RIGHT = 2;
    public static final int ORIENTATION_LEFT = 3;

    public static final int LAYOUT_NORMAL = 0;
    public static final int LAYOUT_MIXED = 1;
    public static final int LAYOUT_TREE = 2;
    public static final int LAYOUT_TREE_LEFT_OFFSET = 3;
    public static final int LAYOUT_TREE_RIGHT_OFFSET = 4;
    public static final int LAYOUT_TREE_LEFT = 5;
    public static final int LAYOUT_TREE_RIGHT = 6;
    public static final int LAYOUT_GRID = -1;

    // в”Ђв”Ђ Public DTOs
    // в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

    public static class LayoutConfig {
        public int orientation = ORIENTATION_TOP;
        public double levelSeparation = 100; // farzandlarni biroz pastga tushirish uchun 60 dan 100 ga oshirildi
        public double mixedHierarchyNodesSeparation = 15;
        public double assistantSeparation = 100;
        public double subtreeSeparation = 40;
        public double siblingSeparation = 20;
        public int layout = LAYOUT_NORMAL;
        public int columns = 10;
        public double partnerNodeSeparation = 15;
    }

    public static class NodeInput {
        public String id;
        public String parentId;
        public String stParentId;
        public double width = 250;
        public double height = 120;
        public List<String> childrenIds = new ArrayList<>();
        public List<String> stChildrenIds = new ArrayList<>();
        public int layout = 0;
        public boolean isAssistant = false;
        public boolean isSplit = false;
        public boolean isMirror = false;
        public boolean isPartner = false;
        public int partnerType = 0;
        public boolean hasPartners = false;
        public double partnerSeparation = 65;
        public double[] padding = { 0, 0, 0, 0 };
        public String lcn = "";
    }

    public static class NodePosition {
        public String id;
        public double x, y, width, height;
        public String leftNeighborId;
        public String rightNeighborId;

        public NodePosition(String id, double x, double y, double w, double h) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
        }
    }

    // в”Ђв”Ђ Internal node (Buchheim fields)
    // в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

    private static class Node {
        // identity
        String id;
        int level = 0;
        double width, height;

        // final position
        double x = 0, y = 0;

        // Buchheim algorithm fields
        double prelim = 0; // preliminary x
        double mod = 0; // modifier (shift descendants)
        double change = 0; // shift change (for executeShifts)
        double shift = 0; // accumulated shift
        int number = 1; // 1-based index among this node's siblings
        Node thread = null;
        Node ancestor = null; // initialized to self

        // links
        Node parent = null;
        Node leftNeighbor = null; // same level, left
        Node rightNeighbor = null; // same level, right
        List<Node> children = new ArrayList<>();
        List<Node> stChildren = new ArrayList<>(); // subtree / partner

        // metadata
        boolean isPartner = false;
        boolean isSplit = false;
        boolean isAssistant = false;
        int partnerType = 0; // 1=right, 2=left
        int layout = 0;
        String lcn = "";

        Node(String id, double w, double h) {
            this.id = id;
            this.width = w;
            this.height = h;
            this.ancestor = this; // default: self
        }
    }

    // ==================== PUBLIC ENTRY POINT ====================

    public Map<String, NodePosition> calculate(
            List<NodeInput> nodeInputs,
            List<String> rootIds,
            Map<String, LayoutConfig> layoutConfigs) {

        if (nodeInputs == null || nodeInputs.isEmpty())
            return Collections.emptyMap();

        // 1. Build nodeMap
        Map<String, Node> nodeMap = new LinkedHashMap<>();
        for (NodeInput inp : nodeInputs) {
            Node n = new Node(inp.id,
                    inp.width > 0 ? inp.width : 250,
                    inp.height > 0 ? inp.height : 120);
            n.isPartner = inp.isPartner;
            n.isSplit = inp.isSplit;
            n.isAssistant = inp.isAssistant;
            n.partnerType = inp.partnerType;
            n.layout = inp.layout;
            n.lcn = inp.lcn != null ? inp.lcn : "";
            nodeMap.put(inp.id, n);
        }

        // 2. Wire parent в†’ children / stChildren
        for (NodeInput inp : nodeInputs) {
            Node node = nodeMap.get(inp.id);
            if (node == null)
                continue;

            if (inp.parentId != null) {
                Node p = nodeMap.get(inp.parentId);
                if (p != null) {
                    node.parent = p;
                    p.children.add(node);
                }
            } else if (inp.stParentId != null) {
                // BUG FIX: Bitta parent qolganda FamilyTree.js pid ni null qilib stpid
                // yuboradi.
                // Ularni oddiy children sifatida ko'rishimiz kerak, aks holda firstWalk da
                // o'tkazib yuborilib,
                // x=0 koordinatada ustma-ust tushib qoladi.
                Node sp = nodeMap.get(inp.stParentId);
                if (sp != null) {
                    node.parent = sp;
                    sp.children.add(node);
                }
            }
            // stChildren ro'yxatiga ham qo'shamiz (library reference uchun)
            if (inp.stParentId != null) {
                Node sp = nodeMap.get(inp.stParentId);
                if (sp != null)
                    sp.stChildren.add(node);
            }
        }

        // 3. Collect roots
        List<Node> roots = new ArrayList<>();
        if (rootIds != null && !rootIds.isEmpty()) {
            for (String rid : rootIds) {
                Node r = nodeMap.get(rid);
                if (r != null)
                    roots.add(r);
            }
        } else {
            for (Node n : nodeMap.values())
                if (n.parent == null)
                    roots.add(n);
        }
        if (roots.isEmpty())
            return Collections.emptyMap();

        // 4. Config
        LayoutConfig cfg = (layoutConfigs != null)
                ? layoutConfigs.getOrDefault("base", new LayoutConfig())
                : new LayoutConfig();

        // 5. PARTNER NODES: vaqtinchalik children listidan ajrat
        // (isPartner=true bo'lgan nodelar parent bilan bir xil qatorda bo'lishi kerak)
        Map<String, List<Node>> partnersByParent = new LinkedHashMap<>();
        for (Node n : nodeMap.values()) {
            List<Node> extractedPartners = new ArrayList<>();
            for (Node c : n.children) {
                if (c.isPartner) {
                    extractedPartners.add(c);
                }
            }
            if (!extractedPartners.isEmpty()) {
                partnersByParent.put(n.id, extractedPartners);

                Iterator<Node> it = n.children.iterator();
                while (it.hasNext()) {
                    Node c = it.next();
                    if (c.isPartner) {
                        it.remove(); // Buchheim uchun vaqtinchalik olib chiqamiz
                    } else if (c.id != null && c.id.startsWith("_ft_child_group_")) {
                        try {
                            String numStr = c.id.substring("_ft_child_group_".length());
                            int pIndex = Integer.parseInt(numStr);
                            // Agar guruh formati mos bo'lsa, mos partnerga farzand sifatida biriktirish
                            // Shu orqali kutilgan qaram (Add son/daughter) lar faqat o'sha ota/onaning
                            // layoutPartnerSubtree funksiyasi orqali uning aniq tagida markazlashib
                            // qo'yilladi.
                            if (pIndex >= 0 && pIndex < extractedPartners.size()) {
                                Node partner = extractedPartners.get(pIndex);
                                partner.children.add(c);
                                c.parent = partner;
                                it.remove();
                            }
                        } catch (Exception ignored) {
                        }
                    } else {
                        // MAVJUD (Haqiqiy) FARZANDLARNI HAM MOS SPOUSE TAGIGA JOYLASHTIRISH
                        for (Node partner : extractedPartners) {
                            if (partner.stChildren.contains(c)) {
                                // "Add child" (qo'shadigan) tugunini partnerning ichidan qidiramiz
                                Node attachNode = partner;
                                for (Node pc : partner.children) {
                                    if (pc.id != null && pc.id.startsWith("_ft_child_group_")) {
                                        attachNode = pc;
                                        break;
                                    }
                                }
                                attachNode.children.add(c);
                                c.parent = attachNode;
                                it.remove();
                                break;
                            }
                        }
                    }
                }
            }
        }

        // 6. Build level-maxHeight map (for correct Y per-level)
        Map<Integer, Double> levelMaxH = new LinkedHashMap<>();
        Set<String> seen = new HashSet<>();
        for (Node root : roots)
            collectLevelHeights(root, levelMaxH, seen);

        // 7. Layout each root subtree (partners temporarily excluded)
        boolean horiz = (cfg.orientation == ORIENTATION_TOP
                || cfg.orientation == ORIENTATION_BOTTOM);

        for (Node root : roots) {
            // a) assign levels
            assignLevels(root, 0, new HashSet<>(), layoutConfigs);

            // b) Buchheim init + firstWalk (partners excluded)
            initBuchheim(root);
            firstWalk(root, cfg, layoutConfigs);

            // c) secondWalk вЂ” sets final x/y relative to origin
            secondWalk(root, -root.prelim, 0, cfg, levelMaxH);

            // d) apply orientation transforms
            applyOrientation(root, cfg.orientation);
        }

        // 8. PARTNER NODES: Vertikal ustun — parent.y DAN BOSHLAB pastga.
        // Parent node doim YUQORIDA qoladi (tepa-tekislash).
        // Spouselar parent ostiga ketma-ket joylashadi.
        // Ko'p spouse bo'lsa: parent yuqorida, bolalar pastga suradi (8.5-qadam).
        //
        // hGap = parentdan spouse ustunigacha gorizontal bo'shliq
        // vGap = spouselar orasidagi vertikal bo'shliq
        double hGap = cfg.partnerNodeSeparation * 4; // gorizontal: 60px (2x kengroq)
        double vGap = cfg.partnerNodeSeparation; // vertikal: 15px (kichik, ixcham ustun)

        for (Map.Entry<String, List<Node>> e : partnersByParent.entrySet()) {
            Node parentNode = nodeMap.get(e.getKey());
            if (parentNode == null)
                continue;

            // partnerType=1 -> RIGHT, partnerType=2 -> LEFT
            List<Node> rightPartners = new ArrayList<>();
            List<Node> leftPartners = new ArrayList<>();
            for (Node partner : e.getValue()) {
                if (partner.partnerType == 2)
                    leftPartners.add(partner);
                else
                    rightPartners.add(partner);
            }

            // LEVEL MAX_X/MIN_X xaritasi (bolalar va add son/daughter lar ustma-ust tushmasligi uchun)
            Map<Integer, Double> partnerLevelMaxXMap = new HashMap<>();
            Map<Integer, Double> partnerLevelMinXMap = new HashMap<>();

            // Asosiy odamning (parentning) o'z non-partner qismini band qilingan deb belgilash
            for (Node c : parentNode.children) {
                if (!c.isPartner) {
                    populateMaxXMap(c, partnerLevelMaxXMap);
                    populateMinXMap(c, partnerLevelMinXMap);
                }
            }

            // -- LEFT: parent.y dan boshlab pastga, chap tomonda ---------------
            if (!leftPartners.isEmpty()) {
                double maxW = leftPartners.stream().mapToDouble(p -> p.width).max().orElse(0);
                double xLeft = parentNode.x - maxW - hGap;
                double curY = parentNode.y; // parent bilan tepa-tekislash
                for (Node partner : leftPartners) {
                    partner.x = xLeft;
                    partner.y = curY;
                    partner.level = parentNode.level;
                    curY += partner.height + vGap;
                    parentNode.children.add(partner);
                    
                    layoutPartnerSubtree(partner, cfg, levelMaxH, layoutConfigs);
                    
                    // Agar partner farzandlari (yoki qaramlari) bo'lsa, xaritadagi chap chegara bo'yicha chapga suramiz
                     double requiredShift = 0;
                     for (Node c : partner.children) {
                         if (!c.isPartner) {
                             double shift = checkMinXOverlap(c, partnerLevelMinXMap, cfg.siblingSeparation);
                             if (shift < requiredShift) requiredShift = shift; // manfiy siljish
                         }
                     }
                     if (requiredShift < 0) {
                         for (Node c : partner.children) {
                             if (!c.isPartner) shiftSubtreeX(c, requiredShift);
                         }
                     }
                     // Xaritani yangilaymiz
                     for (Node c : partner.children) {
                         if (!c.isPartner) populateMinXMap(c, partnerLevelMinXMap);
                     }
                }
            }

            // -- RIGHT: parent.y dan boshlab pastga, o'ng tomonda --------------
            if (!rightPartners.isEmpty()) {
                double xRight = parentNode.x + parentNode.width + hGap;
                double curY = parentNode.y; // parent bilan tepa-tekislash
                for (Node partner : rightPartners) {
                    partner.x = xRight;
                    partner.y = curY;
                    partner.level = parentNode.level;
                    curY += partner.height + vGap;
                    parentNode.children.add(partner);
                    
                    layoutPartnerSubtree(partner, cfg, levelMaxH, layoutConfigs);
                    
                    // Agar partner farzandlari (yoki qaramlari) bo'lsa, xaritadagi o'ng chegara bo'yicha o'ngga suramiz
                    double requiredShift = 0;
                    for (Node c : partner.children) {
                        if (!c.isPartner) {
                            double shift = checkMaxXOverlap(c, partnerLevelMaxXMap, cfg.siblingSeparation);
                            if (shift > requiredShift) requiredShift = shift; // musbat siljish
                        }
                    }
                    if (requiredShift > 0) {
                        for (Node c : partner.children) {
                            if (!c.isPartner) shiftSubtreeX(c, requiredShift);
                        }
                    }
                    // Xaritani yangilaymiz
                    for (Node c : partner.children) {
                        if (!c.isPartner) populateMaxXMap(c, partnerLevelMaxXMap);
                    }
                }
            }
        }

        // 8.5. CHILDREN PUSH DOWN: Spouse ustun pastga chiqqanda, bolalarni pastga sur.
        // Ko'p spouse bo'lsa: spouse'larning eng pastki nuqtasidan keyin bolalar
        // boshlanadi.
        // Bu "farzandlar pastga siljiydi" hodisasini ta'minlaydi.
        for (Map.Entry<String, List<Node>> e : partnersByParent.entrySet()) {
            Node parentNode = nodeMap.get(e.getKey());
            if (parentNode == null)
                continue;

            // Barcha spouse'larning eng pastki nuqtasi
            double maxSpouseBottom = parentNode.y + parentNode.height;
            for (Node partner : e.getValue()) {
                double bottom = partner.y + partner.height;
                if (bottom > maxSpouseBottom)
                    maxSpouseBottom = bottom;
            }

            // Bolalar kamida shu nuqtadan + levelSeparation pastda bo'lishi kerak
            double requiredChildY = maxSpouseBottom + cfg.levelSeparation;

            for (Node child : parentNode.children) {
                if (!child.isPartner && child.y < requiredChildY) {
                    shiftSubtreeY(child, requiredChildY - child.y);
                }
            }
        }

        // 9. Fix sibling overlaps caused by partners expanding each child's width
        // (Buchheim knew only node widths; partners add extra space AFTER layout)
        for (Node root : roots)
            fixSiblingOverlaps(root, cfg, partnersByParent);

        // 9.5. Offset roots so disconnected trees or subtrees don't overlap each other
        // This MUST be done AFTER partners are added and fixSiblingOverlaps is applied!
        double offset = 0;
        for (Node root : roots) {
            if (horiz) {
                double minX = subtreeMinX(root);
                shiftSubtreeX(root, -minX + offset);
                offset = subtreeMaxX(root) + cfg.subtreeSeparation + 100; // Qo'shimcha 100px uzoqlik ishonch uchun
            } else {
                double minY = subtreeMinY(root);
                shiftSubtreeY(root, -minY + offset);
                offset = subtreeMaxY(root) + cfg.subtreeSeparation + 100;
            }
        }

        // 9.9. FINAL PARTNER POSITION FIX
        // Barcha overlap fix qadamlaridan KEYIN partner pozitsiyalarini qayta
        // hisoblash,
        // shunda ular turli masofalarda yoki ustma-ust qolib ketmasdan har doim bir xil
        // masofada bo'ladi.
        for (Map.Entry<String, List<Node>> e : partnersByParent.entrySet()) {
            Node parentNode = nodeMap.get(e.getKey());
            if (parentNode == null)
                continue;

            List<Node> rightPartners = new ArrayList<>();
            List<Node> leftPartners = new ArrayList<>();
            for (Node partner : e.getValue()) {
                if (partner.partnerType == 2)
                    leftPartners.add(partner);
                else
                    rightPartners.add(partner);
            }

            if (!leftPartners.isEmpty()) {
                double maxW = leftPartners.stream().mapToDouble(p -> p.width).max().orElse(0);
                double xLeft = parentNode.x - maxW - hGap;
                double curY = parentNode.y;
                for (Node partner : leftPartners) {
                    double dx = xLeft - partner.x;
                    double dy = curY - partner.y;
                    shiftSubtreeX(partner, dx);
                    shiftSubtreeY(partner, dy);
                    curY += partner.height + vGap;
                }
            }

            if (!rightPartners.isEmpty()) {
                double xRight = parentNode.x + parentNode.width + hGap;
                double curY = parentNode.y;
                for (Node partner : rightPartners) {
                    double dx = xRight - partner.x;
                    double dy = curY - partner.y;
                    shiftSubtreeX(partner, dx);
                    shiftSubtreeY(partner, dy);
                    curY += partner.height + vGap;
                }
            }
        }

        // 10. GLOBAL LEVEL Y SINXRONIZATSIYA
        // Muammo: bir odamning ko'p juftlari bo'lsa, uning bolalari pastroqqa tushadi.
        // Lekin o'sha qatlamdagi boshqa odamlarning bolalari yuqorida qoladi.
        // Yechim: har bir level uchun eng katta (eng past) Y ni topib,
        //         shu qatlamdagi BARCHA nodelarni shu Y ga sinxronlashtirish.
        synchronizeLevelY(roots, cfg, nodeMap, partnersByParent, hGap, vGap);

        setNeighbors(new ArrayList<>(roots), cfg.orientation);

        Map<String, NodePosition> result = new LinkedHashMap<>();
        Set<String> visited = new HashSet<>();
        for (Node root : roots)
            collectPositions(root, result, visited);

        return result;
    }

    // ==================== LEVEL HELPER ====================

    /**
     * Partner nodening o'z farzandlarini to'g'ri joylashtiramiz.
     * Sabab: partner Buchheim dan olib chiqilganligi uchun uning
     * farzandlari hech qachon positionlanmagan bo'lishi mumkin (y=0 bug).
     *
     * Bu mini-Buchheim partner ni vaqtinchalik root sifatida ishlatib,
     * farzandlar uchun to'g'ri x va y ni hisoblaydi.
     */
    private void layoutPartnerSubtree(Node partner,
            LayoutConfig cfg, Map<Integer, Double> levelMaxH, Map<String, LayoutConfig> layoutConfigs) {
        // Non-partner farzandlar bormi?
        boolean hasRealChildren = false;
        for (Node c : partner.children)
            if (!c.isPartner) {
                hasRealChildren = true;
                break;
            }
        if (!hasRealChildren)
            return;

        // Partner ning x va y ni saqlab olamiz (biz allaqachon to'g'ri set qildik)
        double savedX = partner.x;
        double savedY = partner.y;

        // Levels ni partner dan boshlab qayta belgilaymiz
        assignLevels(partner, partner.level, new HashSet<>(), layoutConfigs);

        // Mini Buchheim: init в†’ firstWalk в†’ secondWalk
        initBuchheim(partner);
        firstWalk(partner, cfg, layoutConfigs);
        // secondWalk ni 0-dan boshlash: partner.x = 0 atrofida hisoblash
        secondWalk(partner, -partner.prelim, 0, cfg, levelMaxH);

        // secondWalk partner.x va partner.y ni o'zgartirdi, ularni tiklymiz
        double dx = savedX - partner.x;
        // Y shift ham kerak bo'lishi mumkin (partner.y level dan hisoblangan)
        double dy = savedY - partner.y;

        partner.x = savedX;
        partner.y = savedY;

        // Barcha farzandlarni dx, dy ga ko'chiramiz
        for (Node c : partner.children) {
            shiftSubtreeX(c, dx);
            shiftSubtreeY(c, dy);
        }
        // stChildren ham (agar parent==null)
        for (Node c : partner.stChildren)
            if (c.parent == null) {
                shiftSubtreeX(c, dx);
                shiftSubtreeY(c, dy);
            }
    }

    /**
     * After partners are re-added to parent.children, siblings may overlap
     * because partners take extra horizontal space Buchheim didn't know about.
     * Left-to-right scan: if curr's full family (including partners+subtree)
     * overlaps with next sibling, push next (and all subsequent) right.
     *
     * BUG FIX: When shifting a sibling, also shift that sibling's own partner
     * nodes (spouses). Without this, the sibling moves but its spouses stay
     * behind and cause overlaps.
     */
    private void fixSiblingOverlaps(Node v, LayoutConfig cfg,
            Map<String, List<Node>> nodeToPartners) {
        // 1. Recurse into non-partner children FIRST (BOTTOM-UP traversal)
        for (Node c : v.children)
            if (!c.isPartner)
                fixSiblingOverlaps(c, cfg, nodeToPartners);

        // 2. Collect non-partner children (regular order)
        List<Node> reg = new ArrayList<>();
        for (Node c : v.children)
            if (!c.isPartner)
                reg.add(c);

        if (reg.isEmpty())
            return;

        // 3. Fix sibling overlaps at THIS level using accumulated left nodes
        List<Node> accumulatedLeftNodes = new ArrayList<>();
        collectAllNodesWithPartners(reg.get(0), accumulatedLeftNodes);

        for (int i = 0; i + 1 < reg.size(); i++) {
            Node next = reg.get(i + 1);

            List<Node> nextNodes = new ArrayList<>();
            collectAllNodesWithPartners(next, nextNodes);

            double maxOverlap = 0;
            for (Node a : accumulatedLeftNodes) {
                for (Node b : nextNodes) {
                    boolean yOverlaps = (a.y < b.y + b.height) && (b.y < a.y + a.height);
                    if (yOverlaps) {
                        double overlap = a.x + a.width + cfg.siblingSeparation - b.x;
                        if (overlap > maxOverlap) {
                            maxOverlap = overlap;
                        }
                    }
                }
            }

            if (maxOverlap > 0.01) {
                // Shift next AND all right siblings by maxOverlap amount
                // Note: partners are already in sib.children, so shiftSubtreeX shifts them
                // automatically.
                for (int j = i + 1; j < reg.size(); j++) {
                    Node sib = reg.get(j);
                    shiftSubtreeX(sib, maxOverlap);
                }
            }

            accumulatedLeftNodes.addAll(nextNodes);
        }
    }

    private void collectAllNodesWithPartners(Node v, List<Node> list) {
        if (v == null)
            return;
        list.add(v);
        for (Node c : v.children) {
            collectAllNodesWithPartners(c, list);
        }
        for (Node c : v.stChildren) {
            if (c.parent == null) {
                collectAllNodesWithPartners(c, list);
            }
        }
    }

    /**
     * Right boundary of a node's full family:
     * the node's own subtree + all of its partner (spouse) subtrees.
     */
    private double fullFamilyMaxX(Node node, Map<String, List<Node>> nodeToPartners) {
        double max = subtreeMaxX(node);
        for (Node p : nodeToPartners.getOrDefault(node.id, Collections.emptyList()))
            max = Math.max(max, subtreeMaxX(p));
        return max;
    }

    /**
     * Left boundary of a node's full family:
     * the node's own subtree + all of its partner (spouse) subtrees.
     */
    private double fullFamilyMinX(Node node, Map<String, List<Node>> nodeToPartners) {
        double min = subtreeMinX(node);
        for (Node p : nodeToPartners.getOrDefault(node.id, Collections.emptyList()))
            min = Math.min(min, subtreeMinX(p));
        return min;
    }

    // ==================== MAX X, MIN X MAP HELPERS ====================
    private void populateMaxXMap(Node n, Map<Integer, Double> map) {
        double right = n.x + n.width;
        if (!map.containsKey(n.level) || right > map.get(n.level)) {
            map.put(n.level, right);
        }
        for (Node c : n.children) populateMaxXMap(c, map);
        for (Node c : n.stChildren) if (c.parent == null) populateMaxXMap(c, map);
    }
    
    private void populateMinXMap(Node n, Map<Integer, Double> map) {
        double left = n.x;
        if (!map.containsKey(n.level) || left < map.get(n.level)) {
            map.put(n.level, left);
        }
        for (Node c : n.children) populateMinXMap(c, map);
        for (Node c : n.stChildren) if (c.parent == null) populateMinXMap(c, map);
    }
    
    private double checkMaxXOverlap(Node n, Map<Integer, Double> map, double sep) {
        double shift = 0;
        if (map.containsKey(n.level)) {
            double safeX = map.get(n.level) + sep;
            if (n.x < safeX) {
                double diff = safeX - n.x;
                if (diff > shift) shift = diff;
            }
        }
        for (Node c : n.children) {
            double cShift = checkMaxXOverlap(c, map, sep);
            if (cShift > shift) shift = cShift;
        }
        return shift;
    }
    
    private double checkMinXOverlap(Node n, Map<Integer, Double> map, double sep) {
        double shift = 0; // Manfiy qaytish kerak
        if (map.containsKey(n.level)) {
            double safeX = map.get(n.level) - sep - n.width;
            if (n.x > safeX) {
                double diff = safeX - n.x; // manfiy son
                if (diff < shift) shift = diff;
            }
        }
        for (Node c : n.children) {
            double cShift = checkMinXOverlap(c, map, sep);
            if (cShift < shift) shift = cShift;
        }
        return shift;
    }

    // ==================== LEVEL Y SYNC ====================

    /**
     * Har bir qatlamdagi (level) nodlarning Y koordinatalarini sinxronlashtirish.
     *
     * Sabab: ko'p juftli (multi-spouse) personning bolalari pastroqqa tushadi
     * (8.5-qadam tufayli). Lekin o'sha qatlamdagi boshqa personlarning bolalari
     * yuqorida qoladi. Vizual natija: bir avlodning bolalari ikki xil balandlikda.
     *
     * Algoritm:
     * 1. Barcha non-partner nodelarni level bo'yicha guruhlash
     * 2. Har bir level uchun maxY ni topish (eng past joylashgan node)
     * 3. Shu levelda Y < maxY bo'lgan nodelarni (va subtreeni) pastga siljitish
     * 4. Partner pozitsiyalarini qayta tiklash (ular parent bilan birga turishi kerak)
     */
    private void synchronizeLevelY(
            List<Node> roots,
            LayoutConfig cfg,
            Map<String, Node> nodeMap,
            Map<String, List<Node>> partnersByParent,
            double hGap, double vGap) {

        // 1. Level bo'yicha non-partner nodelarni yig'amiz
        Map<Integer, List<Node>> levelMap = new LinkedHashMap<>();
        Set<String> seen = new HashSet<>();
        for (Node root : roots)
            collectNonPartnerByLevel(root, levelMap, seen);

        // 2. Har bir level uchun maxY topib, siljitamiz
        List<Integer> sortedLevels = new ArrayList<>(levelMap.keySet());
        Collections.sort(sortedLevels);

        for (int level : sortedLevels) {
            List<Node> nodes = levelMap.get(level);
            if (nodes == null || nodes.isEmpty()) continue;

            // Shu qatlamdagi eng katta Y (eng pastdagi node)
            double maxY = nodes.stream().mapToDouble(n -> n.y).max().orElse(0);

            // Agar hammasi bir xil Y da bo'lsa -- skip
            boolean allSame = nodes.stream().allMatch(n -> Math.abs(n.y - maxY) < 1.0);
            if (allSame) continue;

            // Y < maxY bo'lgan nodelarni va ularning subtreelerini pastga siljitamiz
            for (Node n : nodes) {
                if (n.y < maxY - 1.0) {
                    shiftSubtreeY(n, maxY - n.y);
                }
            }
        }

        // 3. Partner pozitsiyalarini qayta tiklash
        // (subtree shift partner nodelarni ham siljitgan bo'lishi mumkin,
        //  lekin partner ota bilan bir xil Y da turishi kerak)
        for (Map.Entry<String, List<Node>> e : partnersByParent.entrySet()) {
            Node parentNode = nodeMap.get(e.getKey());
            if (parentNode == null) continue;

            List<Node> rightPartners = new ArrayList<>();
            List<Node> leftPartners = new ArrayList<>();
            for (Node partner : e.getValue()) {
                if (partner.partnerType == 2)
                    leftPartners.add(partner);
                else
                    rightPartners.add(partner);
            }

            if (!leftPartners.isEmpty()) {
                double maxW = leftPartners.stream().mapToDouble(p -> p.width).max().orElse(0);
                double xLeft = parentNode.x - maxW - hGap;
                double curY = parentNode.y;
                for (Node partner : leftPartners) {
                    double dx = xLeft - partner.x;
                    double dy = curY - partner.y;
                    shiftSubtreeX(partner, dx);
                    shiftSubtreeY(partner, dy);
                    curY += partner.height + vGap;
                }
            }

            if (!rightPartners.isEmpty()) {
                double xRight = parentNode.x + parentNode.width + hGap;
                double curY = parentNode.y;
                for (Node partner : rightPartners) {
                    double dx = xRight - partner.x;
                    double dy = curY - partner.y;
                    shiftSubtreeX(partner, dx);
                    shiftSubtreeY(partner, dy);
                    curY += partner.height + vGap;
                }
            }
        }
    }

    /**
     * Non-partner nodelarni level bo'yicha yig'adi.
     * Partner nodelar sinxronizatsiyadan chiqariladi chunki
     * ular parent bilan bir Y da turishi kerak (parent Y o'zgarsa, ular ham o'zgaradi).
     */
    private void collectNonPartnerByLevel(Node v, Map<Integer, List<Node>> map, Set<String> seen) {
        if (seen.contains(v.id)) return;
        seen.add(v.id);

        // Faqat non-partner nodelarni qo'shamiz
        if (!v.isPartner) {
            map.computeIfAbsent(v.level, k -> new ArrayList<>()).add(v);
        }

        for (Node c : v.children)
            collectNonPartnerByLevel(c, map, seen);
        for (Node c : v.stChildren)
            if (c.parent == null)
                collectNonPartnerByLevel(c, map, seen);
    }

    // ==================== LEVEL HELPER ====================

    private boolean isVerticalGroup(Node v, Map<String, LayoutConfig> configs) {
        if (v == null || v.children.isEmpty() || configs == null)
            return false;
        Node first = v.children.get(0);
        LayoutConfig cfg = configs.get(first.lcn);
        if (cfg != null && cfg.columns == 1)
            return true;

        // Fallback for strict Balkan OrgChart
        return v.id.startsWith("_ft_child_group") || (first.lcn != null && first.lcn.contains("group"));
    }

    private void assignLevels(Node v, int level, Set<String> seen, Map<String, LayoutConfig> configs) {
        if (seen.contains(v.id))
            return;
        seen.add(v.id);
        v.level = level;

        boolean vertical = isVerticalGroup(v, configs);
        int nextLevel = level + 1;

        for (Node c : v.children) {
            // Partner nodelar parent bilan BIR XIL LEVEL (pastga emas, yoniga)
            int childLevel = c.isPartner ? level : nextLevel;
            assignLevels(c, childLevel, seen, configs);

            if (vertical && !c.isPartner) {
                nextLevel++; // ustma-ust tushishi uchun har biri yangi qavatga
            }
        }
        for (Node c : v.stChildren)
            if (c.parent == null)
                assignLevels(c, nextLevel, seen, configs);
    }

    private void collectLevelHeights(Node v, Map<Integer, Double> map, Set<String> seen) {
        if (seen.contains(v.id))
            return;
        seen.add(v.id);
        map.merge(v.level, v.height, Math::max);
        for (Node c : v.children)
            collectLevelHeights(c, map, seen);
        for (Node c : v.stChildren)
            if (c.parent == null)
                collectLevelHeights(c, map, seen);
    }

    /** Cumulative Y for given level using per-level max heights */
    private double yForLevel(int level, LayoutConfig cfg,
            Map<Integer, Double> levelMaxH) {
        double y = 0;
        for (int i = 0; i < level; i++)
            y += levelMaxH.getOrDefault(i, 120.0) + cfg.levelSeparation;
        return y;
    }

    // ==================== BUCHHEIM INIT ====================

    private void initBuchheim(Node v) {
        v.ancestor = v;
        v.thread = null;
        v.change = 0;
        v.shift = 0;
        v.mod = 0;
        v.prelim = 0;
        for (int i = 0; i < v.children.size(); i++) {
            Node c = v.children.get(i);
            c.number = i + 1;
            initBuchheim(c);
        }
        // stChildren without primary parent
        for (Node c : v.stChildren)
            if (c.parent == null) {
                c.number = 1;
                initBuchheim(c);
            }
    }

    // ==================== FIRST WALK ====================

    private void firstWalk(Node v, LayoutConfig cfg, Map<String, LayoutConfig> configs) {
        boolean verticalSelf = v.parent != null && isVerticalGroup(v.parent, configs);

        if (v.children.isEmpty()) {
            // Leaf node
            Node w = leftSibling(v);
            if (w != null && !verticalSelf)
                v.prelim = w.prelim + nodeWidth(w) + cfg.siblingSeparation;
            else
                v.prelim = 0; // vertical children don't shift right
        } else {
            Node defaultAncestor = v.children.get(0);
            boolean verticalKids = isVerticalGroup(v, configs);

            for (Node w : v.children) {
                firstWalk(w, cfg, configs);
                if (!verticalKids) {
                    defaultAncestor = apportion(w, defaultAncestor, cfg);
                }
            }
            if (!verticalKids) {
                executeShifts(v);
            }

            double firstPrelim = v.children.get(0).prelim;
            double lastPrelim = v.children.get(v.children.size() - 1).prelim
                    + nodeWidth(v.children.get(v.children.size() - 1));
            // For vertical stacked kids, midpoint is just 0 since they all have prelim=0
            double midpoint = verticalKids ? 0.0 : (firstPrelim + lastPrelim) / 2.0;

            Node w = leftSibling(v);
            if (w != null && !verticalSelf) {
                v.prelim = w.prelim + nodeWidth(w) + cfg.siblingSeparation;
                v.mod = v.prelim - midpoint;
            } else {
                v.prelim = midpoint;
            }
        }
    }

    // ==================== APPORTION (key overlap-fix method) ====================

    /**
     * Buchheim apportion вЂ” moves conflicting subtrees apart.
     *
     * vir = right inner contour of left part (starts at v)
     * vor = right outer contour of right part (starts at v)
     * vil = left inner contour of left part (starts at leftSibling(v))
     * vol = left outer contour of right part (starts at leftmostSibling(v))
     */
    private Node apportion(Node v, Node defaultAncestor, LayoutConfig cfg) {
        Node w = leftSibling(v);
        if (w == null)
            return defaultAncestor;

        Node vir = v, vor = v;
        Node vil = w;
        Node vol = leftmostSibling(v);

        double sir = v.mod, sor = v.mod;
        double sil = w.mod, sol = (vol != null ? vol.mod : 0);

        while (nextRight(vil) != null && nextLeft(vir) != null) {
            vil = nextRight(vil);
            vir = nextLeft(vir);
            if (vol != null)
                vol = nextLeft(vol);
            vor = nextRight(vor);

            vor.ancestor = v;

            double shift = (vil.prelim + sil) - (vir.prelim + sir)
                    + nodeWidth(vil) + cfg.siblingSeparation;
            if (shift > 0) {
                Node anc = ancestor(vil, v, defaultAncestor);
                moveSubtree(anc, v, shift);
                sir += shift;
                sor += shift;
            }
            sil += vil.mod;
            sir += vir.mod;
            if (vol != null)
                sol += vol.mod;
            sor += vor.mod;
        }

        // Extend threads
        if (nextRight(vil) != null && nextRight(vor) == null) {
            vor.thread = nextRight(vil);
            vor.mod += sil - sor;
        }
        if (nextLeft(vir) != null && nextLeft(vol != null ? vol : v) == null) {
            if (vol != null) {
                vol.thread = nextLeft(vir);
                vol.mod += sir - sol;
            }
            defaultAncestor = v;
        }
        return defaultAncestor;
    }

    // ==================== MOVE SUBTREE ====================

    private void moveSubtree(Node wl, Node wr, double shift) {
        int subtrees = wr.number - wl.number;
        if (subtrees <= 0)
            return;
        wr.change -= shift / subtrees;
        wr.shift += shift;
        wl.change += shift / subtrees;
        wr.prelim += shift;
        wr.mod += shift;
    }

    // ==================== EXECUTE SHIFTS ====================

    private void executeShifts(Node v) {
        double shift = 0, change = 0;
        List<Node> ch = v.children;
        for (int i = ch.size() - 1; i >= 0; i--) {
            Node w = ch.get(i);
            w.prelim += shift;
            w.mod += shift;
            change += w.change;
            shift += w.shift + change;
        }
    }

    // ==================== SECOND WALK ====================

    private void secondWalk(Node v, double m, int depth,
            LayoutConfig cfg,
            Map<Integer, Double> levelMaxH) {
        v.x = v.prelim + m;
        v.y = yForLevel(v.level, cfg, levelMaxH);

        double newM = m + v.mod;
        for (Node c : v.children)
            secondWalk(c, newM, depth + 1, cfg, levelMaxH);

        // stChildren with no primary parent
        for (Node c : v.stChildren)
            if (c.parent == null)
                secondWalk(c, newM, depth + 1, cfg, levelMaxH);
    }

    // ==================== CONTOUR TRAVERSAL ====================

    /** Left contour: leftmost child, else thread */
    private Node nextLeft(Node v) {
        return v.children.isEmpty() ? v.thread : v.children.get(0);
    }

    /** Right contour: rightmost child, else thread */
    private Node nextRight(Node v) {
        return v.children.isEmpty()
                ? v.thread
                : v.children.get(v.children.size() - 1);
    }

    // ==================== SIBLING HELPERS ====================

    private Node leftSibling(Node v) {
        if (v.parent == null)
            return null;
        List<Node> sibs = v.parent.children;
        int idx = v.number - 1;
        return idx > 0 ? sibs.get(idx - 1) : null;
    }

    private Node leftmostSibling(Node v) {
        if (v.parent == null || v.number == 1)
            return null;
        return v.parent.children.get(0);
    }

    private Node ancestor(Node vil, Node v, Node defaultAncestor) {
        if (v.parent != null && v.parent.children.contains(vil.ancestor))
            return vil.ancestor;
        return defaultAncestor;
    }

    private double nodeWidth(Node n) {
        return n.width;
    }

    // ==================== BOUNDS & SHIFT ====================

    private double subtreeMinX(Node v) {
        double min = v.x;
        for (Node c : v.children)
            min = Math.min(min, subtreeMinX(c));
        for (Node c : v.stChildren)
            if (c.parent == null)
                min = Math.min(min, subtreeMinX(c));
        return min;
    }

    private double subtreeMaxX(Node v) {
        double max = v.x + v.width;
        for (Node c : v.children)
            max = Math.max(max, subtreeMaxX(c));
        for (Node c : v.stChildren)
            if (c.parent == null)
                max = Math.max(max, subtreeMaxX(c));
        return max;
    }

    private double subtreeMinY(Node v) {
        double min = v.y;
        for (Node c : v.children)
            min = Math.min(min, subtreeMinY(c));
        for (Node c : v.stChildren)
            if (c.parent == null)
                min = Math.min(min, subtreeMinY(c));
        return min;
    }

    private double subtreeMaxY(Node v) {
        double max = v.y + v.height;
        for (Node c : v.children)
            max = Math.max(max, subtreeMaxY(c));
        for (Node c : v.stChildren)
            if (c.parent == null)
                max = Math.max(max, subtreeMaxY(c));
        return max;
    }

    private void shiftSubtreeX(Node v, double dx) {
        v.x += dx;
        for (Node c : v.children)
            shiftSubtreeX(c, dx);
        for (Node c : v.stChildren)
            if (c.parent == null)
                shiftSubtreeX(c, dx);
    }

    private void shiftSubtreeY(Node v, double dy) {
        v.y += dy;
        for (Node c : v.children)
            shiftSubtreeY(c, dy);
        for (Node c : v.stChildren)
            if (c.parent == null)
                shiftSubtreeY(c, dy);
    }

    // ==================== ORIENTATION ====================

    private void applyOrientation(Node root, int orientation) {
        if (orientation == ORIENTATION_BOTTOM)
            mirrorY(root);
        else if (orientation == ORIENTATION_RIGHT)
            swapXY(root);
        else if (orientation == ORIENTATION_LEFT) {
            swapXY(root);
            mirrorX(root);
        }
    }

    private void mirrorY(Node v) {
        v.y = -v.y - v.height;
        for (Node c : v.children)
            mirrorY(c);
        for (Node c : v.stChildren)
            mirrorY(c);
    }

    private void mirrorX(Node v) {
        v.x = -v.x - v.width;
        for (Node c : v.children)
            mirrorX(c);
        for (Node c : v.stChildren)
            mirrorX(c);
    }

    private void swapXY(Node v) {
        double t = v.x;
        v.x = v.y;
        v.y = t;
        t = v.width;
        v.width = v.height;
        v.height = t;
        for (Node c : v.children)
            swapXY(c);
        for (Node c : v.stChildren)
            swapXY(c);
    }

    // ==================== GLOBAL LEVEL OVERLAP FIX ====================

    /**
     * Yakuniy global overlap fix.
     *
     * Buchheim + partner placement + fixSiblingOverlaps'dan keyin ham turli
     * subtree'lardan nodelar bir xil Y-levelde overlap qilib qolishi mumkin.
     * (Masalan: bir subtree'ning RIGHT partner'i boshqa subtree'ning LEFT partner'i
     * bilan)
     *
     * Algoritm:
     * - Har bir level uchun barcha nodelarni x bo'yicha saralamiz
     * - Ketma-ket juftlarni tekshirib, overlap bo'lsa o'ng nodeni (va uning
     * BUTUN subtree'sini) o'ngga siljitamiz
     * - O'zgarish bo'lmaguncha (yoki 5 pass'gacha) takrorlaymiz
     */
    private void globalLevelOverlapFix(List<Node> roots, LayoutConfig cfg,
            Map<String, List<Node>> nodeToPartners) {
        for (int pass = 0; pass < 5; pass++) {
            boolean changed = false;

            // Har pass'da yangi levelMap qurish (oldingi pass'da x'lar o'zgargan)
            Map<Integer, List<Node>> levelMap = new LinkedHashMap<>();
            Set<String> seen = new HashSet<>();
            for (Node root : roots)
                collectByLevel(root, levelMap, seen);

            // Level'larni yuqoridan pastga (kichikdan kattaga) qayta ishlash
            List<Integer> sortedLevels = new ArrayList<>(levelMap.keySet());
            Collections.sort(sortedLevels);

            for (int level : sortedLevels) {
                List<Node> nodes = levelMap.get(level);
                // x bo'yicha chapdan o'ngga tartibla
                nodes.sort(Comparator.comparingDouble(n -> n.x));

                for (int i = 0; i + 1 < nodes.size(); i++) {
                    Node a = nodes.get(i);
                    Node b = nodes.get(i + 1);
                    // Faqat Y-oraliqlari kesishsa tekshiramiz
                    // (vertikal ustundagi partnerlar bir xil leveldа bo'lsa ham
                    // turli Y pozitsiyada bo'ladi — ular orasida X overlap yo'q)
                    boolean yOverlaps = (a.y < b.y + b.height) && (b.y < a.y + a.height);
                    if (!yOverlaps)
                        continue;

                    double minRequiredX = a.x + a.width + cfg.siblingSeparation;
                    if (b.x < minRequiredX) {
                        double shift = minRequiredX - b.x;
                        // MUHIM: partner (spouse) nodelarni MUSTAQIL siljitmaymiz!
                        // Partner faqat egasi (main person) bilan birga harakat qilishi kerak.
                        // Aks holda "Asliddin" Abbosdan juda uzoqqa ketib qoladi.
                        if (b.isPartner)
                            continue;
                        shiftSubtreeX(b, shift);
                        changed = true;
                    }
                }
            }

            // Hech qanday overlap topilmasa — erta chiqish
            if (!changed)
                break;
        }
    }

    /**
     * FINAL SAFETY NET: Barcha nodelarni to'g'ridan-to'g'ri juftlab tekshiradi.
     * Agar ikkita nodening to'rtburchaklari kesishsa, o'ng/pastki nodeni va uning
     * spouse ustunini siljitadi. Level farqi muhim emas — butun canvas
     * tekshiriladi.
     * Max 5 pass bajariladi.
     */
    private void globalAllPairsOverlapFix(List<Node> roots, LayoutConfig cfg,
            Map<String, List<Node>> nodeToPartners) {
        for (int pass = 0; pass < 5; pass++) {
            boolean changed = false;

            // Barcha nodelarni yig'ib olish
            List<Node> all = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            for (Node root : roots)
                collectAllNodes(root, all, seen);

            // X bo'yicha tartibla (chapdan o'ngga)
            all.sort(Comparator.comparingDouble(n -> n.x));

            for (int i = 0; i < all.size(); i++) {
                Node a = all.get(i);
                for (int j = i + 1; j < all.size(); j++) {
                    Node b = all.get(j);

                    // b.x > a.x + a.width bo'lsa — keyingi barcha b lar ham overlap qilmaydi
                    if (b.x >= a.x + a.width + cfg.siblingSeparation)
                        break;

                    // To'liq to'rtburchak kesishuvini tekshirish
                    boolean xOverlaps = (a.x < b.x + b.width + cfg.siblingSeparation)
                            && (b.x < a.x + a.width + cfg.siblingSeparation);
                    boolean yOverlaps = (a.y < b.y + b.height) && (b.y < a.y + a.height);

                    if (xOverlaps && yOverlaps) {
                        double shift = (a.x + a.width + cfg.siblingSeparation) - b.x;
                        if (shift > 0.01) {
                            // MUHIM: partner (spouse) nodelarni MUSTAQIL siljitmaymiz!
                            // Partner faqat egasi bilan birga harakat qiladi (fixSiblingOverlaps da).
                            // Mustaqil siljitish katta bo'shliq (gap) paydo qiladi.
                            if (b.isPartner)
                                continue;
                            shiftSubtreeX(b, shift);
                            changed = true;
                        }
                    }
                }
            }

            if (!changed)
                break;
        }
    }

    private void collectAllNodes(Node v, List<Node> list, Set<String> seen) {
        if (seen.contains(v.id))
            return;
        seen.add(v.id);
        list.add(v);
        for (Node c : v.children)
            collectAllNodes(c, list, seen);
        for (Node c : v.stChildren)
            collectAllNodes(c, list, seen);
    }

    // ==================== NEIGHBORS ====================

    /**
     * Qo'shnilarni belgilash:
     * Faqat bir xil parentning NON-PARTNER bolalari o'zaro qo'shni bo'ladi.
     * Bu chiziqlarni bolalar ustidan o'tib ketishini oldini oladi.
     *
     * Oldingi koordinata-bazali yondashuv muammosi:
     *   - Partner (spouse) va child bir xil Y ga ega bo'lsa,
     *     ular turli darajada bo'lsa ham bir guruhga tushib,
     *     noto'g'ri qo'shnilik o'rnatilardi va chiziq bolalar ustidan o'tardi.
     */
    private void setNeighbors(List<Node> roots, int orientation) {
        boolean horiz = (orientation == ORIENTATION_TOP || orientation == ORIENTATION_BOTTOM);
        Set<String> seen = new HashSet<>();
        for (Node root : roots)
            setNeighborsForSubtree(root, horiz, seen);
    }

    private void setNeighborsForSubtree(Node v, boolean horiz, Set<String> seen) {
        if (seen.contains(v.id))
            return;
        seen.add(v.id);

        // Faqat NON-PARTNER bolalarni qo'shni qilib belgilaymiz
        List<Node> realChildren = new ArrayList<>();
        for (Node c : v.children) {
            if (!c.isPartner)
                realChildren.add(c);
        }

        if (!realChildren.isEmpty()) {
            // X (yoki Y vertikal uchun) bo'yicha tartibla
            realChildren.sort(horiz
                    ? Comparator.comparingDouble(n -> n.x)
                    : Comparator.comparingDouble(n -> n.y));

            for (int i = 0; i < realChildren.size(); i++) {
                Node n = realChildren.get(i);
                n.leftNeighbor = (i > 0) ? realChildren.get(i - 1) : null;
                n.rightNeighbor = (i < realChildren.size() - 1) ? realChildren.get(i + 1) : null;
            }
        }

        // Rekursiv barcha bolalarga ham qo'llamiz
        for (Node c : v.children)
            setNeighborsForSubtree(c, horiz, seen);
        for (Node c : v.stChildren)
            if (c.parent == null)
                setNeighborsForSubtree(c, horiz, seen);
    }

    private void collectByCoordinate(Node v, Map<Long, List<Node>> map, Set<String> seen, boolean horiz) {
        if (seen.contains(v.id))
            return;
        seen.add(v.id);
        
        long key = Math.round(horiz ? v.y : v.x);
        map.computeIfAbsent(key, k -> new ArrayList<>()).add(v);
        
        for (Node c : v.children)
            collectByCoordinate(c, map, seen, horiz);
        for (Node c : v.stChildren)
            collectByCoordinate(c, map, seen, horiz);
    }

    private void collectByLevel(Node v, Map<Integer, List<Node>> map, Set<String> seen) {
        if (seen.contains(v.id))
            return;
        seen.add(v.id);
        map.computeIfAbsent(v.level, k -> new ArrayList<>()).add(v);
        for (Node c : v.children)
            collectByLevel(c, map, seen);
        for (Node c : v.stChildren)
            collectByLevel(c, map, seen);
    }

    // ==================== COLLECT POSITIONS ====================

    private void collectPositions(Node v,
            Map<String, NodePosition> result,
            Set<String> visited) {
        if (visited.contains(v.id))
            return;
        visited.add(v.id);

        NodePosition pos = new NodePosition(v.id, v.x, v.y, v.width, v.height);
        if (v.leftNeighbor != null)
            pos.leftNeighborId = v.leftNeighbor.id;
        if (v.rightNeighbor != null)
            pos.rightNeighborId = v.rightNeighbor.id;
        result.put(v.id, pos);

        for (Node c : v.children)
            collectPositions(c, result, visited);
        for (Node c : v.stChildren)
            collectPositions(c, result, visited);
    }

    // ==================== OUTPUT FORMAT ====================

    public Map<String, Object> toOrgChartFormat(Map<String, NodePosition> positions) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, NodePosition> e : positions.entrySet()) {
            NodePosition p = e.getValue();
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("p", new double[] { p.x, p.y, p.width, p.height });
            if (p.leftNeighborId != null)
                node.put("ln", p.leftNeighborId);
            if (p.rightNeighborId != null)
                node.put("rn", p.rightNeighborId);
            result.put(e.getKey(), node);
        }
        return result;
    }
}
