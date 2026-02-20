package com.example.shajara.controller;

import com.example.shajara.dto.person.*;
import com.example.shajara.service.PersonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/persons")
@RequiredArgsConstructor
public class PersonController {

    private final PersonService personService;

    @PostMapping("/create-person")
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
    public ResponseEntity<PersonAddParentDto> addParent(
            @PathVariable Long childId
    ) {
        //PersonResponseDto updatedChild = personService.addParent(childId, parentId);
        PersonAddParentDto updatedChild=personService.addParents(childId);
        return ResponseEntity.ok(updatedChild);
    }

    @PostMapping("/{id}/add-spouse")
    public ResponseEntity<PersonAddSpouseDto> addSpouse(
            @PathVariable Long id
    ) {
        PersonAddSpouseDto updated=personService.addSpouse(id);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/add-child")
    public ResponseEntity<PersonAddChildResponseDto> addChild(@RequestBody @Valid PersonAddChildDto dto) {
        return ResponseEntity.ok(personService.addChild(dto));
    }


    @PostMapping("/{personId}/photo")
    public ResponseEntity<PersonResponseDto> updatePhoto(
            @PathVariable Long personId,
            @RequestParam("photo") MultipartFile photo
    ) {
        PersonResponseDto updatedPerson = personService.updatePhoto(personId, photo);
        return ResponseEntity.ok(updatedPerson);
    }

//    @GetMapping("/{id}/photo-url")
//    public ResponseEntity<String> getPersonPhoto(@PathVariable Long id) {
//        Person person = personService.find(id); // personService.find
//        String signedUrl = personService.getPersonPhotoSignedUrl(person);
//        return ResponseEntity.ok(signedUrl);
//    }



}

