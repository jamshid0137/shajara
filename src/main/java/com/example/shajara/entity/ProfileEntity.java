package com.example.shajara.entity;

import com.example.shajara.enums.GeneralStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Table(name = "profile")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name")
    private String name;
    @Column(name = "username")
    private String username; //email or phone ikkalasidan bittasi

    @Column(name = "temp_username")
    private String tempUsername; //o'zgartirmoqchi bo'lgan usernami

    @Column(name = "password")
    private String password;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private GeneralStatus status;

    @Column(name = "visible")
    private Boolean visible=Boolean.TRUE;// masalan accountini o'chirib yuborsa visibleni false qb qo'yamz db dan o'chrib yubormasdan.

    @Column(name="created_date")
    private LocalDateTime createdDate;//tizimda qachon yaratildi bu profile degani.

    @Column(name = "photo_id")
    private String photoId;

    //SHU JOYIDA LIST QILIB O'ZINI FAMILYTREELARINI BERIB KETISHIM KERAK //TODO
    @OneToMany(mappedBy = "profile",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<FamilyTree> familyTrees = new ArrayList<>();


}
