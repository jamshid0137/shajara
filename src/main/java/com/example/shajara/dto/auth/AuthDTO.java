package com.example.shajara.dto.auth;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthDTO {
    private String username;
    private String password;

}
