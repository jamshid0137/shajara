package com.example.shajara.dto.profile;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@AllArgsConstructor
@Setter
@Builder
@NoArgsConstructor
public class ProfileUsernameUpdateDTO {
    @NotBlank(message = "username required !")
    private String username;

}
