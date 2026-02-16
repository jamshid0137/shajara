package com.example.shajara.controller.profile;

import com.example.shajara.dto.AppResponse;
import com.example.shajara.dto.CodeConfirmDTO;
import com.example.shajara.dto.ProfileDTO;
import com.example.shajara.dto.profile.ProfileDetailUpdateDTO;
import com.example.shajara.dto.profile.ProfilePasswordUpdateDTO;
import com.example.shajara.dto.profile.ProfilePhotoUpdateDTO;
import com.example.shajara.dto.profile.ProfileUsernameUpdateDTO;
import com.example.shajara.service.profile.ProfileService;
import com.example.shajara.util.SpringSecurityUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/profile")
public class ProfileController {
    @Autowired
    private ProfileService profileService;

    @PutMapping("/detail")
    public ResponseEntity<AppResponse<String>> updateDetail(@Valid @RequestBody ProfileDetailUpdateDTO dto){
        AppResponse<String>response=profileService.updateDetail(dto);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/password")
    public ResponseEntity<AppResponse<String>> updatePassword(@Valid @RequestBody ProfilePasswordUpdateDTO dto){
        AppResponse<String>response=profileService.updatePassword(dto);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/change-username")
    public ResponseEntity<AppResponse<String>> updateUsername(@Valid @RequestBody ProfileUsernameUpdateDTO dto){
        AppResponse<String>response=profileService.updateUsername(dto);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/username/confirm")
    public ResponseEntity<AppResponse<String>> updateUsernameConfirm(@Valid @RequestBody CodeConfirmDTO dto){
        AppResponse<String>response=profileService.updateUsernameConfirm(dto);
        return ResponseEntity.ok(response);
    }


    // --- Yangi qo‘shiladigan metod: profilni va familyTree’larni olish ---
    @GetMapping("/me")
    public ResponseEntity<ProfileDTO> getProfileDetail() {
        Integer currentProfileId = SpringSecurityUtil.getCurrentUserId();
        ProfileDTO profileDTO = profileService.getProfileDetail(currentProfileId);
        return ResponseEntity.ok(profileDTO);
    }
}
