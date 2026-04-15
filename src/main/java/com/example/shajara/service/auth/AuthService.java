package com.example.shajara.service.auth;

import com.example.shajara.dto.AppResponse;
import com.example.shajara.dto.ProfileDTO;
import com.example.shajara.dto.auth.AuthDTO;
import com.example.shajara.dto.auth.RegistrationDTO;
import com.example.shajara.dto.auth.ResetPasswordConfirmDTO;
import com.example.shajara.dto.auth.ResetPasswordDTO;
import com.example.shajara.dto.sms.SmsResendDTO;
import com.example.shajara.dto.sms.SmsVerificationDTO;
import com.example.shajara.entity.ProfileEntity;
import com.example.shajara.enums.GeneralStatus;
import com.example.shajara.enums.ProfileRole;
import com.example.shajara.exception.AppBadException;
import com.example.shajara.repository.ProfileRepository;
import com.example.shajara.repository.ProfileRoleRepository;
import com.example.shajara.service.email.EmailHistoryService;
import com.example.shajara.service.email.EmailSendingService;
import com.example.shajara.service.email.SmsHistoryService;
import com.example.shajara.service.profile.ProfileRoleService;
import com.example.shajara.service.profile.ProfileService;
import com.example.shajara.util.EmailUtil;
import com.example.shajara.util.JwtUtil;
import com.example.shajara.util.SmsUtil;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AuthService {
    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private BCryptPasswordEncoder bc;

    @Autowired
    private ProfileRoleService profileRoleService;
    @Autowired
    private EmailSendingService emailSendingService;

    @Autowired
    private ProfileService profileService;

    @Autowired
    private ProfileRoleRepository profileRoleRepository;

    @Autowired
    private SmsHistoryService smsHistoryService;

    @Autowired
    private EmailHistoryService emailHistoryService;

    private static Logger log = LoggerFactory.getLogger(AuthService.class);

    public AppResponse<String> registration(RegistrationDTO dto) {
        // 1.validation buni keyin tekshiramiz
        // 2.email bor yo'qligi tizimda
        // inregist bo'lgan bo'lsa shartmanda
        Optional<ProfileEntity> optional = profileRepository.findByUsernameAndVisibleTrue(dto.getUsername());
        if (optional.isPresent()) {
            ProfileEntity profile = optional.get();
            if (profile.getStatus().equals(GeneralStatus.IN_REGISTRATION)) { // menga id kerakda shunga o'chirib
                                                                             // yubormadim
                profileRoleService.deleteRoll(profile.getId());

                // send sms/email
                ProfileEntity entity = optional.get();
                entity.setName(dto.getName());
                entity.setPassword(bc.encode(dto.getPassword()));
                entity.setUsername(dto.getUsername());
                // entity.setStatus(GeneralStatus.IN_REGISTRATION);
                entity.setStatus(GeneralStatus.ACTIVE);
                entity.setVisible(true);
                entity.setCreatedDate(LocalDateTime.now());
                profileRepository.save(entity);// save
                // insert role
                profileRoleService.create(entity.getId(), ProfileRole.ROLE_USER);
                if (EmailUtil.isEmail(dto.getUsername())) {
                    // emailSendingService.sendRegistrationEmail(dto.getUsername(),entity.getId());
                } else if (SmsUtil.isPhone(dto.getUsername())) {
                    // todo
                    // throw new AppBadException("Sms provider ishlamayapti.Email orqali
                    // registratsiya qiling !");
                    // smsSendingService.registrationSms(dto.getUsername());
                } else {
                    throw new AppBadException("wrong username !");
                }
                // return new AppResponse<>("Activation link send to your email");
                return new AppResponse<>("Tabriklaymiz ,Account created ,Login qilib kiring !");

            } else {
                log.warn("Profile already exist with username : {}", dto.getUsername());
                // throw new
                // AppBadException(messageSource.getMessage("email.phone.exist",null,new
                // Locale(lang.name())));
                throw new AppBadException("Email or Phone  already exist !");
            }
        }

        ProfileEntity entity = new ProfileEntity();
        entity.setName(dto.getName());
        entity.setPassword(bc.encode(dto.getPassword()));
        entity.setUsername(dto.getUsername());
        // entity.setStatus(GeneralStatus.IN_REGISTRATION);
        entity.setStatus(GeneralStatus.ACTIVE);
        entity.setVisible(true);
        entity.setCreatedDate(LocalDateTime.now());
        profileRepository.save(entity);// save
        // insert role
        profileRoleService.create(entity.getId(), ProfileRole.ROLE_USER);
        if (EmailUtil.isEmail(dto.getUsername())) {
            // emailSendingService.sendRegistrationEmail(dto.getUsername(), entity.getId());
        } else if (SmsUtil.isPhone(dto.getUsername())) {
            // todo
            // throw new AppBadException("Sms provider ishlamayapti.Email orqali
            // registratsiya qiling !");
            // smsSendingService.registrationSms(dto.getUsername());
        } else {
            throw new AppBadException("wrong username !");
        }
        return new AppResponse<>("Activation link send to your email");
    }

    // todo cheksiz bosib o'tiravarmasligi uchun nima qilay buni
    public String registrationEmailVerification(String token) {
        try {
            Integer profileId = JwtUtil.decodeRegVerToken(token);// idni email serviceda shifrlagandikku o'sha uchun
            ProfileEntity profile = profileService.getById(profileId);
            if (profile.getStatus().equals(GeneralStatus.IN_REGISTRATION)) {// block yana active qilmidi linkdan

                profileRepository.changeStatus(profileId, GeneralStatus.ACTIVE);
                return "Verification finished !";
            }
        } catch (JwtException e) {
            e.printStackTrace();
        }
        log.warn("Registration Email Verification failed !");
        throw new AppBadException("Verification failed !");

    }

    public ProfileDTO login(AuthDTO dto) {
        Optional<ProfileEntity> optional = profileRepository.findByUsernameAndVisibleTrue(dto.getUsername());
        if (optional.isEmpty()) {
            log.warn(" username wrong : {}", dto.getUsername());
            throw new AppBadException("Username is wrong");
        }
        if (!optional.get().getStatus().equals(GeneralStatus.ACTIVE)) {
            log.warn("Wrong status  ");// xuddi shunaqa qolgan joylarda ham yozib chiqiladi
            throw new AppBadException("Status is wrong !");
        }

        if (!bc.matches(dto.getPassword(), optional.get().getPassword())) {// matches bcrypda tenglika tekshiriberadi.
            throw new AppBadException("Password is wrong");
        }

        ProfileEntity profile = optional.get();
        // response
        return getLogInResponse(profile);
    }

    public ProfileDTO registrationSmsVerification(SmsVerificationDTO dto) {
        // todo
        throw new AppBadException("Sms provider ishlamayapti.Email orqali registratsiya qiling !");
        // phone and code:12345
        //
        // Optional<ProfileEntity>optional=profileRepository.findByUsernameAndVisibleTrue(dto.getPhone());
        // if(optional.isEmpty()){
        // throw new AppBadException("phone not exist !");
        // }
        // ProfileEntity profile= optional.get();
        // if(!profile.getStatus().equals(GeneralStatus.IN_REGISTRATION)){
        // throw new AppBadException("status wrong !");
        // }
        // //code check
        // if(!smsHistoryService.check(dto.getPhone(), dto.getCode())){
        // throw new AppBadException("code wrong !");
        // }
        //
        // //ACTIVE
        // profileRepository.changeStatus(profile.getId(),GeneralStatus.ACTIVE);
        // //smsni tasdiqlaagandan keyin qayta login qilib o'tirmasin to'g'ri kirsin
        // accountiga
        //
        //
        // return getLogInResponse(profile);

        // return resourceBundleService.message("verif.finish",lang);
    }

    public String registrationSmsVerificationResend(SmsResendDTO dto) {
        // todo
        throw new AppBadException("Sms provider ishlamayapti.Email orqali registratsiya qiling !");
        // phone and code:12345
        //
        // Optional<ProfileEntity>optional=profileRepository.findByUsernameAndVisibleTrue(dto.getPhone());
        // if(optional.isEmpty()){
        // throw new AppBadException("phone not exist !");
        // }
        // ProfileEntity profile= optional.get();
        // if(!profile.getStatus().equals(GeneralStatus.IN_REGISTRATION)){
        // throw new AppBadException("status wrong !");
        // }
        // //smsSendingService.registrationSms(dto.getPhone(),lang);//sms jo'nat degani
        //
        // return "yangi sms muaffiqiyatli qayta jo'natildi !";
    }

    public AppResponse<String> resetPassword(ResetPasswordDTO dto) {
        String username = dto.getUsername();
        // check
        Optional<ProfileEntity> optional = profileRepository.findByUsernameAndVisibleTrue(username);
        if (optional.isEmpty()) {
            throw new AppBadException("phone not exist !");
        }
        ProfileEntity profile = optional.get();
        if (!profile.getStatus().equals(GeneralStatus.ACTIVE)) {
            throw new AppBadException("status wrong !");
        }

        if (EmailUtil.isEmail(dto.getUsername())) {
            emailSendingService.sendResetPasswordEmail(dto.getUsername());
        } else if (SmsUtil.isPhone(dto.getUsername())) {
            // todo
            throw new AppBadException("Sms provider ishlamayapti.Email orqali  qiling !");
            // smsSendingService.sendResetPasswordSms(dto.getUsername());
        } else {
            throw new AppBadException("wrong username !");
        }

        return new AppResponse<>("Tasdiqlash kodi " + username + "ga jo'natildi");
    }

    public AppResponse<String> resetPasswordConfirm(ResetPasswordConfirmDTO dto) {// parolni o'zgartirayotganda
                                                                                  // tasdiqlash kodni

        Optional<ProfileEntity> optional = profileRepository.findByUsernameAndVisibleTrue(dto.getUsername());
        if (optional.isEmpty()) {
            throw new AppBadException("phone not exist !");
        }

        ProfileEntity profile = optional.get();
        if (!profile.getStatus().equals(GeneralStatus.ACTIVE)) {
            throw new AppBadException("status wrong !");
        }
        // check
        if (EmailUtil.isEmail(dto.getUsername())) {
            if (!emailHistoryService.check(dto.getUsername(), dto.getCode())) {
                throw new AppBadException("code wrong !");
            }
        } else if (SmsUtil.isPhone(dto.getUsername())) {
            // todo
            throw new AppBadException("Sms provider ishlamayapti.Email orqali  qiling !");
            // if(!smsHistoryService.check(dto.getUsername(), dto.getCode())){
            // throw new AppBadException("code wrong !");
            // }
        } else {
            throw new AppBadException("wrong username !");
        }
        profileRepository.changePassword(profile.getId(), bc.encode(dto.getPassword()));

        return new AppResponse<>("passwordi  " + dto.getUsername() + "ni  muaffaqiyatli o'zgartirildi !");
    }

    public ProfileDTO getLogInResponse(ProfileEntity profile) {
        ProfileDTO response = new ProfileDTO();
        response.setName(profile.getName());
        response.setUsername(profile.getUsername());
        // setroles
        response.setRoles(profileRoleRepository.getAllRolesListByProfileId(profile.getId()));
        // setjwt
        response.setJwt(JwtUtil.encode(profile.getUsername(), profile.getId(), response.getRoles()));
        return response;
    }

}
