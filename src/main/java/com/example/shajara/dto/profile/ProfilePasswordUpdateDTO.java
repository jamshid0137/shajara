package com.example.shajara.dto.profile;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@AllArgsConstructor
@Setter
@Builder
@NoArgsConstructor
public class ProfilePasswordUpdateDTO {
    @NotBlank(message = "current password required !")
    private String currentPassword;

    @NotBlank(message = "newPassword required !")
    private String newPassword;

}
