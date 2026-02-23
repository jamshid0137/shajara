package com.example.shajara.dto.person;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonAddParentDto {
    private Long id;
    private Long fatherId;
    private Long motherId;
    private Long treeId;
}
