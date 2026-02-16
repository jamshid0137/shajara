package com.example.shajara.service;


import com.example.shajara.dto.relation.RelationCreateDto;
import com.example.shajara.dto.relation.RelationResponseDto;
import com.example.shajara.entity.Person;

import java.util.List;

public interface RelationService {

    RelationResponseDto create(RelationCreateDto dto);

    List<RelationResponseDto> getAll();

    RelationResponseDto getById(Long id);

    RelationResponseDto update(Long id, RelationCreateDto dto);

    void delete(Long id);

    List<RelationResponseDto> getByPerson(Long personId);

    List<RelationResponseDto> getByTree(Long treeId);
    void updateDivorceStatus(Long personId1, Long personId2, boolean divorced);

    List<Person> findAllSpousesNative(Long personId);

    List<Long> findForAllSpousesNativeRelationdIds(Long personId);

}
