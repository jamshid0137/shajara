package com.example.shajara.entity.history;


import com.example.shajara.enums.SmsType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Table(name = "email_history")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "email")
    private String email;  //email

    @Column(name = "created_date")
    private LocalDateTime createdDate;


    @Column(name = "code")
    private String code;

    @Column(name = "attempt_count")
    private Integer attempt_count = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "email_type")
    private SmsType smsType;
}