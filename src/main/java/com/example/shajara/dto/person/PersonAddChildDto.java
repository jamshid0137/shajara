package com.example.shajara.dto.person;

import com.example.shajara.enums.Gender;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PersonAddChildDto {
    private Long id; // birinchi ota-ona (nodeMenudan bosilgan)
    private Long spouseId; // ikkinchi ota-ona — faqat juft nodeda null emas
    private Gender childGender;
    private Long treeId;
}
