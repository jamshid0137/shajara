package com.example.shajara.dto.layout1new;

import java.util.List;
import java.util.Map;

public class LayoutRequestNew {

    private List<OrgChartLayoutCalculatorNew.NodeInput> nodes;
    private List<String> roots;
    private Map<String, OrgChartLayoutCalculatorNew.LayoutConfig> configs;

    public List<OrgChartLayoutCalculatorNew.NodeInput> getNodes() {
        return nodes;
    }

    public void setNodes(List<OrgChartLayoutCalculatorNew.NodeInput> nodes) {
        this.nodes = nodes;
    }

    public List<String> getRoots() {
        return roots;
    }

    public void setRoots(List<String> roots) {
        this.roots = roots;
    }

    public Map<String, OrgChartLayoutCalculatorNew.LayoutConfig> getConfigs() {
        return configs;
    }

    public void setConfigs(Map<String, OrgChartLayoutCalculatorNew.LayoutConfig> configs) {
        this.configs = configs;
    }
}

