package com.example.shajara.dto.sms;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@AllArgsConstructor
@Setter
@Builder
@NoArgsConstructor
public class SmsResendDTO {
    @NotBlank(message = "phone required")//bo'shmasligini tekshiriberadi.
    private String phone;

}
