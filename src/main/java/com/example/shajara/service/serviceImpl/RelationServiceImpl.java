package com.example.shajara.service.serviceImpl;


import com.example.shajara.dto.relation.RelationCreateDto;
import com.example.shajara.dto.relation.RelationResponseDto;
import com.example.shajara.entity.Person;
import com.example.shajara.entity.Relation;
import com.example.shajara.enums.RelationType;
import com.example.shajara.repository.PersonRepository;
import com.example.shajara.repository.RelationRepository;
import com.example.shajara.service.RelationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RelationServiceImpl implements RelationService {

    private final RelationRepository relationRepository;
    private final PersonRepository personRepository;

    // ================= CREATE =================
    @Override
    public RelationResponseDto create(RelationCreateDto dto) {
        Person from = getPerson(dto.getFromPersonId());
        Person to = getPerson(dto.getToPersonId());

        if(!from.getFamilyTree().getId().equals(to.getFamilyTree().getId())){
            throw new RuntimeException("These people don't belong to one family tree !");
        }

        if(from.getGender()==to.getGender()){
            throw new RuntimeException("The same genders cannot marry !");
        }
        if(dto.getType() != RelationType.SPOUSE){
            throw new RuntimeException("Only SPOUSE relation allowed");
        }
        boolean qarindosh=qarindoshmasmi(from,to);
        if(qarindosh){
            throw new RuntimeException("These people are relative with each !");
        }

        Relation relation = Relation.builder()
                .fromPerson(from)
                .toPerson(to)
                .type(RelationType.SPOUSE)
                .build();

        return toDto(relationRepository.save(relation));
    }

    // ================= UPDATE =================
    @Override
    public RelationResponseDto update(Long id, RelationCreateDto dto) {
        Relation relation = relationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Relation not found"));

        Person from = getPerson(dto.getFromPersonId());
        Person to = getPerson(dto.getToPersonId());
        if(qarindoshmasmi(from,to)){
            throw new RuntimeException("These people already are relative !");
        }

        if(dto.getType() != RelationType.SPOUSE){
            throw new RuntimeException("Only SPOUSE relation allowed");
        }

        relation.setFromPerson(from);
        relation.setToPerson(to);
        relation.setType(RelationType.SPOUSE);

        return toDto(relationRepository.save(relation));
    }

    // ================= DELETE =================
    @Override
    public void delete(Long id) {
        relationRepository.deleteById(id);
    }

    // ================= GET =================
    @Override
    public List<RelationResponseDto> getAll() {
        return relationRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public RelationResponseDto getById(Long id) {
        return toDto(relationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Relation not found")));
    }

    @Override
    public List<RelationResponseDto> getByPerson(Long personId) {
        return relationRepository.findAllByFromPersonId(personId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public List<RelationResponseDto> getByTree(Long treeId) {
        return relationRepository.findAllByTreeId(treeId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    // ================= DIVORCE =================
    @Transactional
    @Override
    public void updateDivorceStatus(Long personId1, Long personId2, boolean divorced) {
        List<Relation> relations =
                relationRepository.findSpouseRelationsBetween(personId1, personId2, RelationType.SPOUSE);

        if(relations.isEmpty()){
            throw new RuntimeException("Spouse relation not found between persons");
        }

        relations.forEach(r -> r.setDivorced(divorced));
        relationRepository.saveAll(relations);
    }

    // ================= HELPERS =================
    private Person getPerson(Long id){
        return personRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Person not found with id: " + id));
    }

    private RelationResponseDto toDto(Relation r){
        return RelationResponseDto.builder()
                .id(r.getId())
                .fromPersonId(r.getFromPerson().getId())
                .toPersonId(r.getToPerson().getId())
                .type(r.getType())
                .build();
    }


    public boolean qarindoshmasmi(Person from, Person to) {
        //  Spouse tekshiruvlari
        List<Person> fromSpouses = relationRepository.findAllSpousesNative(from.getId());
        for (Person p : fromSpouses) {
            if (p.getId().equals(to.getId())) return true;
        }

        //  Parent-Child tekshiruvlari
        if ((from.getFatherId() != null && from.getFatherId().equals(to.getId())) ||
                (from.getMotherId() != null && from.getMotherId().equals(to.getId()))) {
            return true;
        }

        if ((to.getFatherId() != null && to.getFatherId().equals(from.getId())) ||
                (to.getMotherId() != null && to.getMotherId().equals(from.getId()))) {
            return true;
        }

        return false;
    }

    @Override
    public List<Person> findAllSpousesNative(Long personId){
        return relationRepository.findAllSpousesNative(personId);
    }

    @Override
    public List<Long> findForAllSpousesNativeRelationdIds(Long personId){
        return relationRepository.findForAllSpousesNativeRelationdIds(personId);
    }
}
