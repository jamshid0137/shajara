package com.example.shajara.dto.profile;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@AllArgsConstructor
@Setter
@Builder
@NoArgsConstructor
public class ProfileDetailUpdateDTO {
    @NotBlank(message = "name required !")
    private String name;
}
