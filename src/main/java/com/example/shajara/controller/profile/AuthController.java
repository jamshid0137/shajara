package com.example.shajara.controller.profile;

import com.example.shajara.dto.AppResponse;
import com.example.shajara.dto.ProfileDTO;
import com.example.shajara.dto.auth.AuthDTO;
import com.example.shajara.dto.auth.RegistrationDTO;
import com.example.shajara.dto.auth.ResetPasswordConfirmDTO;
import com.example.shajara.dto.auth.ResetPasswordDTO;
import com.example.shajara.dto.sms.SmsResendDTO;
import com.example.shajara.dto.sms.SmsVerificationDTO;
import com.example.shajara.service.auth.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
//@Tag(name = "AuthController",description = "Bu auth amallar uchun !")

public class AuthController {
    private static Logger log = LoggerFactory.getLogger(AuthController.class);
    //har bir metodda bu classda yoziladi loglar
    @Autowired
    private AuthService authService;

    @PostMapping("/registration")
    public ResponseEntity<AppResponse<String>> registration(@Valid  @RequestBody RegistrationDTO dto
                                                            /*@RequestParam("lang")AppLanguage language*/
                                                            ){//@Valid validatsiyani ishlat xatosini ayt degani
        log.info("registration : username : {},password : {}",dto.getUsername(),dto.getPassword());
        return ResponseEntity.ok().body(authService.registration(dto));
    }

    ///auth/registration/verification/{token}?lang=UZ
    @GetMapping("/registration/email-verification/{token}")
    public ResponseEntity<String> registrationEmailVerification(@PathVariable("token") String token){//@Valid validatsiyani ishlat xatosini ayt degani
        log.info("registration email verify : token : {}",token);
        return ResponseEntity.ok().body(authService.registrationEmailVerification(token));
    }

    @PostMapping("/registration/sms-verification")
    //@Operation(summary = "Create post", description = "Api used for creating new post")//xato yozuvi faqat
    public ResponseEntity<ProfileDTO> registrationSmsVerification(@Valid @RequestBody SmsVerificationDTO dto){//@Valid validatsiyani ishlat xatosini ayt degani

        return ResponseEntity.ok().body(authService.registrationSmsVerification(dto));
    }

    @PostMapping("/registration/sms-verification-resend")
    public ResponseEntity<String> registrationSmsVerificationResend(@Valid @RequestBody SmsResendDTO dto){//@Valid validatsiyani ishlat xatosini ayt degani

        return ResponseEntity.ok().body(authService.registrationSmsVerificationResend(dto));
    }

    @PostMapping("/login")
    public ResponseEntity<ProfileDTO> login(@Valid  @RequestBody AuthDTO dto){//@Valid validatsiyani ishlat xatosini ayt degani
        log.info("login : username : {},password : {}",dto.getUsername(),dto.getPassword());
        return ResponseEntity.ok().body(authService.login(dto));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<AppResponse<String>> resetPassword(@Valid @RequestBody ResetPasswordDTO dto){//@Valid validatsiyani ishlat xatosini ayt degani
        return ResponseEntity.ok().body(authService.resetPassword(dto));
    }

    @PostMapping("/reset-password-confirm")
    public ResponseEntity<AppResponse<String>> resetPasswordConfirm(@Valid @RequestBody ResetPasswordConfirmDTO dto){//@Valid validatsiyani ishlat xatosini ayt degani
        return ResponseEntity.ok().body(authService.resetPasswordConfirm(dto));
    }

}
