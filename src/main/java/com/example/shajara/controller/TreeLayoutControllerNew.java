package com.example.shajara.controller;

import com.example.shajara.dto.layoutnew.TreeLayoutResponseDtoNew;
import com.example.shajara.entity.FamilyTree;
import com.example.shajara.exception.NotFoundException;
import com.example.shajara.repository.FamilyTreeRepository;
import com.example.shajara.service.TreeLayoutServiceNew;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/layoutnew")
@RequiredArgsConstructor
public class TreeLayoutControllerNew {

    private final TreeLayoutServiceNew TreeLayoutServiceNew;
    private final FamilyTreeRepository familyTreeRepository;

    /**
     * Person ID bo'yicha layout.
     * GET /api/layout/person/{personId}
     */
    @GetMapping("/person/{personId}")
    public ResponseEntity<TreeLayoutResponseDtoNew> getByPerson(@PathVariable Long personId) {
        TreeLayoutResponseDtoNew layout = TreeLayoutServiceNew.calculateLayout(personId);

        // lastPersonId ni yangilaymiz вЂ” keyingi safar sahifa ochilganda shu shaxs
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
     * Tree ID bo'yicha layout вЂ” FamilyTree.lastPersonId ni markaz qilib ishlatadi.
     * GET /api/layout/tree/{treeId}
     */
    @GetMapping("/tree/{treeId}")
    public ResponseEntity<TreeLayoutResponseDtoNew> getByTree(@PathVariable Long treeId) {
        familyTreeRepository.findById(treeId)
                .orElseThrow(() -> new NotFoundException("FamilyTree topilmadi: " + treeId));

        // Yangi mantiq: Berilgan daraxtga tegishli barcha personlarni qaytarish
        return ResponseEntity.ok(TreeLayoutServiceNew.calculateFullTreeLayout(treeId));
    }
}

