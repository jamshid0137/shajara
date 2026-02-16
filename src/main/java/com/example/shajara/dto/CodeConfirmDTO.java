package com.example.shajara.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@AllArgsConstructor
@Setter
@Builder
@NoArgsConstructor
public class CodeConfirmDTO {
    @NotBlank(message = "code required !")
    private String code;

}
