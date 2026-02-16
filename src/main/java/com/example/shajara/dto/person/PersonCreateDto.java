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
public class PersonCreateDto {


    private String name;
    @JsonProperty(defaultValue = "MALE")
    private Gender gender =Gender.MALE;  // default

    private LocalDate birthDate;

    private String profession;

    private String homeland;

    private LocalDate diedDate;

    private String phoneNumber;

    @NotNull
    private Long treeId;


    private Long fatherId;
    private Long motherId;
}
