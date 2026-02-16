package com.example.shajara.util;


import com.example.shajara.config.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SpringSecurityUtil {
    public static CustomUserDetails getCurrentProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        return user;
    }

    public static Integer getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        //REGISTRATSIYADAN O'TMAGAN BO'LSA NULL QAYTADI
        if (authentication == null ||
                !authentication.isAuthenticated() ||
                authentication.getPrincipal().equals("anonymousUser")) {
            return null; // yoki custom exception
        }

        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        return user.getId();
    }
}
