package com.example.shajara.entity.history;


import com.example.shajara.enums.SmsType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Table(name = "sms_history")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmsHistoryEntity { //sms jo'natilganda bazaga saqlash uchun
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "username")
    private String username;  //PHONE

    @Column(name = "message", columnDefinition = "text")
    private String message;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "sms_type")
    private SmsType smsType;

    @Column(name = "code")
    private String code;

    @Column(name = "attempt_count")
    private Integer attempt_count = 0;
}
