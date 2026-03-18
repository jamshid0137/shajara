package com.example.shajara.controller;

import com.example.shajara.dto.layout1.LayoutRequest;
import com.example.shajara.dto.layout1.OrgChartLayoutCalculator;
import com.example.shajara.dto.layout1.OrgChartLayoutCalculator.NodePosition;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/layout1")
@RequiredArgsConstructor
public class LayoutController {

    private final OrgChartLayoutCalculator calculator;

    /**
     * Layout hisoblash
     * POST /api/layout1/calculate
     *
     * Request body:
     * {
     *   "nodes": [...],
     *   "roots": ["1"],
     *   "configs": {
     *     "base": {
     *       "orientation": 0,
     *       "levelSeparation": 60,
     *       "siblingSeparation": 20,
     *       "subtreeSeparation": 40
     *     }
     *   }
     * }
     *
     * Response:
     * {
     *   "nodeId": {"p": [x, y, w, h], "ln": "...", "rn": "..."},
     *   ...
     * }
     */
    @PostMapping("/calculate")
    public Map<String, Object> calculate(
            @RequestBody LayoutRequest request) {

        Map<String, NodePosition> positions =
            calculator.calculate(
                request.getNodes(),
                request.getRoots(),
                request.getConfigs()
            );

        return calculator.toOrgChartFormat(positions);
    }
}
