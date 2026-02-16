package com.example.shajara.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResetPasswordDTO {
    @NotBlank(message = "Username required")
    private String username;
}
