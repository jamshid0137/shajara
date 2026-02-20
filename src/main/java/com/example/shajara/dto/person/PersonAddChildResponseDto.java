package com.example.shajara.dto.person;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PersonAddChildResponseDto {
    private Long id;
    private Long newChildId;
}
