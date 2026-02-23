package com.example.shajara.dto.layout;

import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TreeNodeDto {
    private Long id;
    private String name;
    private String gender; // MALE / FEMALE
    private String role; // CENTER, FATHER, MOTHER, SPOUSE, CHILD, SIBLING
    private LocalDate birthDate;
    private LocalDate diedDate;
    private String photoUrl;

    private double x;
    private double y;
}
