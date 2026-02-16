package com.example.shajara.dto.relation;


import com.example.shajara.enums.RelationType;
import lombok.*;

@Getter
@Builder
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RelationResponseDto {

    private Long id;
    private Long fromPersonId;
    private Long toPersonId;
    private RelationType type;
}