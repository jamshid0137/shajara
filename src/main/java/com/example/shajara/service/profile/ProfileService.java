package com.example.shajara.service.profile;

import com.example.shajara.dto.AppResponse;
import com.example.shajara.dto.CodeConfirmDTO;
import com.example.shajara.dto.FamilyTreeDto;
import com.example.shajara.dto.ProfileDTO;
import com.example.shajara.dto.profile.ProfileDetailUpdateDTO;
import com.example.shajara.dto.profile.ProfilePasswordUpdateDTO;
import com.example.shajara.dto.profile.ProfileUsernameUpdateDTO;
import com.example.shajara.entity.ProfileEntity;
import com.example.shajara.enums.GeneralStatus;
import com.example.shajara.enums.ProfileRole;
import com.example.shajara.exception.AppBadException;
import com.example.shajara.repository.ProfileRepository;
import com.example.shajara.repository.ProfileRoleRepository;
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

import java.util.List;
import java.util.Optional;

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

    private final FamilyTreeService familyTreeService;




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

        List<FamilyTreeDto> familyTrees = familyTreeService.getAllByProfileId(profileId);

        return ProfileDTO.builder()
                .name(profile.getName())
                .username(profile.getUsername())
                .roles(profileRoleRepository.getAllRolesListByProfileId(profileId))
                .familyTrees(familyTrees)
                .build();
    }

}
