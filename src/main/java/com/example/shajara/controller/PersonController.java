package com.example.shajara.controller;

import com.example.shajara.dto.person.PersonCreateDto;
import com.example.shajara.dto.person.PersonResponseDto;
import com.example.shajara.dto.person.PersonResponseFullDto;
import com.example.shajara.dto.person.PersonUpdateDto;
import com.example.shajara.service.PersonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/persons")
@RequiredArgsConstructor
public class PersonController {

    private final PersonService personService;

    @PostMapping
    public ResponseEntity<PersonResponseDto> create(@RequestBody @Valid PersonCreateDto dto) {
        return ResponseEntity.ok(personService.create(dto));
    }

    @GetMapping("/tree/{treeId}")
    public ResponseEntity<List<PersonResponseDto>> getByTree(@PathVariable Long treeId) {
        return ResponseEntity.ok(personService.getAllByTree(treeId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PersonResponseDto> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(personService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PersonResponseDto> update(
            @PathVariable Long id,
            @RequestBody @Valid PersonUpdateDto dto) {

        return ResponseEntity.ok(personService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        personService.delete(id);
        return ResponseEntity.noContent().build();
    }



    @GetMapping("/{personId}/full")
    public PersonResponseFullDto getPersonWithRelations(@PathVariable Long personId) {
        return personService.getPersonWithRelations(personId);
    }


    @PostMapping("/{childId}/add-parent")
    public ResponseEntity<PersonResponseDto> addParent(
            @PathVariable Long childId,
            @RequestParam Long parentId
    ) {
        PersonResponseDto updatedChild = personService.addParent(childId, parentId);
        return ResponseEntity.ok(updatedChild);
    }

}

