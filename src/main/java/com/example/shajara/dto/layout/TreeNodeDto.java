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
    private String gender;
    private String role;
    private LocalDate birthDate;
    private LocalDate diedDate;
    private String photoUrl;
    private Long treeId;

    // ✅ FamilyTree.js uchun to'g'ridan-to'g'ri fid/mid
    private Long fatherId; // Person.fatherId
    private Long motherId; // Person.motherId

    private double x;
    private double y;
}
