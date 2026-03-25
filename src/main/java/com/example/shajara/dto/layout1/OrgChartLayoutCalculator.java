package com.example.shajara.dto.layout1;

import org.springframework.stereotype.Component;
import java.util.*;

/**
 * Buchheim (improved Walker) algorithm — O(n) tree layout
 *
 * Qoidalar:
 * - Parent har doim farzanddan YUQORIDA (Y = level * levelHeight)
 * - Sibling va spouselar EKV QATORDA (same level)
 * - apportion() metodi bilan USTMA-UST tushish oldini olinadi
 */
@Component
public class OrgChartLayoutCalculator {

    // ── Constants ──────────────────────────────────────────────────────────────
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

    // ── Public DTOs ────────────────────────────────────────────────────────────

    public static class LayoutConfig {
        public int orientation = ORIENTATION_TOP;
        public double levelSeparation = 60;
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

    // ── Internal node (Buchheim fields) ────────────────────────────────────────

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

        // 2. Wire parent → children / stChildren
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
            }
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
            if (n.isPartner && n.parent != null) {
                partnersByParent
                        .computeIfAbsent(n.parent.id, k -> new ArrayList<>())
                        .add(n);
                n.parent.children.remove(n); // Buchheim uchun vaqtinchalik olib chiqamiz
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
        double offset = 0;

        for (Node root : roots) {
            // a) assign levels
            assignLevels(root, 0, new HashSet<>());

            // b) Buchheim init + firstWalk (partners excluded)
            initBuchheim(root);
            firstWalk(root, cfg);

            // c) secondWalk — sets final x/y relative to origin
            secondWalk(root, -root.prelim, 0, cfg, levelMaxH);

            // d) apply orientation transforms
            applyOrientation(root, cfg.orientation);

            // e) shift to offsetX so trees don't overlap
            if (horiz) {
                double minX = subtreeMinX(root);
                shiftSubtreeX(root, -minX + offset);
                offset = subtreeMaxX(root) + cfg.subtreeSeparation;
            } else {
                double minY = subtreeMinY(root);
                shiftSubtreeY(root, -minY + offset);
                offset = subtreeMaxY(root) + cfg.subtreeSeparation;
            }
        }

        // 8. PARTNER NODES reposition: parent bilan bir xil Y, yoniga X
        for (Map.Entry<String, List<Node>> e : partnersByParent.entrySet()) {
            Node parentNode = nodeMap.get(e.getKey());
            if (parentNode == null)
                continue;

            // Juftlar orasidagi masofa: 4x ko'proq
            double gap = cfg.partnerNodeSeparation * 4;

            // Right va left partnerlarni alohida ajratamiz
            List<Node> rightPartners = new ArrayList<>();
            List<Node> leftPartners = new ArrayList<>();
            for (Node partner : e.getValue()) {
                if (partner.partnerType == 2)
                    leftPartners.add(partner);
                else
                    rightPartners.add(partner);
            }

            // ── RIGHT partners: Main ──[S1]──[S2]────[S3 (extra gap before last)] ──
            double xRight = parentNode.x + parentNode.width + gap;
            for (int ri = 0; ri < rightPartners.size(); ri++) {
                Node partner = rightPartners.get(ri);

                // Agar bir nechta spousedan oxirgisi bo'lsa — extra gap qo'sh
                boolean isLastRight = (ri == rightPartners.size() - 1) && (ri > 0);
                if (isLastRight)
                    xRight += gap * 0.5; // oxirgi uchun yarim gap ko'proq

                partner.x = xRight;
                partner.y = parentNode.y;
                partner.level = parentNode.level;
                xRight += partner.width + gap;

                parentNode.children.add(partner);

                // Partner ning o'z farzandlarini to'g'ri joylash (y=0 bug oldini ol)
                layoutPartnerSubtree(partner, cfg, levelMaxH);
            }

            // ── LEFT partners: [S3 (extra gap)]────[S2]──[S1]── Main ──
            double xLeft = parentNode.x - gap;
            for (int li = 0; li < leftPartners.size(); li++) {
                Node partner = leftPartners.get(li);

                boolean isLastLeft = (li == leftPartners.size() - 1) && (li > 0);
                if (isLastLeft)
                    xLeft -= gap * 0.5; // oxirgi uchun yarim gap ko'proq

                xLeft -= partner.width;
                partner.x = xLeft;
                partner.y = parentNode.y;
                partner.level = parentNode.level;
                xLeft -= gap;

                parentNode.children.add(partner);

                // Partner ning o'z farzandlarini to'g'ri joylash (y=0 bug oldini ol)
                layoutPartnerSubtree(partner, cfg, levelMaxH);
            }
        }

        // 9. Fix sibling overlaps caused by partners expanding each child's width
        // (Buchheim knew only node widths; partners add extra space AFTER layout)
        for (Node root : roots)
            fixSiblingOverlaps(root, cfg);

        // 10. setNeighbors → THEN collectPositions
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
            LayoutConfig cfg, Map<Integer, Double> levelMaxH) {
        // Non-partner farzandlar bormi?
        boolean hasRealChildren = false;
        for (Node c : partner.children)
            if (!c.isPartner) { hasRealChildren = true; break; }
        if (!hasRealChildren) return;

        // Partner ning x va y ni saqlab olamiz (biz allaqachon to'g'ri set qildik)
        double savedX = partner.x;
        double savedY = partner.y;

        // Levels ni partner dan boshlab qayta belgilaymiz
        assignLevels(partner, partner.level, new HashSet<>());

        // Mini Buchheim: init → firstWalk → secondWalk
        initBuchheim(partner);
        firstWalk(partner, cfg);
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
     */
    private void fixSiblingOverlaps(Node v, LayoutConfig cfg) {
        // Collect non-partner children (regular order)
        List<Node> reg = new ArrayList<>();
        for (Node c : v.children)
            if (!c.isPartner)
                reg.add(c);

        for (int i = 0; i + 1 < reg.size(); i++) {
            Node curr = reg.get(i);
            Node next = reg.get(i + 1);

            // Right edge of curr's FULL family (includes partners & their subtrees)
            double currRight = subtreeMaxX(curr);
            // Left edge of next's family (may have left-side partners)
            double nextLeft = subtreeMinX(next);

            double overlap = currRight + cfg.siblingSeparation - nextLeft;
            if (overlap > 0.01) {
                // Shift next AND all right siblings by overlap amount
                for (int j = i + 1; j < reg.size(); j++)
                    shiftSubtreeX(reg.get(j), overlap);
            }
        }
        // Recurse into non-partner children
        for (Node c : v.children)
            if (!c.isPartner)
                fixSiblingOverlaps(c, cfg);
    }

    // ==================== LEVEL HELPER ====================

    private void assignLevels(Node v, int level, Set<String> seen) {
        if (seen.contains(v.id))
            return;
        seen.add(v.id);
        v.level = level;
        for (Node c : v.children) {
            // Partner nodelar parent bilan BIR XIL LEVEL (pastga emas, yoniga)
            int childLevel = c.isPartner ? level : level + 1;
            assignLevels(c, childLevel, seen);
        }
        for (Node c : v.stChildren)
            if (c.parent == null)
                assignLevels(c, level + 1, seen);
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

    private void firstWalk(Node v, LayoutConfig cfg) {
        if (v.children.isEmpty()) {
            // Leaf node
            Node w = leftSibling(v);
            if (w != null)
                v.prelim = w.prelim + nodeWidth(w) + cfg.siblingSeparation;
            else
                v.prelim = 0;
        } else {
            Node defaultAncestor = v.children.get(0);
            for (Node w : v.children) {
                firstWalk(w, cfg);
                defaultAncestor = apportion(w, defaultAncestor, cfg);
            }
            executeShifts(v);

            double firstPrelim = v.children.get(0).prelim;
            double lastPrelim = v.children.get(v.children.size() - 1).prelim
                    + nodeWidth(v.children.get(v.children.size() - 1));
            double midpoint = (firstPrelim + lastPrelim) / 2.0;

            Node w = leftSibling(v);
            if (w != null) {
                v.prelim = w.prelim + nodeWidth(w) + cfg.siblingSeparation;
                v.mod = v.prelim - midpoint;
            } else {
                v.prelim = midpoint;
            }
        }
    }

    // ==================== APPORTION (key overlap-fix method) ====================

    /**
     * Buchheim apportion — moves conflicting subtrees apart.
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

    // ==================== NEIGHBORS ====================

    private void setNeighbors(List<Node> roots, int orientation) {
        Map<Integer, List<Node>> levelMap = new LinkedHashMap<>();
        Set<String> seen = new HashSet<>();
        for (Node root : roots)
            collectByLevel(root, levelMap, seen);

        boolean horiz = (orientation == ORIENTATION_TOP
                || orientation == ORIENTATION_BOTTOM);

        for (List<Node> nodes : levelMap.values()) {
            nodes.sort(horiz
                    ? Comparator.comparingDouble(n -> n.x)
                    : Comparator.comparingDouble(n -> n.y));
            for (int i = 0; i < nodes.size(); i++) {
                Node n = nodes.get(i);
                n.leftNeighbor = (i > 0) ? nodes.get(i - 1) : null;
                n.rightNeighbor = (i < nodes.size() - 1) ? nodes.get(i + 1) : null;
            }
        }
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
