package com.example.shajara.service.profile;

import com.example.shajara.dto.*;
import com.example.shajara.dto.profile.ProfileDetailUpdateDTO;
import com.example.shajara.dto.profile.ProfilePasswordUpdateDTO;
import com.example.shajara.dto.profile.ProfileUsernameUpdateDTO;
import com.example.shajara.entity.FamilyTree;
import com.example.shajara.entity.Person;
import com.example.shajara.entity.ProfileEntity;
import com.example.shajara.entity.Relation;
import com.example.shajara.enums.GeneralStatus;
import com.example.shajara.enums.ProfileRole;
import com.example.shajara.exception.AppBadException;
import com.example.shajara.repository.*;
import com.example.shajara.service.FamilyTreeService;
import com.example.shajara.service.email.EmailHistoryService;
import com.example.shajara.service.email.EmailSendingService;
import com.example.shajara.service.email.SmsHistoryService;
import com.example.shajara.util.EmailUtil;
import com.example.shajara.util.JwtUtil;
import com.example.shajara.util.SmsUtil;
import com.example.shajara.util.SpringSecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class ProfileService {
    @Autowired
    private ProfileRepository profileRepository;
    @Autowired
    private BCryptPasswordEncoder bc;
    @Autowired
    private EmailSendingService emailSendingService;

    @Autowired
    private EmailHistoryService emailHistoryService;
    @Autowired
    private SmsHistoryService smsHistoryService;

    @Autowired
    private ProfileRoleRepository profileRoleRepository;

    private final FamilyTreeRepository repository;
    private final FamilyTreeService familyTreeService;

    private final RelationRepository relationRepository;
    @Autowired
    private PersonRepository personRepository;


    public AppResponse<String>updateDetail(ProfileDetailUpdateDTO dto){
        Integer userId= SpringSecurityUtil.getCurrentUserId();

        profileRepository.changeName(userId,dto.getName());
        return new AppResponse<>("Name maffaqiyatli o'zgatirildi !");
    }

    public ProfileEntity getById(Integer id){

        return profileRepository.findByIdAndVisibleTrue(id).orElseThrow(() -> {
            throw new AppBadException("Profile Not Found !");
        });
    }

    public AppResponse<String> updatePassword(ProfilePasswordUpdateDTO dto) {
        Integer userId= SpringSecurityUtil.getCurrentUserId();
        ProfileEntity profile=getById(userId);
        String shifrPass=bc.encode(dto.getNewPassword());
        if(!bc.matches(dto.getCurrentPassword(),profile.getPassword())){
            throw new AppBadException("Current-eski  password xato !");
        }
        profileRepository.changePassword(profile.getId(),shifrPass);
        return new AppResponse<>("Password muaffiqiyatli o'zgartirildi !");
    }

    //todo cheksiz bosilishidan oldini olish
    public AppResponse<String> updateUsername(ProfileUsernameUpdateDTO dto) {

        Optional<ProfileEntity>optional=profileRepository.findByUsernameAndVisibleTrue(dto.getUsername());
        //check
        if(optional.isPresent() && optional.get().getStatus()!= GeneralStatus.IN_REGISTRATION){
            throw new AppBadException("bunday user tizimda mavjud");
        }
        //sendConfirmCode
        if(EmailUtil.isEmail(dto.getUsername())){
            emailSendingService.sendChangeUsernameEmail(dto.getUsername());}
        else if(SmsUtil.isPhone(dto.getUsername())) {
            throw new AppBadException("SMS tizimi ishlamayapti.Email bilan kiring ilovaga !");
            //smsSendingService.sendChangeUsernameSms(dto.getUsername()); //todo
        }else{
            throw new AppBadException("wrong username !");
        }
        //save
        Integer userId= SpringSecurityUtil.getCurrentUserId();
        ProfileEntity profile=getById(userId);
        profileRepository.changeTempUsername(userId, dto.getUsername());

        return new AppResponse<>("Usernamega tasdiqlash code jo'natildi  !");
    }

    public AppResponse<String> updateUsernameConfirm(CodeConfirmDTO dto) {
        Integer userId= SpringSecurityUtil.getCurrentUserId();
        ProfileEntity profile=getById(userId);
        String tempUsername=profile.getTempUsername();

        if(!profile.getStatus().equals(GeneralStatus.ACTIVE)){
            throw new AppBadException("status wrong !");
        }
        //check
        if(EmailUtil.isEmail(tempUsername)){
            if(!emailHistoryService.check(tempUsername, dto.getCode())){
                throw new AppBadException("code wrong !");
            }
        }
        else if(SmsUtil.isPhone(tempUsername)) {
            throw new AppBadException("SMS tizimi ishlamayapti.Email bilan kiring ilovaga !");
//            if(!smsHistoryService.check(tempUsername, dto.getCode())){
//                throw new AppBadException("code wrong !");
//            }
        }else{
            throw new AppBadException("wrong username !");
        }
        //update username
        profileRepository.changeUsername(profile.getId(),tempUsername);

        //jwtda eski username joylashgan bo'ladi.
        //yangi jwt yasab bervoramiz.

        profile.setUsername(tempUsername);//TODO

        List<ProfileRole> roles=profileRoleRepository.getAllRolesListByProfileId(profile.getId());
        String jwt= JwtUtil.encode(profile.getUsername(),profile.getId(),roles);//jwt yasadi yangi username uchun

        return new AppResponse<>(jwt,"Username muaffaqiyatli o'zgartirildi !");
    }




    public ProfileDTO getProfileDetail(Integer profileId) {
        ProfileEntity profile = getById(profileId);
        Integer id2=SpringSecurityUtil.getCurrentUserId();

        if(id2==null || !id2.equals(profileId)){
            throw new AppBadException("It does not belong to your profile Id !");
        }

        List<PartFamilyTreeDto> invitedTrees = profile.getInvitedTreeIds().stream()
                .map(treeId -> {
                    FamilyTreeDto tree = familyTreeService.getByIdForInvite(treeId);
                    return PartFamilyTreeDto.builder()
                            .id(tree.getId())
                            .name(tree.getName())
                            .build();
                })
                .toList();

        List<FamilyTreeDto> familyTrees = familyTreeService.getAllByProfileId(profileId);

        return ProfileDTO.builder()
                .name(profile.getName())
                .username(profile.getUsername())
                .roles(profileRoleRepository.getAllRolesListByProfileId(profileId))
                .familyTrees(familyTrees)
                .invitedTrees(invitedTrees)
                .build();
    }


    //TESTLASH SHART BUNI !!! TODO
    @Transactional
    public AppResponse<String> acceptInvitedTree(Long invitedTreeId) {
        Integer userId = SpringSecurityUtil.getCurrentUserId();
        ProfileEntity profile = getById(userId);

        if (!profile.getInvitedTreeIds().contains(invitedTreeId)) {
            throw new AppBadException("Bu tree sizga invite qilinmagan!");
        }

        // 1️⃣ Original tree
        FamilyTreeDto originalTree = familyTreeService.getByIdForInvite(invitedTreeId);
        List<Person> persons=personRepository.findAllByFamilyTreeId(invitedTreeId);
        // 2️⃣ Clone tree
        FamilyTree clonedTree = FamilyTree.builder()
                .name(originalTree.getName())
                .profile(profile)
                .persons(new ArrayList<>())
                .lastPersonId(originalTree.getLastPersonId())
                .build();

        // 3️⃣ Clone persons va mapping
        Map<Long, Person> personMap = new HashMap<>();
        for (Person p : persons) {
            Person clonedPerson = Person.builder()
                    .name(p.getName())
                    .gender(p.getGender())
                    .birthDate(p.getBirthDate())
                    .diedDate(p.getDiedDate())
                    .profession(p.getProfession())
                    .homeland(p.getHomeland())
                    .phoneNumber(p.getPhoneNumber())
                    .fatherId(p.getFatherId())
                    .motherId(p.getMotherId())
                    .familyTree(clonedTree)
                    .photoUrl(p.getPhotoUrl())
                    .build();

            clonedTree.getPersons().add(clonedPerson);
            personMap.put(p.getId(), clonedPerson);
        }

        // 4️⃣ Save cloned tree (cascade bilan persons saqlanadi)
        clonedTree = repository.save(clonedTree);

        // 5️⃣ Clone relations
        List<Relation> relations = relationRepository.findByTreeId(originalTree.getId());
        for (Relation r : relations) {
            Relation clonedRelation = Relation.builder()
                    .fromPerson(personMap.get(r.getFromPerson().getId()))
                    .toPerson(personMap.get(r.getToPerson().getId()))
                    .type(r.getType())
                    .divorced(r.isDivorced())
                    .build();

            relationRepository.save(clonedRelation);
        }

        // 6️⃣ Update profile
        profile.getInvitedTreeIds().remove(invitedTreeId);
        profile.getFamilyTrees().add(clonedTree);
        profileRepository.save(profile);

        return new AppResponse<>("Tree muvaffaqiyatli qo'shildi!");
    }


    @Transactional
    public AppResponse<String> removeInvitedTree(Long invitedTreeId) {
        Integer userId = SpringSecurityUtil.getCurrentUserId();
        ProfileEntity profile = profileRepository.findById(userId)
                .orElseThrow(() -> new AppBadException("Profil topilmadi!"));

        if (!profile.getInvitedTreeIds().contains(invitedTreeId)) {
            throw new AppBadException("Bu tree sizga invite qilinmagan!");
        }

        // Invited listdan o'chirish
        profile.getInvitedTreeIds().remove(invitedTreeId);
        profileRepository.save(profile);

        return new AppResponse<>("Tree invited listdan muvaffaqiyatli o'chirildi!");
    }

}
