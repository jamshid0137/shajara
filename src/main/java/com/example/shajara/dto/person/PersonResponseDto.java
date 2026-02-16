package com.example.shajara.dto.person;

import com.example.shajara.enums.Gender;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonResponseDto {

    private Long id;
    private String name;
    private Gender gender;
    private LocalDate birthDate;
    private String profession;
    private String homeland;
    private LocalDate diedDate;
    private String phoneNumber;


    private Long treeId;

    private Long fatherId;
    private Long motherId;

}
