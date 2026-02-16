package com.example.shajara.service.serviceImpl;


import com.example.shajara.dto.FamilyTreeDto;
import com.example.shajara.dto.person.PersonCreateDto;
import com.example.shajara.dto.person.PersonResponseDto;
import com.example.shajara.entity.FamilyTree;
import com.example.shajara.entity.ProfileEntity;
import com.example.shajara.exception.AppBadException;
import com.example.shajara.repository.FamilyTreeRepository;
import com.example.shajara.repository.ProfileRepository;
import com.example.shajara.service.FamilyTreeService;
import com.example.shajara.service.PersonService;
import com.example.shajara.util.SpringSecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class FamilyTreeServiceImpl implements FamilyTreeService {

    private final FamilyTreeRepository repository;
    private final PersonService personService;

    private final ProfileRepository profileRepository;

    @Override
    public FamilyTreeDto create(FamilyTreeDto dto) {
        ProfileEntity profile=profileRepository.findById(dto.getProfileId()).orElseThrow(()-> new AppBadException("ProfileId wrong !"));
        FamilyTree tree = FamilyTree.builder()
                        .name(dto.getName())
                        .profile(profile)
                        .build();
        //BU TREE UCHUN BIRINCHI PERSONNI YARATDIM


        Integer id=SpringSecurityUtil.getCurrentUserId();
        Optional<ProfileEntity> profileEntity=profileRepository.findByIdAndVisibleTrue(id);
        if(profileEntity.isEmpty()){
            throw new AppBadException("Profile yo'q !");
        }
        tree.setProfile(profileEntity.get());
        tree=repository.save(tree);

        PersonCreateDto personCreateDto=new PersonCreateDto();
        personCreateDto.setTreeId(tree.getId());

        PersonResponseDto response=personService.create(personCreateDto);
        tree.setLastPersonId(response.getId());


        return toDto(repository.save(tree));
    }

    @Override
    public List<FamilyTreeDto> getAll() {
        Integer profileId = SpringSecurityUtil.getCurrentUserId();

        return repository.findByProfileId(profileId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public FamilyTreeDto getById(Long id) {

        Integer profileId = SpringSecurityUtil.getCurrentUserId();

        FamilyTree tree = repository
                .findByIdAndProfileId(id, profileId)
                .orElseThrow(() -> new AppBadException("Tree not found"));

        return toDto(tree);
    }

    @Override
    public FamilyTreeDto update(Long id, String name) {
        Integer profileId = SpringSecurityUtil.getCurrentUserId();

        FamilyTree tree = repository
                .findByIdAndProfileId(id, profileId)
                .orElseThrow(() -> new AppBadException("Tree not found"));

        tree.setName(name);

        return toDto(repository.save(tree));
    }

    @Override
    public void delete(Long id) {

        Integer profileId = SpringSecurityUtil.getCurrentUserId();

        FamilyTree tree = repository
                .findByIdAndProfileId(id, profileId)
                .orElseThrow(() -> new AppBadException("Tree not found"));

        repository.delete(tree);
    }
    @Override
    public List<FamilyTreeDto> getAllByProfileId(Integer profileId){
        return repository.findByProfileId(profileId)
                .stream()
                .map(tree -> new FamilyTreeDto(tree.getId(), tree.getName(), tree.getLastPersonId(),tree.getProfile().getId()))
                .collect(Collectors.toList());
    }



    private FamilyTreeDto toDto(FamilyTree tree) {
        return new FamilyTreeDto(tree.getId(), tree.getName(),tree.getLastPersonId(),tree.getProfile().getId());
    }


}
