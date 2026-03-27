package com.example.shajara.controller;

import com.example.shajara.dto.layout1.OrgChartLayoutCalculator;
import com.example.shajara.dto.layout1.OrgChartLayoutCalculator.NodeInput;
import com.example.shajara.dto.layout1.OrgChartLayoutCalculator.LayoutConfig;
import com.example.shajara.dto.layout1.OrgChartLayoutCalculator.NodePosition;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * OrgChart.js layout hisoblash endpointi
 *
 * OrgChart.js _setPositions() quyidagi compact formatda yuboradi:
 * {
 * n: [ { p:[id, parentId, stParentId, w, h], c:[childIds], v:[stChildIds], l,
 * a, s, im, i, g, e, q, k, b } ],
 * r: [rootId1, rootId2, ...],
 * c: { "base": [orientation, levelSep, mixedSep, subtreeSep, siblingSep,
 * layout, cols, collapse, assistantSep, partnerSep] },
 * v: "version"
 * }
 *
 * Response (OrgChart.remote._fromResDTO kutadi):
 * { nodeId: { p:[x,y,w,h], ln: leftNeighborId, rn: rightNeighborId }, ... }
 */
@RestController
@RequestMapping("/api/layout1")
@RequiredArgsConstructor
public class LayoutController {

    private static final Logger log = LoggerFactory.getLogger(LayoutController.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final OrgChartLayoutCalculator calculator;

    @PostMapping("/calculate")
    public Map<String, Object> calculate(@RequestBody Map<String, Object> body) {

        // ── REQUEST LOG ────────────────────────────────────────────────
        try {
            String bodyJson = mapper.writeValueAsString(body);
            java.nio.file.Files.writeString(
                java.nio.file.Paths.get("d:\\загрузки\\shajara\\last_req.json"),
                bodyJson
            );
            log.info("=== /api/layout1/calculate REQUEST ===");
            log.info("Keys: {}", body.keySet());
            // n (nodes)
            Object nRaw = body.get("n");
            if (nRaw instanceof List) {
                log.info("n (nodes) count: {}", ((List<?>) nRaw).size());
                if (!((List<?>) nRaw).isEmpty()) {
                    log.info("n[0] (first node): {}", mapper.writeValueAsString(((List<?>) nRaw).get(0)));
                }
            } else {
                log.warn("n field is not a List, it is: {}", nRaw == null ? "null" : nRaw.getClass());
            }
            // r (roots)
            log.info("r (roots): {}", body.get("r"));
            // c (configs)
            log.info("c (configs): {}", mapper.writeValueAsString(body.get("c")));
        } catch (Exception e) {
            log.error("LOG xatosi: {}", e.getMessage());
        }

        // ── 1. Node listini parse qilish ──────────────────────────────
        List<NodeInput> nodeInputs = parseNodes(body);
        log.info("Parsed nodeInputs count: {}", nodeInputs.size());
        if (!nodeInputs.isEmpty()) {
            NodeInput first = nodeInputs.get(0);
            log.info("First nodeInput: id={}, parentId={}, w={}, h={}, children={}",
                    first.id, first.parentId, first.width, first.height, first.childrenIds);
        }

        // ── 2. Root IDlarni parse qilish ─────────────────────────────
        List<String> rootIds = parseRoots(body);
        log.info("Parsed rootIds: {}", rootIds);

        // ── 3. Layout configlarni parse qilish ───────────────────────
        Map<String, LayoutConfig> configs = parseConfigs(body);
        log.info("Parsed configs keys: {}", configs.keySet());
        LayoutConfig base = configs.get("base");
        if (base != null) {
            log.info("base config: orientation={}, levelSep={}, siblingSep={}, subtreeSep={}",
                    base.orientation, base.levelSeparation, base.siblingSeparation, base.subtreeSeparation);
        }

        // ── 4. Layout hisoblash ──────────────────────────────────────
        Map<String, NodePosition> positions = calculator.calculate(
                nodeInputs, rootIds, configs);
        log.info("Calculated positions count: {}", positions.size());

        // ── 5. OrgChart formatga o'girish ─────────────────────────────
        Map<String, Object> response = calculator.toOrgChartFormat(positions);
        try {
            log.info("Response sample (first 2): {}",
                    mapper.writeValueAsString(response.entrySet().stream().limit(2).collect(
                            java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))));
        } catch (Exception ignored) {
        }
        log.info("=== /api/layout1/calculate DONE, returning {} nodes ===", response.size());

        return response;
    }

    // ==================== PARSERS ====================

