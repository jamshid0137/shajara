package com.example.shajara.dto.person;


import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonSimpleDto {
    private Long id;
    private String name;
}