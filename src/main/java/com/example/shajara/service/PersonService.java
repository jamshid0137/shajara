package com.example.shajara.service;

import com.example.shajara.dto.person.PersonAddParentDto;
import com.example.shajara.dto.person.PersonAddFatherDto;
import com.example.shajara.dto.person.PersonAddMotherDto;
import com.example.shajara.dto.person.*;
import com.example.shajara.entity.Person;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PersonService {
    PersonAddChildResponseDto addChild(PersonAddChildDto dto);
    //PersonAddSpouseDto addSpouse2(PersonAddSpouseCreateDto id);

    PersonAddSpouseDto addSpouse(PersonAddSpouseCreateDto dto);
    PersonAddParentDto addParents(PersonAddParentDto dto);

    /** Faqat ota qo'shish — childa fatherId bo'lmasa yangi MALE person yaratadi */
    PersonAddParentDto addFather(PersonAddFatherDto dto);

    /** Faqat ona qo'shish — childa motherId bo'lmasa yangi FEMALE person yaratadi */
    PersonAddParentDto addMother(PersonAddMotherDto dto);

    PersonResponseDto create(PersonCreateDto dto);

    List<PersonResponseDto> getAllByTree(Long treeId);

    PersonResponseDto getById(Long id);

    PersonResponseDto update(Long id, PersonUpdateDto dto);

    void delete(Long id);


    PersonResponseFullDto getPersonWithRelations(Long personId);

    List<PersonSimpleDto> getTreePersons(Long treeId);

    PersonResponseDto addParent(Long childId, Long parentId);


    PersonResponseDto updatePhoto(Long personId, MultipartFile photo);

    //String getPersonPhotoSignedUrl(Person person);
    Person find(Long id);
}
