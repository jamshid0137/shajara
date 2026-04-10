package com.example.shajara.dto.person;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonAddFatherDto {
    private Long id;     // child person ID
    private Long treeId;
}
