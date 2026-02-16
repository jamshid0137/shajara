package com.example.shajara.dto.relation;


import com.example.shajara.enums.RelationType;
import jakarta.validation.constraints.NotNull;
import lombok.*;


@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RelationCreateDto {

    @NotNull
    private Long fromPersonId;

    @NotNull
    private Long toPersonId;

    @NotNull
    private RelationType type;


}