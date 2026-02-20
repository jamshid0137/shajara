package com.example.shajara.entity;

import com.example.shajara.enums.Gender;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "persons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    private LocalDate birthDate;

    private String profession;

    private String homeland;

    private LocalDate diedDate; // nullable

    private String phoneNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tree_id", nullable = false)
    private FamilyTree familyTree;

    private Long fatherId;
    private Long motherId;

    @Column(name = "photo_url")
    private String photoUrl;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Person)) return false;
        return id != null && id.equals(((Person) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

