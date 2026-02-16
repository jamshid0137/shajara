package com.example.shajara.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegistrationDTO {
    @NotBlank(message = "name required")//bo'shmasligini tekshiriberadi.
    private String name;
    @NotBlank(message = "username required")
    private String username;
    @NotBlank(message = "password required")
    private String password;

}
