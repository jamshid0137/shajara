package com.example.shajara.service.serviceImpl;


import com.example.shajara.dto.FamilyTreeDto;
import com.example.shajara.dto.person.PersonCreateDto;
import com.example.shajara.dto.person.PersonResponseDto;
import com.example.shajara.entity.FamilyTree;
import com.example.shajara.entity.ProfileEntity;
import com.example.shajara.enums.GeneralStatus;
import com.example.shajara.exception.AppBadException;
import com.example.shajara.exception.NotFoundException;
import com.example.shajara.repository.FamilyTreeRepository;
import com.example.shajara.repository.ProfileRepository;
import com.example.shajara.service.FamilyTreeService;
import com.example.shajara.service.PersonService;
import com.example.shajara.service.email.EmailSendingService;
import com.example.shajara.util.EmailUtil;
import com.example.shajara.util.SmsUtil;
import com.example.shajara.util.SpringSecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class FamilyTreeServiceImpl implements FamilyTreeService {

    private final FamilyTreeRepository repository;
    private final PersonService personService;
    @Autowired
    private EmailSendingService emailSendingService;

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
            throw new NotFoundException("Profile yo'q !");
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

    public FamilyTreeDto getByIdForInvite(Long id) {

        FamilyTree tree = repository
                .findById(id)
                .orElseThrow(() -> new NotFoundException("Tree not found"));

        return toDto(tree);
    }

    @Override
    public FamilyTreeDto getById(Long id) {

        Integer profileId = SpringSecurityUtil.getCurrentUserId();

        FamilyTree tree = repository
                .findByIdAndProfileId(id, profileId)
                .orElseThrow(() -> new NotFoundException("Tree not found"));

        return toDto(tree);
    }

    @Override
    public FamilyTreeDto update(Long id, String name) {
        Integer profileId = SpringSecurityUtil.getCurrentUserId();

        FamilyTree tree = repository
                .findByIdAndProfileId(id, profileId)
                .orElseThrow(() -> new NotFoundException("Tree not found"));

        tree.setName(name);

        return toDto(repository.save(tree));
    }

    @Override
    public void delete(Long id) {

        Integer profileId = SpringSecurityUtil.getCurrentUserId();

        FamilyTree tree = repository
                .findByIdAndProfileId(id, profileId)
                .orElseThrow(() -> new NotFoundException("Tree not found"));

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




    //new share function
    @Override
    @Transactional
    public void inviteFamilyTreeToProfile(Long treeId, String username) {
        Integer id=SpringSecurityUtil.getCurrentUserId();
        ProfileEntity profileReal=profileRepository.findById(id).orElseThrow(()->new AppBadException("Sizni profilingiz topilmadi"));

        if(SmsUtil.isPhone(username)){
            throw new AppBadException("Sms provider ishlamayapti iltimos email orqali share qiling");
        }else if(!EmailUtil.isEmail(username)){
            throw new AppBadException("Username wrong !");
        }

        FamilyTree tree = repository.findById(treeId)
                .orElseThrow(() -> new NotFoundException("FamilyTree not found"));

        Optional<ProfileEntity> targetProfile = profileRepository.findByUsername(username);
        if(targetProfile.isEmpty()){
            //new yaratib inregist qilib unga qo'shamiza
            ProfileEntity profile=new ProfileEntity();
            profile.setUsername(username);
            profile.setCreatedDate(LocalDateTime.now());
            profile.getInvitedTreeIds().add(treeId);
            profile.setStatus(GeneralStatus.IN_REGISTRATION);
            profileRepository.save(profile);

            emailSendingService.sendShareUsernameEmail(username,profileReal.getName(),tree.getName());
            return;
        }

        if(id!=null && id.equals(targetProfile.get().getId())){
            throw new AppBadException("O'zingga o'zing invite qila olmaysan !");
        }

        if (!targetProfile.get().getInvitedTreeIds().contains(tree.getId())) {
            targetProfile.get().getInvitedTreeIds().add(tree.getId());
        }
        //sendShareUsernameEmail
        emailSendingService.sendShareUsernameEmail(username,profileReal.getName(),tree.getName());

        profileRepository.save(targetProfile.get());
    }

}
