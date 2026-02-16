package com.example.shajara.config;


import com.example.shajara.entity.ProfileEntity;
import com.example.shajara.enums.ProfileRole;
import com.example.shajara.repository.ProfileRepository;
import com.example.shajara.repository.ProfileRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    @Autowired
    private ProfileRepository profileRepository;
    @Autowired
    private ProfileRoleRepository profileRoleRepository;
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {//username bo'yicha shu odamni topib return qilishimiz kk
        System.out.println("loadUserByUsername: "+username);
        Optional<ProfileEntity>optional=profileRepository.findByUsernameAndVisibleTrue(username);
        if(optional.isEmpty()){
            throw new UsernameNotFoundException("Username not found !");
        }
        ProfileEntity profile= optional.get();
        List<ProfileRole>roles=profileRoleRepository.getAllRolesListByProfileId(profile.getId());
        return new CustomUserDetails(profile,roles);
    }
}
