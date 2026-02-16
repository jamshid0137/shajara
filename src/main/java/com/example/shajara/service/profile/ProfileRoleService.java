package com.example.shajara.service.profile;

import com.example.shajara.entity.ProfileRoleEntity;
import com.example.shajara.enums.ProfileRole;
import com.example.shajara.repository.ProfileRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ProfileRoleService {
    @Autowired
    private ProfileRoleRepository profileRoleRepository;

    public void create(Integer profileId, ProfileRole role){ //role qo'shish funksiyasi
        ProfileRoleEntity entity=new ProfileRoleEntity();
        entity.setProfileId(profileId);
        entity.setRole(role);
        entity.setCreatedDate(LocalDateTime.now());
        profileRoleRepository.save(entity);
    }
    public void deleteRoll(Integer profileId){
        profileRoleRepository.deleteByProfileId(profileId);
    }
}
