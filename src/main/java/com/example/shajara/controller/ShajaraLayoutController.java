package com.example.shajara.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shajara")
@RequiredArgsConstructor
public class ShajaraLayoutController {

    private static final Logger log = LoggerFactory.getLogger(ShajaraLayoutController.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    @PostMapping("/save-layout")
    public Map<String, String> saveLayout(@RequestBody List<Map<String, Object>> positions, @RequestParam(required = false) String treeId) {
        try {
            String filename = "layout_" + (treeId != null ? treeId : "default") + ".json";
            String path = "d:\\загрузки\\shajara\\" + filename;
            
            String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(positions);
            Files.writeString(Paths.get(path), json);
            
            log.info("Saved layout for tree {} to {}", treeId, path);
            return Map.of("status", "ok", "message", "Layout saved to " + filename);
        } catch (Exception e) {
            log.error("Failed to save layout: {}", e.getMessage());
            return Map.of("status", "error", "message", e.getMessage());
        }
    }
}
