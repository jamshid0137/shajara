package com.example.shajara.service;


import com.example.shajara.dto.FamilyTreeDto;

import java.util.List;

public interface FamilyTreeService {

    FamilyTreeDto create(FamilyTreeDto dto);

    List<FamilyTreeDto> getAll();

    FamilyTreeDto getById(Long id);

    FamilyTreeDto update(Long id, String name);

    void delete(Long id);

    List<FamilyTreeDto> getAllByProfileId(Integer profileId);
}
