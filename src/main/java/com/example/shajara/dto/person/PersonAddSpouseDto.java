package com.example.shajara.dto.person;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonAddSpouseDto {
    private Long id;
    private Long newSpouseId;
}
