package com.example.shajara.controller;

import com.example.shajara.dto.layout.TreeLayoutResponseDto;
import com.example.shajara.entity.FamilyTree;
import com.example.shajara.exception.NotFoundException;
import com.example.shajara.repository.FamilyTreeRepository;
import com.example.shajara.service.TreeLayoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/layout")
@RequiredArgsConstructor
public class TreeLayoutController {

    private final TreeLayoutService treeLayoutService;
    private final FamilyTreeRepository familyTreeRepository;

    /**
     * Person ID bo'yicha layout.
     * GET /api/layout/person/{personId}
     */
    @GetMapping("/person/{personId}")
    public ResponseEntity<TreeLayoutResponseDto> getByPerson(@PathVariable Long personId) {
        TreeLayoutResponseDto layout = treeLayoutService.calculateLayout(personId);

        // lastPersonId ni yangilaymiz — keyingi safar sahifa ochilganda shu shaxs
        // markaz bo'ladi
        layout.getNodes().stream()
                .filter(n -> n.getId().equals(personId))
                .findFirst()
                .ifPresent(centerNode -> {
                    if (centerNode.getTreeId() != null) {
                        familyTreeRepository.findById(centerNode.getTreeId()).ifPresent(tree -> {
                            tree.setLastPersonId(personId);
                            familyTreeRepository.save(tree);
                        });
                    }
                });

        return ResponseEntity.ok(layout);
    }

    /**
     * Tree ID bo'yicha layout — FamilyTree.lastPersonId ni markaz qilib ishlatadi.
     * GET /api/layout/tree/{treeId}
     */
    @GetMapping("/tree/{treeId}")
    public ResponseEntity<TreeLayoutResponseDto> getByTree(@PathVariable Long treeId) {
        familyTreeRepository.findById(treeId)
                .orElseThrow(() -> new NotFoundException("FamilyTree topilmadi: " + treeId));

        // Yangi mantiq: Berilgan daraxtga tegishli barcha personlarni qaytarish
        return ResponseEntity.ok(treeLayoutService.calculateFullTreeLayout(treeId));
    }
}
