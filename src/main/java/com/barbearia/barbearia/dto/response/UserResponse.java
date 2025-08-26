package com.barbearia.barbearia.dto.response;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserResponse {

    private Long id;
    private String name;
    private String email;
    private String telephone;
    private String role;
}
