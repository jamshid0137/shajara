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
        return ResponseEntity.ok(treeLayoutService.calculateLayout(personId));
    }

    /**
     * Tree ID bo'yicha layout — FamilyTree.lastPersonId ni markaz qilib ishlatadi.
     * GET /api/layout/tree/{treeId}
     */
    @GetMapping("/tree/{treeId}")
    public ResponseEntity<TreeLayoutResponseDto> getByTree(@PathVariable Long treeId) {
        FamilyTree tree = familyTreeRepository.findById(treeId)
                .orElseThrow(() -> new NotFoundException("FamilyTree topilmadi: " + treeId));

        Long lastPersonId = tree.getLastPersonId();
        if (lastPersonId == null) {
            throw new NotFoundException("Bu daraxtda lastPersonId yo'q. Avval biror personni oching.");
        }

        return ResponseEntity.ok(treeLayoutService.calculateLayout(lastPersonId));
    }
}
