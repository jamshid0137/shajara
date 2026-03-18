package com.example.shajara.dto.layout1;

import java.util.List;
import java.util.Map;

public class LayoutRequest {

    private List<OrgChartLayoutCalculator.NodeInput> nodes;
    private List<String> roots;
    private Map<String, OrgChartLayoutCalculator.LayoutConfig> configs;

    public List<OrgChartLayoutCalculator.NodeInput> getNodes() {
        return nodes;
    }

    public void setNodes(List<OrgChartLayoutCalculator.NodeInput> nodes) {
        this.nodes = nodes;
    }

    public List<String> getRoots() {
        return roots;
    }

    public void setRoots(List<String> roots) {
        this.roots = roots;
    }

    public Map<String, OrgChartLayoutCalculator.LayoutConfig> getConfigs() {
        return configs;
    }

    public void setConfigs(Map<String, OrgChartLayoutCalculator.LayoutConfig> configs) {
        this.configs = configs;
    }
}
