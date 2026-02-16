package com.example.shajara.dto;



import com.example.shajara.enums.ProfileRole;
import lombok.*;

import java.util.List;

@Getter
@AllArgsConstructor
@Setter
@Builder
@NoArgsConstructor
public class JwtDTO {
    private Integer id;
    private List<ProfileRole> roleList;

    private String username;

    public JwtDTO(String username, Integer id, List<ProfileRole> roleList) {
        this.id = id;
        this.roleList = roleList;
        this.username=username;
    }
}
