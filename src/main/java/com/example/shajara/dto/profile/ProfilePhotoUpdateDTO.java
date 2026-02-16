package com.example.shajara.dto.profile;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@AllArgsConstructor
@Setter
@Builder
@NoArgsConstructor
public class ProfilePhotoUpdateDTO {
    @NotBlank(message = "attachId required !")
    private String attachId;
}