    @SuppressWarnings("unchecked")
    private List<NodeInput> parseNodes(Map<String, Object> body) {
        List<NodeInput> result = new ArrayList<>();

        Object nRaw = body.get("n");
        if (!(nRaw instanceof List))
            return result;

        List<Object> nList = (List<Object>) nRaw;
        for (Object item : nList) {
            if (!(item instanceof Map))
                continue;
            Map<String, Object> nodeMap = (Map<String, Object>) item;

            NodeInput ni = new NodeInput();

            // p = [id, parentId, stParentId, w, h]
            List<Object> p = (List<Object>) nodeMap.get("p");
            if (p == null || p.size() < 5)
                continue;

            ni.id = String.valueOf(p.get(0));
            ni.parentId = p.get(1) != null ? String.valueOf(p.get(1)) : null;
            ni.stParentId = p.get(2) != null ? String.valueOf(p.get(2)) : null;
            ni.width = toDouble(p.get(3), 250.0);
            ni.height = toDouble(p.get(4), 120.0);

            // parentId "null" string bo'lsa null qilamiz
            if ("null".equals(ni.parentId))
                ni.parentId = null;
            if ("null".equals(ni.stParentId))
                ni.stParentId = null;

            // c = children IDs
            List<Object> c = (List<Object>) nodeMap.get("c");
            if (c != null) {
                for (Object cid : c)
                    ni.childrenIds.add(String.valueOf(cid));
            }

            // v = stChildren IDs
            List<Object> v = (List<Object>) nodeMap.get("v");
            if (v != null) {
                for (Object vid : v)
                    ni.stChildrenIds.add(String.valueOf(vid));
            }

            // l = layout type
            if (nodeMap.containsKey("l")) {
                ni.layout = toInt(nodeMap.get("l"), 0);
            }

            // a = isAssistant
            if (nodeMap.containsKey("a")) {
                ni.isAssistant = toInt(nodeMap.get("a"), 0) == 1;
            }

            // s = isSplit
            if (nodeMap.containsKey("s")) {
                ni.isSplit = toBool(nodeMap.get("s"));
            }

            // im = isMirror
            if (nodeMap.containsKey("im")) {
                ni.isMirror = toBool(nodeMap.get("im"));
            }

            // i = isPartner
            if (nodeMap.containsKey("i")) {
                ni.isPartner = toBool(nodeMap.get("i"));
                ni.partnerType = toBool(nodeMap.get("i")) ? 1 : 0;
            }

            // g = hasPartners
            if (nodeMap.containsKey("g")) {
                ni.hasPartners = toBool(nodeMap.get("g"));
            }

            // e = partnerSeparation
            if (nodeMap.containsKey("e")) {
                ni.partnerSeparation = toDouble(nodeMap.get("e"), 65.0);
            }

            // k = lcn (layout config name)
            if (nodeMap.containsKey("k")) {
                ni.lcn = String.valueOf(nodeMap.get("k"));
            }

            result.add(ni);
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private List<String> parseRoots(Map<String, Object> body) {
        List<String> result = new ArrayList<>();
        Object rRaw = body.get("r");
        if (!(rRaw instanceof List))
            return result;

        for (Object rid : (List<Object>) rRaw) {
            result.add(String.valueOf(rid));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, LayoutConfig> parseConfigs(Map<String, Object> body) {
        Map<String, LayoutConfig> result = new HashMap<>();

        Object cRaw = body.get("c");
        if (!(cRaw instanceof Map))
            return result;

        Map<String, Object> cMap = (Map<String, Object>) cRaw;
        for (Map.Entry<String, Object> entry : cMap.entrySet()) {
            String key = entry.getKey();
            Object val = entry.getValue();

            LayoutConfig lc = new LayoutConfig();

            if (val instanceof List) {
                // Array format: [orientation, levelSep, mixedSep, subtreeSep, siblingSep,
                // layout, cols, collapse, assistantSep, partnerSep]
                List<Object> arr = (List<Object>) val;
                if (arr.size() > 0)
                    lc.orientation = toInt(arr.get(0), 0);
                if (arr.size() > 1)
                    lc.levelSeparation = toDouble(arr.get(1), 60.0);
                if (arr.size() > 2)
                    lc.mixedHierarchyNodesSeparation = toDouble(arr.get(2), 15.0);
                if (arr.size() > 3)
                    lc.subtreeSeparation = toDouble(arr.get(3), 40.0);
                if (arr.size() > 4)
                    lc.siblingSeparation = toDouble(arr.get(4), 20.0);
                if (arr.size() > 5)
                    lc.layout = toInt(arr.get(5), 0);
                if (arr.size() > 6)
                    lc.columns = toInt(arr.get(6), 10);
                // arr[7] = collapse (skip)
                if (arr.size() > 8)
                    lc.assistantSeparation = toDouble(arr.get(8), 100.0);
                if (arr.size() > 9)
                    lc.partnerNodeSeparation = toDouble(arr.get(9), 15.0);

            } else if (val instanceof Map) {
                // Object format (legacy)
                Map<String, Object> m = (Map<String, Object>) val;
                if (m.containsKey("orientation"))
                    lc.orientation = toInt(m.get("orientation"), 0);
                if (m.containsKey("levelSeparation"))
                    lc.levelSeparation = toDouble(m.get("levelSeparation"), 60.0);
                if (m.containsKey("subtreeSeparation"))
                    lc.subtreeSeparation = toDouble(m.get("subtreeSeparation"), 40.0);
                if (m.containsKey("siblingSeparation"))
                    lc.siblingSeparation = toDouble(m.get("siblingSeparation"), 20.0);
                if (m.containsKey("assistantSeparation"))
                    lc.assistantSeparation = toDouble(m.get("assistantSeparation"), 100.0);
                if (m.containsKey("partnerNodeSeparation"))
                    lc.partnerNodeSeparation = toDouble(m.get("partnerNodeSeparation"), 15.0);
            }

            result.put(key, lc);
        }

        // Agar "base" yo'q bo'lsa — default qo'shamiz
        result.putIfAbsent("base", new LayoutConfig());
        return result;
    }

    // ==================== CONVERTERS ====================

    private double toDouble(Object val, double def) {
        if (val == null)
            return def;
        try {
            return ((Number) val).doubleValue();
        } catch (Exception e) {
            return def;
        }
    }

    private int toInt(Object val, int def) {
        if (val == null)
            return def;
        try {
            return ((Number) val).intValue();
        } catch (Exception e) {
            return def;
        }
    }

    private boolean toBool(Object val) {
        if (val == null)
            return false;
        if (val instanceof Boolean)
            return (Boolean) val;
        if (val instanceof Number)
            return ((Number) val).intValue() != 0;
        return false;
    }
}
