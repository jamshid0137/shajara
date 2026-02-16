package com.example.shajara.service;

import com.example.shajara.dto.person.*;

import java.util.List;

public interface PersonService {

    PersonResponseDto create(PersonCreateDto dto);

    List<PersonResponseDto> getAllByTree(Long treeId);

    PersonResponseDto getById(Long id);

    PersonResponseDto update(Long id, PersonUpdateDto dto);

    void delete(Long id);


    PersonResponseFullDto getPersonWithRelations(Long personId);

    List<PersonSimpleDto> getTreePersons(Long treeId);

    PersonResponseDto addParent(Long childId, Long parentId);


}
