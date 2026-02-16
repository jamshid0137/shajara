package com.example.shajara.controller;


import com.example.shajara.dto.FamilyTreeDto;
import com.example.shajara.dto.person.PersonSimpleDto;
import com.example.shajara.service.FamilyTreeService;
import com.example.shajara.service.PersonService;
import com.example.shajara.util.SpringSecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/family-trees")
@RequiredArgsConstructor
public class FamilyTreeController {

    private final FamilyTreeService service;
    private final PersonService personService;


    @PostMapping
    public ResponseEntity<FamilyTreeDto> create(@RequestBody FamilyTreeDto dto) {
        dto.setProfileId(SpringSecurityUtil.getCurrentUserId());
        return ResponseEntity.ok(service.create(dto));
    }

    @GetMapping
    public ResponseEntity<List<FamilyTreeDto>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FamilyTreeDto> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FamilyTreeDto> update(@PathVariable Long id, @RequestParam String name) {
        return ResponseEntity.ok(service.update(id, name));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/{treeId}/persons")
    public List<PersonSimpleDto> getTreePersons(@PathVariable Long treeId) {
        return personService.getTreePersons(treeId);
    }

}