package com.example.shajara.config;


import com.example.shajara.entity.ProfileEntity;
import com.example.shajara.enums.GeneralStatus;
import com.example.shajara.enums.ProfileRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {
    //spring securityga userni nimalarini berib yuborishimiz kerakligini aniqlaymiz
    private Integer id;
    private String name;
    private String username;
    private String password;
    private Collection<? extends GrantedAuthority>roles;
    private GeneralStatus status;

    public CustomUserDetails(ProfileEntity profile,
                             List<ProfileRole> roleList) {
        this.id = profile.getId();
        this.name = profile.getName();
        this.username = profile.getUsername();
        this.status = profile.getStatus();
        this.password= profile.getPassword();

        List<SimpleGrantedAuthority>list=new ArrayList<>();
        for(ProfileRole role:roleList){
            list.add(new SimpleGrantedAuthority(role.name()));
        }

        this.roles = list;

    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() { //rollarini return qilishi kerak
        return roles;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() { //account yaratilganda muddat berilgan bo'lsa
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return status.equals(GeneralStatus.ACTIVE);
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
}

