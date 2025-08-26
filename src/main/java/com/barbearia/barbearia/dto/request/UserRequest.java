package com.barbearia.barbearia.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserRequest {

    private String name;
    private String email;
    private String telephone;
    private String password;
    private String confirmPassword;
}
