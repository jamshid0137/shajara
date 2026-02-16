package com.example.shajara.entity;


import com.example.shajara.enums.ProfileRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;


@Table(name = "profile_role")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileRoleEntity {//bitta odamda bir nechta rollar bo'lishi mumkin.shuni bazada saqlash alohida !
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name="profile_id")
    private Integer profileId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id",insertable = false,updatable = false)
    private ProfileEntity profile;


    @Enumerated(EnumType.STRING)
    @Column(name="roles")
    private ProfileRole roles;


    @Column(name="created_date")
    private LocalDateTime createdDate;//manashu profilega manashu role manashu chisloda berilgan degani

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getProfileId() {
        return profileId;
    }

    public void setProfileId(Integer profileId) {
        this.profileId = profileId;
    }

    public ProfileEntity getProfile() {
        return profile;
    }

    public void setProfile(ProfileEntity profile) {
        this.profile = profile;
    }

    public ProfileRole getRole() {
        return roles;
    }

    public void setRole(ProfileRole roles) {
        this.roles = roles;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }
}
