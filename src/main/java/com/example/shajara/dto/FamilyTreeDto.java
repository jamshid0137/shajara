package com.example.shajara.dto;

import lombok.*;

@Getter
@AllArgsConstructor
@Setter
@Builder
@NoArgsConstructor
public class FamilyTreeDto {
    private Long id;
    private String name;
    private Long lastPersonId;
    private Integer profileId;

}