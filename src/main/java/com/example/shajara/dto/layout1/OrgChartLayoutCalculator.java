package com.example.shajara.dto.layout1;

import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class OrgChartLayoutCalculator {

    // ==================== CONSTANTS ====================
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

    // ==================== DATA CLASSES ====================

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
        public double width;
        public double height;
        public List<String> childrenIds = new ArrayList<>();
        public List<String> stChildrenIds = new ArrayList<>();
        public int layout = 0;
        public boolean isAssistant = false;
        public boolean isSplit = false;
        public boolean isMirror = false;
        public boolean isPartner = false;
        public int partnerType = 0; // 1=right, 2=left
        public boolean hasPartners = false;
        public double partnerSeparation = 65;
        public double[] padding = {0, 0, 0, 0};
        public String lcn = "";
    }

    public static class NodePosition {
        public String id;
        public double x;
        public double y;
        public double width;
        public double height;
        public String leftNeighborId;
        public String rightNeighborId;

        public NodePosition(String id, double x, double y,
                           double w, double h) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.width = w;
            this.height = h;
        }
    }

    // Internal node used during calculation
    private static class Node {
        String id;
        double x = 0, y = 0;
        double width, height;
        double mod = 0;          // modifier for Reingold-Tilford
        double prelim = 0;       // preliminary x position
        Node parent;
        Node leftNeighbor;
        Node rightNeighbor;
        List<Node> children = new ArrayList<>();
        List<Node> stChildren = new ArrayList<>();
        boolean isAssistant = false;
        boolean isSplit = false;
        boolean isMirror = false;
        int partnerType = 0;
        boolean hasPartners = false;
        double partnerSeparation = 65;
        int layout = 0;
        String lcn = "";
        double[] padding = {0, 0, 0, 0};
        int level = 0;

        Node(String id, double width, double height) {
            this.id = id;
            this.width = width;
            this.height = height;
        }

        double getSize(int orientation) {
            if (orientation == ORIENTATION_TOP ||
                orientation == ORIENTATION_BOTTOM) {
                return width;
            }
            return height;
        }

        double getMidpoint(int orientation) {
            return getSize(orientation) / 2.0;
        }
    }

    // ==================== MAIN CALCULATE METHOD ====================

    /**
     * Asosiy hisoblash metodi
     * @param nodeInputs - barcha nodelar
     * @param rootIds - root node idlari
     * @param layoutConfigs - layout konfiguratsiyasi
     * @return - har bir node uchun pozitsiya
     */
    public Map<String, NodePosition> calculate(
            List<NodeInput> nodeInputs,
            List<String> rootIds,
            Map<String, LayoutConfig> layoutConfigs) {

        // 1. Node map tuzish
        Map<String, Node> nodeMap = new HashMap<>();
        for (NodeInput input : nodeInputs) {
            Node node = new Node(input.id, input.width, input.height);
            node.isAssistant = input.isAssistant;
            node.isSplit = input.isSplit;
            node.isMirror = input.isMirror;
            node.partnerType = input.partnerType;
            node.hasPartners = input.hasPartners;
            node.partnerSeparation = input.partnerSeparation;
            node.layout = input.layout;
            node.lcn = input.lcn != null ? input.lcn : "";
            node.padding = input.padding;
            nodeMap.put(input.id, node);
        }

        // 2. Parent-child bog'liqliklarini o'rnatish
        for (NodeInput input : nodeInputs) {
            Node node = nodeMap.get(input.id);
            if (node == null) continue;

            if (input.parentId != null) {
                Node parent = nodeMap.get(input.parentId);
                if (parent != null) {
                    node.parent = parent;
                    parent.children.add(node);
                }
            }

            if (input.stParentId != null) {
                Node stParent = nodeMap.get(input.stParentId);
                if (stParent != null) {
                    stParent.stChildren.add(node);
                }
            }
        }

        // 3. Root nodelarni topish
        List<Node> roots = new ArrayList<>();
        if (rootIds != null && !rootIds.isEmpty()) {
            for (String rootId : rootIds) {
                Node root = nodeMap.get(rootId);
                if (root != null) roots.add(root);
            }
        } else {
            for (Node node : nodeMap.values()) {
                if (node.parent == null) roots.add(node);
            }
        }

        // 4. Default layout config
        LayoutConfig baseConfig = layoutConfigs != null
                ? layoutConfigs.getOrDefault("base", new LayoutConfig())
                : new LayoutConfig();

        // 5. Har bir root uchun layout hisoblash
        double offsetX = 0;
        Map<String, NodePosition> result = new HashMap<>();

        for (Node root : roots) {
            setLevels(root, 0);
            calculateLayout(root, baseConfig, layoutConfigs);

            // Har bir root uchun offset
            if (baseConfig.orientation == ORIENTATION_TOP ||
                baseConfig.orientation == ORIENTATION_BOTTOM) {

                double minX = getMinX(root);
                shiftTree(root, -minX + offsetX);
                double maxX = getMaxX(root);
                offsetX = maxX + baseConfig.subtreeSeparation;
            } else {
                double minY = getMinY(root);
                shiftTreeY(root, -minY + offsetX);
                double maxY = getMaxY(root);
                offsetX = maxY + baseConfig.subtreeSeparation;
            }

            collectPositions(root, result, baseConfig.orientation);
        }

        // 6. Neighbor o'rnatish
        setNeighbors(roots, baseConfig.orientation);

        return result;
    }

    // ==================== LEVEL SETTING ====================

    private void setLevels(Node node, int level) {
        node.level = level;
        for (Node child : node.children) {
            setLevels(child, level + 1);
        }
        for (Node stChild : node.stChildren) {
            setLevels(stChild, level + 1);
        }
    }

    // ==================== MAIN LAYOUT ALGORITHM ====================

    private void calculateLayout(Node root,
                                  LayoutConfig baseConfig,
                                  Map<String, LayoutConfig> layoutConfigs) {
        // Reingold-Tilford algoritmi asosida
        firstWalk(root, baseConfig, layoutConfigs);
        secondWalk(root, 0, baseConfig);

        // Orientation ga qarab Y ni o'zgartirish
        if (baseConfig.orientation == ORIENTATION_BOTTOM) {
            mirrorY(root);
        } else if (baseConfig.orientation == ORIENTATION_RIGHT) {
            swapXY(root);
        } else if (baseConfig.orientation == ORIENTATION_LEFT) {
            swapXY(root);
            mirrorX(root);
        }
    }

    // ==================== FIRST WALK (bottom-up) ====================

    private void firstWalk(Node node,
                           LayoutConfig config,
                           Map<String, LayoutConfig> layoutConfigs) {

        LayoutConfig effectiveConfig = getEffectiveConfig(
                node, config, layoutConfigs);

        if (node.children.isEmpty()) {
            // Leaf node
            node.prelim = 0;
            if (node.leftNeighbor != null) {
                node.prelim = node.leftNeighbor.prelim
                        + nodeSize(node.leftNeighbor, effectiveConfig)
                        + effectiveConfig.siblingSeparation;
            }
        } else {
            // Internal node
            Node leftmost = null;
            double step = getLayoutStep(node, effectiveConfig);

            for (int i = 0; i < node.children.size(); i++) {
                Node child = node.children.get(i);

                // Left neighbor o'rnatish
                if (i > 0) {
                    child.leftNeighbor = node.children.get(i - 1);
                    node.children.get(i - 1).rightNeighbor = child;
                }

                firstWalk(child, effectiveConfig, layoutConfigs);

                if (leftmost == null) leftmost = child;
            }

            // Partner va assistant handling
            handlePartnersAndAssistants(node, effectiveConfig);

            // Node markazini hisoblash
            double midpoint = getMidpointOfChildren(
                    node, effectiveConfig);

            if (node.leftNeighbor != null) {
                node.prelim = node.leftNeighbor.prelim
                        + nodeSize(node.leftNeighbor, effectiveConfig)
                        + effectiveConfig.siblingSeparation;
                node.mod = node.prelim - midpoint;
            } else {
                node.prelim = midpoint;
            }
        }
    }

    // ==================== SECOND WALK (top-down) ====================

    private void secondWalk(Node node, double modSum,
                            LayoutConfig config) {
        node.x = node.prelim + modSum;
        node.y = node.level * (getMaxNodeHeight(node)
                + config.levelSeparation);

        double newModSum = modSum + node.mod;
        for (Node child : node.children) {
            secondWalk(child, newModSum, config);
        }

        // stChildren uchun
        for (Node stChild : node.stChildren) {
            secondWalk(stChild, newModSum, config);
        }
    }

    // ==================== HELPER METHODS ====================

    private LayoutConfig getEffectiveConfig(
            Node node,
            LayoutConfig defaultConfig,
            Map<String, LayoutConfig> layoutConfigs) {

        if (layoutConfigs != null && node.lcn != null
                && !node.lcn.isEmpty()) {
            LayoutConfig lc = layoutConfigs.get(node.lcn);
            if (lc != null) return lc;
        }
        return defaultConfig;
    }

    private double nodeSize(Node node, LayoutConfig config) {
        if (config.orientation == ORIENTATION_TOP ||
            config.orientation == ORIENTATION_BOTTOM) {
            return node.width;
        }
        return node.height;
    }

    private double getLayoutStep(Node node, LayoutConfig config) {
        if (node.layout == LAYOUT_MIXED) {
            return config.mixedHierarchyNodesSeparation;
        }
        return config.levelSeparation;
    }

    private double getMidpointOfChildren(Node node,
                                          LayoutConfig config) {
        if (node.children.isEmpty()) return 0;

        Node firstChild = node.children.get(0);
        Node lastChild = node.children.get(
                node.children.size() - 1);

        double firstX = firstChild.prelim;
        double lastX = lastChild.prelim + nodeSize(lastChild, config);

        return (firstX + lastX) / 2.0 - nodeSize(node, config) / 2.0;
    }

    private double getMaxNodeHeight(Node node) {
        // Bir leveldagi maksimal height
        return node.height;
    }

    private void handlePartnersAndAssistants(Node node,
                                              LayoutConfig config) {
        if (!node.hasPartners) return;

        double partnerOffset = node.partnerSeparation;

        for (Node child : node.children) {
            if (child.partnerType == 1) {
                // Right partner
                child.prelim = node.prelim
                        + nodeSize(node, config)
                        + partnerOffset;
            } else if (child.partnerType == 2) {
                // Left partner
                child.prelim = node.prelim
                        - nodeSize(child, config)
                        - partnerOffset;
            } else if (child.isAssistant) {
                // Assistant - yuqorida turadigan maxsus node
                child.prelim = node.prelim
                        + nodeSize(node, config) / 2.0
                        - nodeSize(child, config) / 2.0;
                child.y = node.y
                        + config.assistantSeparation;
            }
        }
    }

    // ==================== TREE SHIFTING ====================

    private void shiftTree(Node node, double shift) {
        node.x += shift;
        for (Node child : node.children) {
            shiftTree(child, shift);
        }
        for (Node stChild : node.stChildren) {
            shiftTree(stChild, shift);
        }
    }

    private void shiftTreeY(Node node, double shift) {
        node.y += shift;
        for (Node child : node.children) {
            shiftTreeY(child, shift);
        }
        for (Node stChild : node.stChildren) {
            shiftTreeY(stChild, shift);
        }
    }

    // ==================== ORIENTATION TRANSFORMS ====================

    private void mirrorY(Node node) {
        node.y = -node.y - node.height;
        for (Node child : node.children) mirrorY(child);
        for (Node stChild : node.stChildren) mirrorY(stChild);
    }

    private void mirrorX(Node node) {
        node.x = -node.x - node.width;
        for (Node child : node.children) mirrorX(child);
        for (Node stChild : node.stChildren) mirrorX(stChild);
    }

    private void swapXY(Node node) {
        double temp = node.x;
        node.x = node.y;
        node.y = temp;

        temp = node.width;
        node.width = node.height;
        node.height = temp;

        for (Node child : node.children) swapXY(child);
        for (Node stChild : node.stChildren) swapXY(stChild);
    }

    // ==================== BOUNDS ====================

    private double getMinX(Node node) {
        double min = node.x;
        for (Node child : node.children) {
            min = Math.min(min, getMinX(child));
        }
        return min;
    }

    private double getMaxX(Node node) {
        double max = node.x + node.width;
        for (Node child : node.children) {
            max = Math.max(max, getMaxX(child));
        }
        return max;
    }

    private double getMinY(Node node) {
        double min = node.y;
        for (Node child : node.children) {
            min = Math.min(min, getMinY(child));
        }
        return min;
    }

    private double getMaxY(Node node) {
        double max = node.y + node.height;
        for (Node child : node.children) {
            max = Math.max(max, getMaxY(child));
        }
        return max;
    }

    // ==================== COLLECT RESULTS ====================

    private void collectPositions(Node node,
                                   Map<String, NodePosition> result,
                                   int orientation) {
        NodePosition pos = new NodePosition(
                node.id, node.x, node.y,
                node.width, node.height);

        if (node.leftNeighbor != null) {
            pos.leftNeighborId = node.leftNeighbor.id;
        }
        if (node.rightNeighbor != null) {
            pos.rightNeighborId = node.rightNeighbor.id;
        }

        result.put(node.id, pos);

        for (Node child : node.children) {
            collectPositions(child, result, orientation);
        }
        for (Node stChild : node.stChildren) {
            collectPositions(stChild, result, orientation);
        }
    }

    // ==================== NEIGHBOR SETTING ====================

    private void setNeighbors(List<Node> roots, int orientation) {
        Map<Integer, List<Node>> levelMap = new HashMap<>();
        for (Node root : roots) {
            collectByLevel(root, 0, levelMap);
        }

        for (List<Node> levelNodes : levelMap.values()) {
            // X ga qarab sort
            levelNodes.sort((a, b) -> Double.compare(a.x, b.x));

            for (int i = 0; i < levelNodes.size(); i++) {
                if (i > 0) {
                    levelNodes.get(i).leftNeighbor
                            = levelNodes.get(i - 1);
                }
                if (i < levelNodes.size() - 1) {
                    levelNodes.get(i).rightNeighbor
                            = levelNodes.get(i + 1);
                }
            }
        }
    }

    private void collectByLevel(Node node, int level,
                                 Map<Integer, List<Node>> levelMap) {
        levelMap.computeIfAbsent(level, k -> new ArrayList<>())
                .add(node);
        for (Node child : node.children) {
            collectByLevel(child, level + 1, levelMap);
        }
    }

    // ==================== JSON OUTPUT (OrgChart formatda) ====================

    /**
     * OrgChart.js ga mos JSON formatga o'girish
     * Result format: {"nodeId": {"p": [x, y, w, h], "ln": "...", "rn": "..."}}
     */
    public Map<String, Object> toOrgChartFormat(
            Map<String, NodePosition> positions) {

        Map<String, Object> result = new HashMap<>();

        for (Map.Entry<String, NodePosition> entry
                : positions.entrySet()) {

            NodePosition pos = entry.getValue();
            Map<String, Object> nodeResult = new HashMap<>();

            // p = [x, y, width, height]
            nodeResult.put("p", new double[]{
                    pos.x, pos.y, pos.width, pos.height
            });

            if (pos.leftNeighborId != null) {
                nodeResult.put("ln", pos.leftNeighborId);
            }
            if (pos.rightNeighborId != null) {
                nodeResult.put("rn", pos.rightNeighborId);
            }

            result.put(entry.getKey(), nodeResult);
        }

        return result;
    }

    // ==================== TEST ====================

    public static void main(String[] args) {
        OrgChartLayoutCalculator calc
                = new OrgChartLayoutCalculator();

        // Test nodelar
        List<NodeInput> nodes = new ArrayList<>();

        NodeInput ceo = new NodeInput();
        ceo.id = "1";
        ceo.width = 250;
        ceo.height = 120;
        nodes.add(ceo);

        NodeInput vp1 = new NodeInput();
        vp1.id = "2";
        vp1.parentId = "1";
        vp1.width = 250;
        vp1.height = 120;
        nodes.add(vp1);

        NodeInput vp2 = new NodeInput();
        vp2.id = "3";
        vp2.parentId = "1";
        vp2.width = 250;
        vp2.height = 120;
        nodes.add(vp2);

        NodeInput emp1 = new NodeInput();
        emp1.id = "4";
        emp1.parentId = "2";
        emp1.width = 250;
        emp1.height = 120;
        nodes.add(emp1);

        // Layout config
        Map<String, LayoutConfig> configs = new HashMap<>();
        LayoutConfig base = new LayoutConfig();
        base.orientation = ORIENTATION_TOP;
        base.levelSeparation = 60;
        base.siblingSeparation = 20;
        base.subtreeSeparation = 40;
        configs.put("base", base);

        // Hisoblash
        List<String> rootIds = Arrays.asList("1");
        Map<String, NodePosition> positions
                = calc.calculate(nodes, rootIds, configs);

        // Natija
        for (Map.Entry<String, NodePosition> entry
                : positions.entrySet()) {
            NodePosition pos = entry.getValue();
            System.out.printf(
                "Node %s: x=%.1f, y=%.1f, w=%.1f, h=%.1f%n",
                entry.getKey(),
                pos.x, pos.y, pos.width, pos.height
            );
        }

        // OrgChart formatda
        calc.toOrgChartFormat(positions);
    }
}
