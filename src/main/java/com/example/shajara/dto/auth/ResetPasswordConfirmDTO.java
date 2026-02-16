package com.example.shajara.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResetPasswordConfirmDTO {
    @NotBlank(message = "Username required")
    private String username;
    @NotBlank(message = "Confirm code required")
    private String code;
    @NotBlank(message = "password required")
    private String password;//yangi parolni o'ylab topib yozadi.

}
