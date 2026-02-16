package com.example.shajara.controller;


import com.example.shajara.dto.relation.RelationCreateDto;
import com.example.shajara.dto.relation.RelationResponseDto;
import com.example.shajara.service.RelationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/relations")
@RequiredArgsConstructor
public class RelationController {

    private final RelationService service;

    @PostMapping
    public ResponseEntity<RelationResponseDto> create(@RequestBody @Valid RelationCreateDto dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @GetMapping
    public ResponseEntity<List<RelationResponseDto>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RelationResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RelationResponseDto> update(@PathVariable Long id,
                                                      @RequestBody @Valid RelationCreateDto dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/person/{personId}")
    public ResponseEntity<List<RelationResponseDto>> getByPerson(@PathVariable Long personId) {
        return ResponseEntity.ok(service.getByPerson(personId));
    }

    @GetMapping("/tree/{treeId}")
    public ResponseEntity<List<RelationResponseDto>> getByTree(@PathVariable Long treeId) {
        return ResponseEntity.ok(service.getByTree(treeId));
    }

    @PutMapping("/spouse/divorce")      //AJRASHSA O'ZGARTIRISH
    public void updateDivorce(
            @RequestParam Long personId1,
            @RequestParam Long personId2,
            @RequestParam boolean divorced
    ) {
        service.updateDivorceStatus(personId1, personId2, divorced);
    }
}
