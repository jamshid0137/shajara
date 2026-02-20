package com.example.shajara.dto.person;

import com.example.shajara.enums.Gender;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PersonAddChildDto {
    private Long id;
    private Gender childGender;
}
