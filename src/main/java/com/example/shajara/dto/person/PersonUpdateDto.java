package com.example.shajara.dto.person;

import com.example.shajara.enums.Gender;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PersonUpdateDto {

    private String name;

    private Gender gender; // default

    private LocalDate birthDate;

    private String profession;

    private String homeland;

    private LocalDate diedDate;

    private String phoneNumber;

    private Long treeId;

    private Long motherId;
    private Long fatherId;

    private String photoUrl;
}
