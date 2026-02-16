package com.example.shajara.dto;

import com.example.shajara.enums.ProfileRole;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) //qaysinidir qiymati null bo'lsa jsonga qo'shmaydi.
public class ProfileDTO {


    private String name;

    private String username;
    private List<ProfileRole> roles;
    private String jwt;


    //BU JOYIDA HAM FAMILY TREELARINI QO'SHIB KETISHIM KERAK //TODO
    private List<FamilyTreeDto> familyTrees;
}
