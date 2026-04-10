package com.example.shajara.dto.person;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonAddMotherDto {
    private Long id;     // child person ID
    private Long treeId;
}
